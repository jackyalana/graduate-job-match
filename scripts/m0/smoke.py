#!/usr/bin/env python3
"""Real HTTP baseline probes; known defects are observations, not asserted fixes.
Run on the freshly seeded M0 database BEFORE interactive browser checks.
"""
import concurrent.futures
import datetime
import json
from pathlib import Path
import urllib.request
import urllib.error
from local import ROOT, LOCAL, DATABASE, mysql, credentials

BASE='http://127.0.0.1:18080'
evidence=ROOT/'docs/evidence/M0'
records=[]
tokens={}

def call(name, path, role=None, data=None, expected=0):
    headers={'Content-Type':'application/json'}
    if role: headers['satoken']=tokens[role]
    payload=None if data is None else json.dumps(data).encode()
    request=urllib.request.Request(BASE+path, data=payload, headers=headers)
    try:
        response=urllib.request.urlopen(request,timeout=30)
    except urllib.error.HTTPError as error: response=error
    raw=response.read()
    try: result=json.loads(raw)
    except ValueError: result={'raw':raw.decode(errors='replace')}
    safe_data=dict(data) if isinstance(data,dict) else data
    if safe_data and 'password' in safe_data: safe_data['password']='[REDACTED]'
    safe_result=dict(result)
    if 'login/password' in path and result.get('code')==0: safe_result['data']='[TOKEN REDACTED]'
    record=dict(name=name,method=request.get_method(),path=path,role=role,http_status=response.status,
                request=safe_data,response=safe_result)
    records.append(record)
    if expected is not None: assert result.get('code')==expected, record
    return result.get('data')

def sql(statement): return mysql('USE '+DATABASE+'; '+statement)
def save_results():
    output=dict(timestamp=datetime.datetime.now(datetime.timezone.utc).isoformat(),
                scope='Upstream M0 baseline, synthetic data only; not SDD acceptance', records=records)
    (evidence/'smoke-results.json').write_text(json.dumps(output,ensure_ascii=False,indent=2))

