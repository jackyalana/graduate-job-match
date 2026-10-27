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

import concurrent.futures
try:
    for role in ['admin','student','student2','hr','hr2']: login(role)
    call('create category','/api/admin/job/categories',role='admin',data={'name':'Delivery synthetic'},code='OK')
    category=call('categories','/api/public/job/categories',code='OK')['data'][0]['id']
    job=create_job('Delivery synthetic role',educationMinRank=None,graduateYearMin=None,graduateYearMax=None);approve(job);jobid=job['id']
    assert count("SELECT COUNT(*) FROM audit_log WHERE resource_type='JOB' AND resource_id="+str(jobid)+" AND action='APPROVE' AND actor_id=1;")=='1'
    resume=next(x['id'] for x in call('resumes','/api/user/resume/list',role='student',code='OK')['data'] if x['isDefault']==1)
    original=b'%PDF-1.4\nOriginal Synthetic\n%%EOF'; upload('original PDF','student',resume,original)
    def submit(i):
        req=urllib.request.Request(BASE+'/api/user/job/apply',data=json.dumps({'jobId':jobid,'resumeId':resume}).encode(),headers={'Content-Type':'application/json','satoken':tokens['student']})
        try:r=urllib.request.urlopen(req,timeout=30)
        except urllib.error.HTTPError as e:r=e
        body=json.load(r);records.append({'name':'parallel apply '+str(i),'actual_http':r.status,'code':body.get('code'),'request_id_present':bool(r.headers.get('X-Request-Id'))})
        assert (r.status,body.get('code')) in [(200,'OK'),(409,'APPLY_DUPLICATE')]
        return body['code']
    with concurrent.futures.ThreadPoolExecutor(max_workers=2) as pool: result=list(pool.map(submit,range(2)))
    assert result.count('OK')==1 and result.count('APPLY_DUPLICATE')==1,result
    assert count('SELECT COUNT(*) FROM job_apply WHERE user_id=2 AND job_id='+str(jobid)+';')=='1'
    my=call('own apply detail','/api/user/apply/list/detail',role='student',code='OK')['data'][0]; aid=my['applyId'];assert my['version']==1
    assert count('SELECT COUNT(*) FROM job_apply WHERE id='+str(aid)+' AND JSON_VALID(profile_snapshot) AND JSON_VALID(resume_snapshot) AND JSON_VALID(job_snapshot) AND JSON_VALID(match_snapshot) AND attachment_snapshot IS NOT NULL;')=='1'
    call('student cross owner timeline denied','/api/user/apply/events?applyId='+str(aid),404,'student2',code='APPLY_RECORD_NOT_FOUND')
    call('HR cross owner timeline denied','/api/hr/job/apply/events?applyId='+str(aid),404,'hr2',code='JOB_NOT_FOUND')
    call('HR cross owner attachment denied','/api/hr/resume/file/download?applyId='+str(aid),404,'hr2',code='JOB_NOT_FOUND')
    _,hrbytes,_=request('HR downloads snapshot','/api/hr/resume/file/download?applyId='+str(aid),200,'hr');assert hrbytes==original
    call('edit live resume','/api/user/resume/save',role='student',data={'id':resume,'resumeName':'Updated live','realName':'Synthetic change','status':1},code='OK')
    upload('replace live PDF','student',resume,b'%PDF-1.4\nChanged Synthetic\n%%EOF')
    _,hrbytes,_=request('snapshot remains unchanged','/api/hr/resume/file/download?applyId='+str(aid),200,'hr');assert hrbytes==original
    after=call('snapshot resume unchanged','/api/user/apply/list/detail',role='student',code='OK')['data'][0];assert after['resume']['resumeName']==my['resume']['resumeName']
    live_job=next(x for x in call('load job before edit','/api/hr/job/list',role='hr',code='OK')['data'] if x['id']==jobid)
    call('edit live job','/api/hr/job/save',role='hr',data=dict(live_job,jobName='Synthetic changed live role'),code='OK')
    after_job=call('snapshot job unchanged','/api/user/apply/list/detail',role='student',code='OK')['data'][0];assert after_job['job']['jobName']=='Delivery synthetic role'
    call('stale status denied','/api/hr/job/apply/status',409,'hr',{'applyId':aid,'status':2,'version':0},code='CONFLICT')
    call('invalid skip denied','/api/hr/job/apply/status',400,'hr',{'applyId':aid,'status':4,'version':1},code='APPLY_STATUS_INVALID')
    call('HR interview','/api/hr/job/apply/status',role='hr',data={'applyId':aid,'status':2,'version':1},code='OK')
    call('HR offer','/api/hr/job/apply/status',role='hr',data={'applyId':aid,'status':4,'version':2},code='OK')
    call('student accept','/api/user/apply/status',role='student',data={'applyId':aid,'status':5,'version':3},code='OK')
    events=call('timeline','/api/user/apply/events?applyId='+str(aid),role='student',code='OK')['data'];assert [x['toStatus'] for x in events]==[0,2,4,5]
    call('terminal status cannot revert','/api/hr/job/apply/status',400,'hr',{'applyId':aid,'status':3,'version':4},code='APPLY_STATUS_INVALID')
    call('close job keeps history','/api/hr/job/remove',role='hr',data={'id':jobid},code='OK');assert count('SELECT COUNT(*) FROM job_apply WHERE id='+str(aid)+';')=='1'
    _,hrbytes,_=request('closed job snapshot remains','/api/hr/resume/file/download?applyId='+str(aid),200,'hr');assert hrbytes==original
    race_job=create_job('Delivery status race');approve(race_job)
    second_resume=call('student2 resumes','/api/user/resume/list',role='student2',code='OK')['data'][0]['id']
    call('student2 applies','/api/user/job/apply',role='student2',data={'jobId':race_job['id'],'resumeId':second_resume},code='OK')
    race_apply=call('student2 application','/api/user/apply/list',role='student2',code='OK')['data'][0]['id']
    def update_status(status):
        req=urllib.request.Request(BASE+'/api/hr/job/apply/status',data=json.dumps({'applyId':race_apply,'status':status,'version':1}).encode(),headers={'Content-Type':'application/json','satoken':tokens['hr']})
        try:r=urllib.request.urlopen(req,timeout=30)
        except urllib.error.HTTPError as e:r=e
        body=json.load(r);records.append({'name':'concurrent status '+str(status),'actual_http':r.status,'code':body.get('code'),'request_id_present':bool(r.headers.get('X-Request-Id'))})
        assert (r.status,body.get('code')) in [(200,'OK'),(409,'CONFLICT')]
        return body['code']
    with concurrent.futures.ThreadPoolExecutor(max_workers=2) as pool: statuses=list(pool.map(update_status,[1,2]))
    assert statuses.count('OK')==1 and statuses.count('CONFLICT')==1,statuses
    race_events=call('concurrent status event','/api/user/apply/events?applyId='+str(race_apply),role='student2',code='OK')['data'];assert len(race_events)==2
    print('PASS M3 synthetic workflow',len(records),'requests on',DB)
finally:
    (ROOT/'docs/evidence/delivery/m3-http.json').write_text(json.dumps({'tests':records,'total':len(records)},ensure_ascii=False,indent=2))
