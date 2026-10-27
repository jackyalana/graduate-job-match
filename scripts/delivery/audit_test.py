#!/usr/bin/env python3
"""M1 HTTP integration tests against the isolated synthetic M1 MySQL schema."""
import datetime, json, sys, urllib.error, urllib.request
from pathlib import Path
sys.path.insert(0,str(Path(__file__).resolve().parents[1]/'m0'))
from local import ROOT,LOCAL,mysql,credentials
BASE='http://127.0.0.1:18081'
DB=json.loads((LOCAL/'m1-schemas.json').read_text())['runtime']
assert DB.startswith('graduate_job_match_m1_runtime_')
records=[]; tokens={}

def request(name,path,expected=200,role=None,data=None,method=None,raw=None,headers=None,expected_code=None):
    h=dict(headers or {})
    if role: h['satoken']=tokens[role]
    if data is not None: h['Content-Type']='application/json'; raw=json.dumps(data,ensure_ascii=False).encode()
    req=urllib.request.Request(BASE+path,data=raw,headers=h,method=method)
    try: result=urllib.request.urlopen(req,timeout=30)
    except urllib.error.HTTPError as e: result=e
    body=result.read(); content=result.headers.get('Content-Type','')
    try: parsed=json.loads(body)
    except (ValueError,UnicodeDecodeError): parsed=None
    item={'name':name,'method':req.get_method(),'path':path.split('?')[0],'expected_http':expected,
          'actual_http':result.status,'code':parsed.get('code') if isinstance(parsed,dict) else None,
          'request_id_present':bool(result.headers.get('X-Request-Id'))}
    if isinstance(parsed,dict) and parsed.get('requestId') is not None:
        item['request_id_matches_header']=parsed['requestId']==result.headers.get('X-Request-Id')
    records.append(item)
    assert result.status==expected,(name,item,parsed)
    assert item['request_id_present'],(name,item)
    if 'request_id_matches_header' in item: assert item['request_id_matches_header'],item
    if expected_code is not None: assert item['code']==expected_code,(name,item,parsed)
    return parsed,body,result.headers

def call(name,path,expected=200,role=None,data=None,method=None,code=None):
    return request(name,path,expected,role,data,method,expected_code=code)[0]

def login(role):
    payload=call('password login '+role,'/api/public/auth/login/password',data={'username':'demo_'+role,'password':credentials()['demoPassword']},code='OK')
    tokens[role]=payload['data']

def upload(name,role,resume_id,content,filename='resume.pdf',mime='application/pdf',expected=200):
    boundary='M1BoundaryOnlySynthetic'
    raw=(f'--{boundary}\r\nContent-Disposition: form-data; name="resumeId"\r\n\r\n{resume_id}\r\n'
         f'--{boundary}\r\nContent-Disposition: form-data; name="file"; filename="{filename}"\r\nContent-Type: {mime}\r\n\r\n').encode()+content+f'\r\n--{boundary}--\r\n'.encode()
    return request(name,'/api/user/resume/file/upload',expected,role,raw=raw,headers={'Content-Type':'multipart/form-data; boundary='+boundary})[0]

def count(sql): return mysql('USE `'+DB+'`; '+sql).strip().splitlines()[-1]

def create_job(name,hr='hr',**extra):
    body=dict(jobName=name,city='上海',cityCode='上海',jobType='后端开发',salaryRange='8000-12000',
              status=1,description='仅用于 M1 合成验证',acceptFreshGraduate=True,categoryId=category,
              salaryMin=8000,salaryMax=12000,salaryNegotiable=False)
    body.update(extra)
    call('create '+name,'/api/hr/job/save',role=hr,data=body,code='OK')
    jobs=call('list HR jobs for '+name,'/api/hr/job/list',role=hr,code='OK')['data']
    job=next(x for x in jobs if x['jobName']==name)
    return job

def approve(job):
    pending=call('load pending version '+str(job['id']),'/api/admin/audit/job/list',role='admin',code='OK')['data']
    current=next(x for x in pending if x['id']==job['id'])
    call('approve '+str(job['id']),'/api/admin/audit/job/decision',role='admin',data={'id':job['id'],'pass':True,'version':current['version']},code='OK')

try:
    for role in ['admin','student','hr']:login(role)
    call('create synthetic audit tag','/api/admin/tag/save',role='admin',data={'tagName':'Audit synthetic tag','tagType':'SKILL','baseWeight':3,'status':1},code='OK')
    tag=next(x for x in call('admin tag list','/api/admin/tag/list',role='admin',code='OK')['data'] if x['tagName']=='Audit synthetic tag')
    call('update synthetic audit tag','/api/admin/tag/save',role='admin',data={'id':tag['id'],'tagName':'Audit synthetic updated','tagType':'SKILL','baseWeight':3,'status':1},code='OK')
    call('delete synthetic audit tag','/api/admin/tag/delete',role='admin',data={'id':tag['id']},code='OK')
    assert count("SELECT COUNT(*) FROM audit_log WHERE actor_id=1 AND resource_type='TAG' AND resource_id="+str(tag['id'])+" AND action IN ('CREATE','UPDATE','DELETE');")=='3'
    call('student cannot modify tags','/api/admin/tag/delete',403,'student',{'id':tag['id']},code='FORBIDDEN')
    category=call('categories','/api/public/job/categories',code='OK')['data'][0]['id']
    job=create_job('Audit reason synthetic')
    pending=next(x for x in call('pending job','/api/admin/audit/job/list',role='admin',code='OK')['data'] if x['id']==job['id'])
    call('reject job with reason','/api/admin/audit/job/decision',role='admin',data={'id':job['id'],'pass':False,'version':pending['version'],'reason':'synthetic audit reason'},code='OK')
    assert count("SELECT COUNT(*) FROM audit_log WHERE actor_id=1 AND resource_type='JOB' AND resource_id="+str(job['id'])+" AND action='REJECT' AND reason='synthetic audit reason';")=='1'
    print('PASS synthetic administrator audit',len(records),'requests')
finally:
    (ROOT/'docs/evidence/delivery/audit-http.json').write_text(json.dumps({'tests':records,'total':len(records)},ensure_ascii=False,indent=2))
