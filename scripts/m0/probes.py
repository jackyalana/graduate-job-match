#!/usr/bin/env python3
"""Supplemental M0 baseline probes; requires smoke.py and private demo DB."""
import json
import urllib.request
from local import ROOT,LOCAL,DATABASE,mysql,credentials

BASE='http://127.0.0.1:18080'
records=[]
ids=json.loads((LOCAL/'smoke-ids.json').read_text())
def call(path,data=None,token=None):
    headers={'Content-Type':'application/json'}
    if token: headers['satoken']=token
    req=urllib.request.Request(BASE+path,headers=headers,data=None if data is None else json.dumps(data).encode())
    with urllib.request.urlopen(req,timeout=60) as response: return json.load(response)
def sql(s): return mysql('USE '+DATABASE+'; '+s)
def login(name):
    return call('/api/public/auth/login/password',dict(username='demo_'+name,password=credentials()['demoPassword']))['data']

student=login('student'); admin=login('admin'); hr=login('hr')
try:
    # No network is attempted: no email host configured; no universal code added.
    records.append(dict(name='unconfigured SMTP fails closed',response=call('/api/public/auth/code',dict(email='synthetic@example.invalid',purpose='REGISTER'))))
    records.append(dict(name='registration without valid code rejected',response=call('/api/public/auth/register',dict(username='synthetic_new',email='synthetic_new@example.invalid',password=credentials()['demoPassword'],role='ADMIN',code='000000'))))
    # Error counters expose MySQL duplicate-key failures hidden by upstream global handler.
    records.append(dict(name='MySQL 1062 counter after concurrent match probe',result=mysql("SELECT ERROR_NUMBER,ERROR_NAME,SUM_ERROR_RAISED,SUM_ERROR_HANDLED FROM performance_schema.events_errors_summary_global_by_error WHERE ERROR_NUMBER=1062;")))
    # 201 newer zero-vector candidates push the older Java exact match out of recall.
    before=sql('SELECT MAX(id) FROM job;').splitlines()[-1]
    hrid=int(sql("SELECT id FROM sys_user WHERE username='demo_hr';").splitlines()[-1])
    rows=[f"({hrid},'M0_200_PROBE_{i}','合成企业A','上海','8000-10000','测试','200候选截断探针',1,1)" for i in range(201)]
    sql('INSERT INTO job(hr_id,job_name,company_name,city,salary_range,job_type,description,status,audit_status) VALUES '+','.join(rows)+';')
    recommended=call('/api/user/job/recommend?resumeId='+str(ids['resumeId'])+'&pageSize=50',token=student)
    records.append(dict(name='latest 200 drops older exact match',olderExactMatch=ids['jobId'],
                        eligibleCount=sql('SELECT COUNT(*) FROM job WHERE status=1 AND audit_status=1;').splitlines()[-1],
                        responseCode=recommended['code'],returnedIds=[j['id'] for j in recommended['data']],
                        scores=[j['matchScore'] for j in recommended['data']],
                        oldMatchReturned=any(j['id']==ids['jobId'] for j in recommended['data'])))
    assert not any(j['id']==ids['jobId'] for j in recommended['data'])
    # Keep synthetic probe rows for audit; close them rather than deleting records.
    sql('UPDATE job SET status=0 WHERE id>'+str(int(before))+" AND job_name LIKE 'M0_200_PROBE_%';")
    # Inactive resume is explicitly accepted by recommendation even though apply rejects it.
    resume=call('/api/user/resume/detail?id='+str(ids['resumeId']),token=student)['data']['resume']
    dto={key:resume.get(key) for key in ['id','resumeName','realName','gender','city','education','workYears','expectJobType','expectSalary','workDesc','status']}
    call('/api/user/resume/save',dict(dto,status=0),token=student)
    inactive=call('/api/user/job/recommend?resumeId='+str(ids['resumeId']),token=student)
    records.append(dict(name='inactive resume still recommended',response=inactive))
    call('/api/user/resume/save',dict(dto,status=1),token=student)
    # Updating job tags does NOT return an approved job to pending audit.
    java=int(sql("SELECT id FROM tag WHERE tag_name='Java';").splitlines()[-1])
    call('/api/hr/job/tags/save',dict(jobId=ids['jobId'],items=[dict(tagId=java,proficiency=3)]),token=hr)
    records.append(dict(name='skill edits bypass re-audit',jobStatus=sql('SELECT id,audit_status FROM job WHERE id='+str(ids['jobId'])+';')))
    # Disable account: role endpoints deny, common endpoint checks only session presence.
    sid=int(sql("SELECT id FROM sys_user WHERE username='demo_student';").splitlines()[-1])
    call('/api/admin/user/status/update',dict(id=sid,status=0),token=admin)
    records.append(dict(name='disabled old token on common me',response=call('/api/common/user/me',token=student)))
    records.append(dict(name='disabled old token on role resume',response=call('/api/user/resume/list',token=student)))
    call('/api/admin/user/status/update',dict(id=sid,status=1),token=admin)
    records.append(dict(name='application schema metadata',result=sql('SHOW CREATE TABLE job_apply;')))
    print('Supplemental probes complete: cutoff, concurrency DB error counter, auth, audit and inactive resume.')
finally:
    (ROOT/'docs/evidence/M0/supplemental-probes.json').write_text(json.dumps(records,ensure_ascii=False,indent=2))
