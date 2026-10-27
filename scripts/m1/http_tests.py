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
    call('anonymous common','/api/common/user/me',401,code='AUTH_NOT_LOGGED_IN')
    for role in ['admin','student','student2','hr','hr2']: login(role)
    call('demo registration without email verification','/api/public/auth/register',data={'username':'m1_demo_new','password':credentials()['demoPassword'],'email':'m1_new@example.invalid','role':'USER'},code='OK')
    new_login=call('new demo user login','/api/public/auth/login/password',data={'username':'m1_demo_new','password':credentials()['demoPassword']},code='OK')
    tokens['new']=new_login['data']
    assert call('new user email explicitly unverified','/api/common/user/me',role='new',code='OK')['data']['emailVerified'] is False
    call('demo cannot self-register as admin','/api/public/auth/register',422,data={'username':'m1_admin_new','password':credentials()['demoPassword'],'email':'m1_admin@example.invalid','role':'ADMIN'},code='VALIDATION_FAILED')
    call('demo HR registration','/api/public/auth/register',data={'username':'m1_hr_pending','password':credentials()['demoPassword'],'email':'m1_hr@example.invalid','role':'HR','companyName':'合成待审核企业'},code='OK')
    call('pending demo HR login denied','/api/public/auth/login/password',403,data={'username':'m1_hr_pending','password':credentials()['demoPassword']},code='AUTH_HR_PENDING_AUDIT')
    pending_hr=call('find pending demo HR','/api/admin/audit/hr/list',role='admin',code='OK')['data']
    pending_hr_id=next(x['id'] for x in pending_hr if x['username']=='m1_hr_pending')
    call('admin approves demo HR','/api/admin/audit/hr/decision',role='admin',data={'id':pending_hr_id,'pass':True},code='OK')
    hr_new=call('approved demo HR login','/api/public/auth/login/password',data={'username':'m1_hr_pending','password':credentials()['demoPassword']},code='OK')
    tokens['new_hr']=hr_new['data']
    assert call('approved demo HR email unverified','/api/common/user/me',role='new_hr',code='OK')['data']['emailVerified'] is False
    cookie={'Cookie':'satoken='+tokens['student']}
    request('cookie-only token rejected','/api/common/user/me',401,headers=cookie,expected_code='AUTH_NOT_LOGGED_IN')
    cors={'Origin':'http://127.0.0.1:15174','Access-Control-Request-Method':'PUT','Access-Control-Request-Headers':'satoken,content-type'}
    _,_,headers=request('allowed CORS preflight','/api/student/profile',200,method='OPTIONS',headers=cors)
    assert headers.get('Access-Control-Allow-Origin')=='http://127.0.0.1:15174' and headers.get('Access-Control-Allow-Credentials') is None
    cors['Origin']='https://untrusted.example'
    request('untrusted CORS preflight rejected','/api/student/profile',403,method='OPTIONS',headers=cors)
    call('student forbidden admin','/api/admin/user/page',403,'student',code='FORBIDDEN')
    call('admin create category','/api/admin/job/categories',role='admin',data={'name':'M1合成类别'},code='OK')
    category=call('list categories','/api/public/job/categories',code='OK')['data'][0]['id']
    before=call('get student profile','/api/student/profile',role='student',code='OK')['data']
    assert before['educationRank']==2
    profile=dict(school='合成学校',majorCode='CS',educationRank=2,graduationYear=2026,expectedCategoryId=category,salaryMin=8000,cityCodes=['上海','杭州','上海'])
    saved=call('update own profile','/api/student/profile',role='student',data=profile,method='PUT',code='OK')['data']
    assert saved['profileVersion']==before['profileVersion']+1 and saved['cityCodes']==['上海','杭州']
    call('reject spoofed owner or role','/api/student/profile',422,'student',dict(profile,userId=3,role='ADMIN'),method='PUT',code='VALIDATION_FAILED')
    other=call('student2 sees own profile','/api/student/profile',role='student2',code='OK')['data']
    assert other['educationRank'] is None and other['school'] is None
    resume=call('own resume list','/api/user/resume/list',role='student',code='OK')['data'][0]['id']
    # Use the default resume from migration; list sorting may return the second resume first.
    resumes=call('select default resume','/api/user/resume/list',role='student',code='OK')['data']
    resume=next(x['id'] for x in resumes if x['isDefault']==1)
    call('student2 cannot read student1 resume','/api/user/resume/detail?id='+str(resume),404,'student2',code='RESUME_NOT_FOUND')
    upload('cross-student upload denied','student2',resume,b'%PDF-1.4\n%%EOF',expected=404)
    upload('reject fake PDF header','student',resume,b'not-pdf',expected=422)
    upload('reject misleading MIME','student',resume,b'%PDF-1.4\n%%EOF',mime='image/png',expected=422)
    upload('reject wrong extension','student',resume,b'%PDF-1.4\n%%EOF',filename='resume.png',expected=422)
    upload('reject >5MB file','student',resume,b'%PDF-'+b'x'*(5*1024*1024),expected=413)
    uploaded=upload('valid synthetic PDF uploaded','student',resume,b'%PDF-1.4\n%%EOF')
    public_path=uploaded['data']['fileUrl']
    request('anonymous legacy attachment URL denied',public_path,404)
    request('anonymous catchall URL denied','/uploads/'+public_path.rsplit('/',1)[-1],404)
    request('anonymous authenticated download denied','/api/user/resume/file/download?resumeId='+str(resume),401,expected_code='AUTH_NOT_LOGGED_IN')
    request('owner downloads PDF','/api/user/resume/file/download?resumeId='+str(resume),200,'student')
    request('SDD student download','/api/student/resumes/'+str(resume)+'/attachment',200,'student')
    call('cross student download denied','/api/user/resume/file/download?resumeId='+str(resume),404,'student2',code='RESUME_NOT_FOUND')
    job=create_job('M1 合成岗位',educationMinRank=2,graduateYearMin=2025,graduateYearMax=2027)
    jobid=job['id']
    call('pending job detail hidden','/api/public/job/detail?id='+str(jobid),404,code='JOB_NOT_FOUND')
    approve(job)
    assert call('approved public detail','/api/public/job/detail?id='+str(jobid),code='OK')['data']['id']==jobid
    target=call('SDD public page','/api/jobs?page=1&pageSize=10&categoryId='+str(category),code='OK')['data']
    assert target['total']>=1 and any(item['id']==jobid for item in target['items'])
    assert call('SDD public detail','/api/jobs/'+str(jobid),code='OK')['data']['id']==jobid
    filtered=call('structured category and salary search','/api/public/job/list?categoryId='+str(category)+'&salaryMin=9000&salaryMax=10000',code='OK')['data']
    assert any(x['id']==jobid for x in filtered)
    filtered=call('salary preference excludes incompatible jobs','/api/public/job/list?salaryMin=20000',code='OK')['data']
    assert all(x['id']!=jobid for x in filtered)
    call('invalid salary filter rejected','/api/public/job/list?salaryMin=20000&salaryMax=10000',422,code='VALIDATION_FAILED')
    personal=call('qualified recommendation','/api/user/job/recommend?resumeId='+str(resume),role='student',code='OK')['data']
    assert any(x['id']==jobid for x in personal)
    filtered_personal=call('personalized salary filter','/api/user/job/recommend?resumeId='+str(resume)+'&salaryMin=20000',role='student',code='OK')['data']
    assert all(x['id']!=jobid for x in filtered_personal)
    # A student with missing rank/year can still browse, but cannot see the restricted job in personalized results.
    incomplete=call('incomplete profile recommendation','/api/user/job/recommend',role='student2',code='OK')
    assert 'educationRank' in incomplete['details']['missingProfileFields'] and 'graduationYear' in incomplete['details']['missingProfileFields']
    personal2=incomplete['data']
    assert all(x['id']!=jobid for x in personal2)
    tags=call('public skill list','/api/public/tag/list',code='OK')['data']; tagid=tags[0]['id']
    call('change approved job skills','/api/hr/job/tags/save',role='hr',data={'jobId':jobid,'items':[{'tagId':tagid,'proficiency':2}]},code='OK')
    call('skill change re-enters audit','/api/public/job/detail?id='+str(jobid),404,code='JOB_NOT_FOUND')
    call('stale audit decision denied','/api/admin/audit/job/decision',409,'admin',{'id':jobid,'pass':True,'version':job['version']},code='CONFLICT')
    call('audit without version denied','/api/admin/audit/job/decision',409,'admin',{'id':jobid,'pass':True},code='CONFLICT')
    approve(job)
    stale=dict(job,id=jobid,jobName='M1 stale update',status=1,version=job['version'])
    call('stale job version rejected','/api/hr/job/save',409,'hr',stale,code='CONFLICT')
    fresh=call('HR current version','/api/hr/job/list',role='hr',code='OK')['data'][0]
    call('job edit pending again','/api/hr/job/save',role='hr',data=dict(fresh,status=1),code='OK')
    call('edited job hidden','/api/public/job/detail?id='+str(jobid),404,code='JOB_NOT_FOUND')
    approve(job)
    past=(datetime.datetime.utcnow()-datetime.timedelta(days=1)).strftime('%Y-%m-%d %H:%M')
    expired=create_job('M1 过期岗位',deadline=past); approve(expired)
    call('expired public detail denied','/api/public/job/detail?id='+str(expired['id']),404,code='JOB_NOT_FOUND')
    public=call('public list excludes expired','/api/public/job/list',code='OK')['data']; assert all(x['id']!=expired['id'] for x in public)
    call('expired job application denied','/api/user/job/apply',400,'student',{'jobId':expired['id'],'resumeId':resume},code='JOB_NOT_APPLICABLE')
    call('HR2 cannot list HR1 applications','/api/hr/job/applications?jobId='+str(jobid),404,'hr2',code='JOB_NOT_FOUND')
    restricted=create_job('M1 学历限制岗位',educationMinRank=3); approve(restricted)
    assert call('restricted job public detail','/api/public/job/detail?id='+str(restricted['id']),code='OK')['data']['id']==restricted['id']
    personal=call('lower education excludes recommendation','/api/user/job/recommend?resumeId='+str(resume),role='student',code='OK')['data']
    assert all(x['id']!=restricted['id'] for x in personal)
    call('student submits application','/api/user/job/apply',role='student',data={'jobId':jobid,'resumeId':resume},code='OK')
    assert all(x['id']!=jobid for x in call('applied job excluded from recommendation','/api/user/job/recommend?resumeId='+str(resume),role='student',code='OK')['data'])
    applies=call('HR1 sees own applicant','/api/hr/job/applications?jobId='+str(jobid),role='hr',code='OK')['data']; appid=applies[0]['applyId']
    request('corresponding HR downloads','/api/hr/resume/file/download?applyId='+str(appid),200,'hr')
    request('SDD HR download','/api/hr/applications/'+str(appid)+'/attachment',200,'hr')
    call('SDD HR cross-owner download denied','/api/hr/applications/'+str(appid)+'/attachment',404,'hr2',code='JOB_NOT_FOUND')
    call('other HR download denied','/api/hr/resume/file/download?applyId='+str(appid),404,'hr2',code='JOB_NOT_FOUND')
    upload('referenced resume attachment cannot be replaced','student',resume,b'%PDF-1.4\n%%EOF',expected=409)
    call('referenced attachment delete denied','/api/user/resume/file/delete?resumeId='+str(resume),409,'student',data={},code='CONFLICT')
    assert count('SELECT COUNT(*) FROM job_apply WHERE id='+str(appid)+';')=='1'
    call('legacy job remove closes without deleting history','/api/hr/job/remove',role='hr',data={'id':jobid},code='OK')
    assert count('SELECT COUNT(*) FROM job_apply WHERE id='+str(appid)+';')=='1'
    call('closed job detail denied','/api/public/job/detail?id='+str(jobid),404,code='JOB_NOT_FOUND')
    call('legacy user delete protected','/api/admin/user/delete',409,'admin',{'id':2},code='CONFLICT')
    call('archive resume','/api/user/resume/delete',role='student',data={'id':resume},code='OK')
    assert count('SELECT COUNT(*) FROM job_apply WHERE id='+str(appid)+';')=='1'
    call('inactive resume cannot be used for recommendation','/api/user/job/recommend?resumeId='+str(resume),400,'student',code='RESUME_NOT_AVAILABLE')
    call('disable student','/api/admin/user/status/update',role='admin',data={'id':2,'status':0},code='OK')
    for path in ['/api/common/user/me','/api/user/resume/list','/api/student/profile']:
        call('disabled token denied '+path,path,401,'student',code='AUTH_NOT_LOGGED_IN')
    call('re-enable student','/api/admin/user/status/update',role='admin',data={'id':2,'status':1},code='OK')
    call('old token remains invalid after re-enable','/api/common/user/me',401,'student',code='AUTH_NOT_LOGGED_IN')
    login('student')
    login('hr')
    live=create_job('M1 企业停用岗位'); approve(live)
    call('disable HR','/api/admin/user/status/update',role='admin',data={'id':4,'status':0},code='OK')
    call('disabled company job detail denied','/api/public/job/detail?id='+str(live['id']),404,code='JOB_NOT_FOUND')
    assert all(x['id']!=live['id'] for x in call('disabled company omitted from list','/api/public/job/list',code='OK')['data'])
    valid_resume=next(x['id'] for x in call('remaining valid resume','/api/user/resume/list',role='student',code='OK')['data'] if x['status']==1)
    call('disabled company application denied','/api/user/job/apply',400,'student',{'jobId':live['id'],'resumeId':valid_resume},code='JOB_NOT_APPLICABLE')
    call('disabled HR token denied','/api/hr/job/list',401,'hr',code='AUTH_NOT_LOGGED_IN')
    call('disabled HR common denied','/api/common/user/me',401,'hr',code='AUTH_NOT_LOGGED_IN')
    call('re-enable HR','/api/admin/user/status/update',role='admin',data={'id':4,'status':1},code='OK')
    for i in range(5):
        call('failed login '+str(i+1),'/api/public/auth/login/password',400,data={'username':'m1_rate_test','password':'wrong synthetic'},code='AUTH_INVALID_CREDENTIALS')
    call('rate limit after five failures','/api/public/auth/login/password',429,data={'username':'m1_rate_test','password':'wrong synthetic'},code='AUTH_LOGIN_RATE_LIMITED')
    for i in range(5):
        call('failed email login '+str(i+1),'/api/public/auth/login/email',400,data={'email':'m1_unknown@example.invalid','code':'000000'},code='EMAIL_CODE_INVALID')
    call('email login rate limit','/api/public/auth/login/email',429,data={'email':'m1_unknown@example.invalid','code':'000000'},code='AUTH_LOGIN_RATE_LIMITED')
    print('PASS',len(records),'real HTTP requests; database',DB)
finally:
    result={'schema':DB,'tests':records,'total':len(records)}
    (ROOT/'docs/evidence/M1/http-results.json').write_text(json.dumps(result,ensure_ascii=False,indent=2))
