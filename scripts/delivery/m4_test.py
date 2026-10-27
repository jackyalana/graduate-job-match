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
    for role in ['admin','student','student2','hr']: login(role)
    call('create category','/api/admin/job/categories',role='admin',data={'name':'CF synthetic'},code='OK')
    category=call('categories','/api/public/job/categories',code='OK')['data'][0]['id']
    ids=[]
    for i in range(4):
        job=create_job('CF demo '+str(i));approve(job);ids.append(job['id'])
    cold=call('cold batch','/api/student/recommendation-batches',role='student',data={},code='OK')['data']
    assert all(x['match']['strategy']=='HOT_FALLBACK' for x in cold['items'])
    for job in ids[:3]:
        call('student view','/api/student/jobs/'+str(job)+'/view',role='student',data={},code='OK')
        call('repeat student view idempotent','/api/student/jobs/'+str(job)+'/view',role='student',data={},code='OK')
        call('neighbor view','/api/student/jobs/'+str(job)+'/view',role='student2',data={},code='OK')
    call('neighbor favorite target','/api/student/jobs/'+str(ids[3])+'/actions/FAVORITE?active=true',role='student2',data={},code='OK')
    call('favorite idempotent','/api/student/jobs/'+str(ids[3])+'/actions/FAVORITE?active=true',role='student2',data={},code='OK')
    assert call('old batch stale after view','/api/student/recommendation-batches/'+cold['batchId'],409,'student',code='BATCH_STALE')
    ranked=call('CF enabled','/api/student/recommendation-batches',role='student',data={},code='OK')['data']
    target=next(x for x in ranked['items'] if x['id']==ids[3]);assert target['match']['strategy']=='HYBRID' and target['match']['components']['collaborative']==0.6,target
    assert target['match']['components']['hot']>0.8
    call('cancel neighbor favorite','/api/student/jobs/'+str(ids[3])+'/actions/FAVORITE?active=false',role='student2',data={},code='OK')
    cancelled=call('CF sees cancellation','/api/student/recommendation-batches',role='student',data={},code='OK')['data'];target=next(x for x in cancelled['items'] if x['id']==ids[3]);assert target['match']['components']['collaborative']==0.0
    call('hide job','/api/student/jobs/'+str(ids[3])+'/actions/HIDE?active=true',role='student',data={},code='OK')
    hidden=call('hidden job excluded','/api/student/recommendation-batches',role='student',data={},code='OK')['data'];assert all(x['id']!=ids[3] for x in hidden['items'])
    call('unhide job','/api/student/jobs/'+str(ids[3])+'/actions/HIDE?active=false',role='student',data={},code='OK')
    unhidden=call('unhidden job restored','/api/student/recommendation-batches',role='student',data={},code='OK')['data'];assert any(x['id']==ids[3] for x in unhidden['items'])
    stats=call('admin statistics','/api/admin/statistics',role='admin',code='OK')['data'];assert stats['openJobs']==4 and stats['applications']==0
    call('student cannot read admin stats','/api/admin/statistics',403,'student',code='FORBIDDEN')
    print('PASS M4 CF/favorites/actions/statistics',len(records),'requests on',DB)
finally:
    (ROOT/'docs/evidence/delivery/m4-http.json').write_text(json.dumps({'tests':records,'total':len(records)},ensure_ascii=False,indent=2))
