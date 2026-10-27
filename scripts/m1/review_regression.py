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
    for role in ['admin','hr']: login(role)
    for i in range(5):
        call('username failed '+str(i+1),'/api/public/auth/login/password',400,data={'username':'demo_student','password':'wrong'},code='AUTH_INVALID_CREDENTIALS')
    for variant in ['DEMO_STUDENT','dÉmo_student']:
        call('username variant shares limit '+variant,'/api/public/auth/login/password',429,data={'username':variant,'password':'wrong'},code='AUTH_LOGIN_RATE_LIMITED')
    for i in range(5):
        call('email failed '+str(i+1),'/api/public/auth/login/email',400,data={'email':'demo_student2@example.invalid','code':'000000'},code='EMAIL_CODE_INVALID')
    for variant in ['DEMO_STUDENT2@EXAMPLE.INVALID','dÉmo_student2@example.invalid']:
        call('email variant shares limit '+variant,'/api/public/auth/login/email',429,data={'email':variant,'code':'000000'},code='AUTH_LOGIN_RATE_LIMITED')
    for name,path in [('query type','/api/jobs?categoryId=abc'),('path type','/api/jobs/abc'),('missing parameter','/api/public/job/detail')]:
        call(name,path,422,code='VALIDATION_FAILED')
    call('create category','/api/admin/job/categories',role='admin',data={'name':'审核测试类别'},code='OK')
    category=call('list categories','/api/public/job/categories',code='OK')['data'][0]['id']
    job=create_job('审核完整条件',educationMinRank=3,graduateYearMin=2025,graduateYearMax=2027,deadline='2030-12-31 15:30')
    tags=call('skill list','/api/public/tag/list',code='OK')['data']; tagid=tags[0]['id']
    call('set job skills','/api/hr/job/tags/save',role='hr',data={'jobId':job['id'],'items':[{'tagId':tagid,'proficiency':2}]},code='OK')
    detail=call('admin detail includes all audit conditions','/api/admin/audit/job/detail?id='+str(job['id']),role='admin',code='OK')['data']
    for key,value in {'categoryId':category,'cityCode':'上海','salaryMin':8000,'salaryMax':12000,'salaryNegotiable':False,'educationMinRank':3,'graduateYearMin':2025,'graduateYearMax':2027,'acceptFreshGraduate':True}.items(): assert detail[key]==value,(key,detail.get(key))
    assert detail['deadline'].startswith('2030-12-31') and len(detail['tags'])==1 and detail['tags'][0]['tagId']==tagid and detail['tags'][0]['proficiency']==2
    assert detail['version']>job['version']
    call('reject approval of unseen version','/api/admin/audit/job/decision',409,'admin',{'id':job['id'],'pass':True,'version':job['version']},code='CONFLICT')
    call('HR changes skills after detail viewed','/api/hr/job/tags/save',role='hr',data={'jobId':job['id'],'items':[{'tagId':tagid,'proficiency':3}]},code='OK')
    call('reject previously viewed version after change','/api/admin/audit/job/decision',409,'admin',{'id':job['id'],'pass':True,'version':detail['version']},code='CONFLICT')
    latest=call('view updated audit detail','/api/admin/audit/job/detail?id='+str(job['id']),role='admin',code='OK')['data']
    assert latest['version']>detail['version'] and latest['tags'][0]['proficiency']==3
    call('approve viewed version','/api/admin/audit/job/decision',role='admin',data={'id':job['id'],'pass':True,'version':latest['version']},code='OK')
    print('PASS',len(records),'real HTTP requests with assertions; schema',DB)
finally:
    (ROOT/'docs/evidence/M1-review/http-results.json').write_text(json.dumps({'schema':DB,'tests':records,'total':len(records)},ensure_ascii=False,indent=2))