try:
    password=credentials()['demoPassword']
    call('unauthenticated resume access','/api/user/resume/list',expected=401)
    for role in ['admin','student','student2','hr','hr2']:
        tokens[role]=call('password login '+role,'/api/public/auth/login/password',
                          data=dict(username='demo_'+role,password=password))
        call('current user '+role,'/api/common/user/me',role)
    call('pending HR login rejected','/api/public/auth/login/password',
         data=dict(username='demo_hr_pending',password=password),expected=None)
    pending=call('pending HR list','/api/admin/audit/hr/list','admin')
    call('approve synthetic HR','/api/admin/audit/hr/decision','admin',{'id':pending[0]['id'],'pass':True})
    tokens['pending']=call('approved HR login','/api/public/auth/login/password',data=dict(username='demo_hr_pending',password=password))
    call('student cannot enter admin audit','/api/admin/audit/job/list','student',expected=403)
    tags=call('public skills','/api/public/tag/list')
    java=next(t['id'] for t in tags if t['tagName']=='Java')
    vue=next(t['id'] for t in tags if t['tagName']=='Vue')
    job_body=dict(jobName='M0 合成 Java 应届岗位',city='上海',salaryRange='8000-12000',jobType='后端开发',
                  description='合成测试岗位，用于底座审核，不是真实招聘。',status=1)
    call('HR creates pending job','/api/hr/job/save','hr',job_body)
    jobs=call('HR own jobs','/api/hr/job/list','hr'); job=jobs[0]['id']
    call('pending job absent from public list','/api/public/job/list')
    call('pending job cannot be opened','/api/public/job/detail?id='+str(job),expected=None)
    call('HR saves Java requirement','/api/hr/job/tags/save','hr',dict(jobId=job,items=[dict(tagId=java,proficiency=3)]))
    call('admin pending jobs','/api/admin/audit/job/list','admin')
    call('admin approves job','/api/admin/audit/job/decision','admin',{'id':job,'pass':True})
    public=call('public approved job','/api/public/job/list'); assert any(j['id']==job for j in public)
    call('public approved detail','/api/public/job/detail?id='+str(job))
    fallback=call('no resume recommendation','/api/user/job/recommend','student2'); assert all(j['matchScore'] is None for j in fallback)
    resume_body=dict(resumeName='M0 合成学生简历',realName='合成学生A',city='上海',education='本科',
                     workYears='应届',expectJobType='后端开发',expectSalary='8000',workDesc='仅合成项目经历',status=1)
    call('student saves resume','/api/user/resume/save','student',resume_body)
    resumes=call('student resume list','/api/user/resume/list','student'); resume=resumes[0]['id']
    call('student resume detail','/api/user/resume/detail?id='+str(resume),'student')
    call('student saves skills','/api/user/resume/tags/save','student',dict(resumeId=resume,items=[dict(tagId=java,proficiency=3)]))
    rec=call('real cosine recommendation','/api/user/job/recommend?resumeId='+str(resume),'student')
    assert next(j['matchScore'] for j in rec if j['id']==job)==1
    call('student2 denied student1 resume','/api/user/resume/detail?id='+str(resume),'student2',expected=None)
    call('HR2 denied HR1 applications','/api/hr/job/applications?jobId='+str(job),'hr2',expected=None)
    call('student applies once','/api/user/job/apply','student',dict(jobId=job,resumeId=resume))
    call('same resume duplicate rejected','/api/user/job/apply','student',dict(jobId=job,resumeId=resume),expected=None)
    applies=call('HR sees applicant and match','/api/hr/job/applications?jobId='+str(job),'hr'); apply_id=applies[0]['applyId']
    call('student application detail','/api/user/apply/list/detail','student')
    call('HR rejects application','/api/hr/job/apply/status','hr',dict(applyId=apply_id,status=3))
    call('PROBE terminal status can revert to submitted','/api/hr/job/apply/status','hr',dict(applyId=apply_id,status=0))
    call('second resume save','/api/user/resume/save','student',dict(resume_body,resumeName='M0 合成第二简历'))
    resume2=max(r['id'] for r in call('list two resumes','/api/user/resume/list','student'))
    call('PROBE same user second resume same job','/api/user/job/apply','student',dict(jobId=job,resumeId=resume2))
    records.append(dict(name='database duplicate user-job proof',result=sql('SELECT user_id,job_id,COUNT(*) AS copies FROM job_apply GROUP BY user_id,job_id;')))
    # A byte stream deliberately NOT a PDF, to verify the upstream extension-only validator.
    boundary='m0fixtureboundary'
    body=(f'--{boundary}\r\nContent-Disposition: form-data; name="resumeId"\r\n\r\n{resume}\r\n'
          f'--{boundary}\r\nContent-Disposition: form-data; name="file"; filename="synthetic-not-pdf.pdf"\r\nContent-Type: application/pdf\r\n\r\n'
          f'M0 synthetic non-PDF payload\r\n--{boundary}--\r\n').encode()
    request=urllib.request.Request(BASE+'/api/user/resume/file/upload',data=body,
        headers={'satoken':tokens['student'],'Content-Type':'multipart/form-data; boundary='+boundary})
    with urllib.request.urlopen(request) as response: upload=json.load(response)
    records.append(dict(name='PROBE invalid PDF upload',response=upload))
    assert upload['code']==0
    file_url=upload['data']['fileUrl']
    with urllib.request.urlopen(BASE+file_url) as response:
        records.append(dict(name='PROBE anonymous attachment URL',path=file_url,http_status=response.status,body=response.read().decode()))
    # Tag-set update should invalidate cache and recompute to zero for nonoverlap.
    call('replace resume skills','/api/user/resume/tags/save','student',dict(resumeId=resume,items=[dict(tagId=vue,proficiency=3)]))
    rec=call('cache invalidation nonoverlap','/api/user/job/recommend?resumeId='+str(resume),'student')
    assert next(j['matchScore'] for j in rec if j['id']==job)==0
    call('restore resume skills','/api/user/resume/tags/save','student',dict(resumeId=resume,items=[dict(tagId=java,proficiency=3)]))
    # Disposable second job proves removal deletes application history, leaving primary demo intact.
    call('create deletion probe job','/api/hr/job/save','hr',dict(job_body,jobName='M0 合成删除探针岗位'))
    probe=max(j['id'] for j in call('find probe job','/api/hr/job/list','hr'))
    call('approve deletion probe','/api/admin/audit/job/decision','admin',{'id':probe,'pass':True})
    call('apply deletion probe','/api/user/job/apply','student',dict(jobId=probe,resumeId=resume))
    before=sql('SELECT COUNT(*) FROM job_apply WHERE job_id='+str(probe)+';')
    call('PROBE physical remove','/api/hr/job/remove','hr',dict(id=probe))
    after=sql('SELECT COUNT(*) FROM job_apply WHERE job_id='+str(probe)+';')
    records.append(dict(name='physical deletion database proof',before=before,after=after))
    # Actual same-pair concurrent recommendation requests. Cold cache/DB via tag save.
    call('invalidate before concurrency probe','/api/user/resume/tags/save','student',dict(resumeId=resume,items=[dict(tagId=java,proficiency=3)]))
    def concurrent_request(_):
        req=urllib.request.Request(BASE+'/api/user/job/recommend?resumeId='+str(resume),headers={'satoken':tokens['student']})
        with urllib.request.urlopen(req,timeout=30) as res: return dict(http=res.status,response=json.load(res))
    with concurrent.futures.ThreadPoolExecutor(max_workers=20) as pool:
        results=list(pool.map(concurrent_request,range(20)))
    records.append(dict(name='20 concurrent same-pair recommendations',results=results,
                        database=sql('SELECT resume_id,job_id,score FROM match_score;')))
    call('logout student2','/api/common/auth/logout','student2',{})
    call('old token after logout rejected','/api/user/resume/list','student2',expected=401)
    (LOCAL/'smoke-ids.json').write_text(json.dumps(dict(jobId=job,resumeId=resume,applyId=apply_id)))
    print('Core real HTTP smoke completed; known upstream defects captured, not SDD passes.')
finally:
    save_results()
