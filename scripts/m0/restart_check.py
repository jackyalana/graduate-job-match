#!/usr/bin/env python3
"""Verify persisted demo records and login after a real JVM restart."""
import json
import subprocess
import time
import urllib.request
from local import ROOT,LOCAL,DATABASE,mysql,credentials

def counts():
    return mysql('USE '+DATABASE+'; SELECT (SELECT COUNT(*) FROM sys_user) users,'
                 '(SELECT COUNT(*) FROM resume) resumes,(SELECT COUNT(*) FROM job) jobs,'
                 '(SELECT COUNT(*) FROM job_apply) applications;')
before=counts()
subprocess.run(['python3','scripts/m0/local.py','backend-stop'],cwd=ROOT,check=True)
time.sleep(1)
subprocess.run(['python3','scripts/m0/local.py','backend'],cwd=ROOT,check=True)
for _ in range(60):
    try:
        with urllib.request.urlopen('http://127.0.0.1:18080/api/health',timeout=1): break
    except Exception: time.sleep(.5)
else: raise RuntimeError('Restart failed')
body=json.dumps(dict(username='demo_student',password=credentials()['demoPassword'])).encode()
req=urllib.request.Request('http://127.0.0.1:18080/api/public/auth/login/password',data=body,headers={'Content-Type':'application/json'})
with urllib.request.urlopen(req) as res: login=json.load(res)
assert login['code']==0
req=urllib.request.Request('http://127.0.0.1:18080/api/user/apply/list/detail',headers={'satoken':login['data']})
with urllib.request.urlopen(req) as res: applications=json.load(res)
after=counts()
assert before==after
assert applications['code']==0 and len(applications['data'])>=3
result=dict(before=before,after=after,loginCode=login['code'],applicationCount=len(applications['data']),result='persisted after JVM restart')
(ROOT/'docs/evidence/M0/restart-results.json').write_text(json.dumps(result,ensure_ascii=False,indent=2))
print(json.dumps(result,ensure_ascii=False))
