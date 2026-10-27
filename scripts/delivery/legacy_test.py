#!/usr/bin/env python3
"""Check an M2 application survives M3 migration without being misrepresented as a historical snapshot."""
import json,sys,urllib.request
from pathlib import Path
ROOT=Path(__file__).resolve().parents[2];sys.path.insert(0,str(ROOT/'scripts/m0'))
from local import LOCAL,credentials,mysql
name=json.loads((LOCAL/'m1-schemas.json').read_text())['runtime'];assert name.startswith('graduate_job_match_m1_runtime_')
assert mysql('USE `'+name+'`; SELECT COUNT(*) FROM job_apply;').strip().splitlines()[-1]=='1'
base='http://127.0.0.1:18081'
def login(user):
 data=json.dumps({'username':user,'password':credentials()['demoPassword']}).encode()
 with urllib.request.urlopen(urllib.request.Request(base+'/api/public/auth/login/password',data=data,headers={'Content-Type':'application/json'})) as r:return json.load(r)['data']
def get(path,token):
 with urllib.request.urlopen(urllib.request.Request(base+path,headers={'satoken':token})) as r:return json.load(r)['data']
student=get('/api/user/apply/list/detail',login('demo_student'))
assert len(student)==1 and student[0]['snapshotAvailable'] is False and student[0]['resume'] is None and student[0]['job'] is None
hr=get('/api/hr/job/applications?jobId=1',login('demo_hr'))
assert len(hr)==1 and hr[0]['snapshotAvailable'] is False and hr[0]['resume'] is None
print('PASS pre-V003 application retained; unavailable historical snapshot explicitly marked for student and HR')
