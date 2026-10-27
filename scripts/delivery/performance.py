#!/usr/bin/env python3
"""Synthetic scale test on a uniquely created local demo database. Warmup 30s, sample 60s."""
import concurrent.futures,json,platform,statistics,subprocess,sys,time,urllib.request,urllib.error
from pathlib import Path
ROOT=Path(__file__).resolve().parents[2];sys.path.insert(0,str(ROOT/'scripts/m0'))
from local import LOCAL,credentials,mysql
name=json.loads((LOCAL/'m1-schemas.json').read_text())['runtime'];assert name.startswith('graduate_job_match_m1_runtime_')
assert mysql('SELECT @@port;').strip().splitlines()[-1]=='13306'
assert mysql('USE `'+name+'`; SELECT COUNT(*) FROM job;').strip().splitlines()[-1]=='0'
# Fresh demo has users 1..5, of which 2 and 3 are students; create 998 synthetic students.
for start in range(0,998,250):
 rows=["('bench_%04d','not-a-login','bench_%04d@example.invalid','USER',1,1)"%(i,i) for i in range(start,min(998,start+250))]
 mysql('USE `'+name+'`; INSERT INTO sys_user(username,password,email,role,status,audit_status) VALUES '+','.join(rows)+';')
for start in range(0,2000,250):
 rows=["(4,'Synthetic performance job %04d','SyntheticCo','Shanghai','Dev',1,1,UTC_TIMESTAMP(),1)"%i for i in range(start,min(2000,start+250))]
 mysql('USE `'+name+'`; INSERT INTO job(hr_id,job_name,company_name,city,job_type,status,audit_status,created_at,accept_fresh_graduate) VALUES '+','.join(rows)+';')
users=[2,3]+list(range(6,1004));assert len(users)==1000
counter=0
for batch_start in range(0,20000,500):
 rows=[]
 for i in range(batch_start,batch_start+500):
  user=users[i%1000];job=1+(i//1000);rows.append("(%d,%d,'VIEW',DATE_SUB(UTC_TIMESTAMP(),INTERVAL 1 DAY),'bench-%d')"%(user,job,i))
 mysql('USE `'+name+'`; INSERT INTO behavior_event(user_id,job_id,event_type,event_time,dedupe_key) VALUES '+','.join(rows)+';');counter+=len(rows)
print('seeded synthetic students=1000 jobs=2000 behavior=20000',flush=True)
base='http://127.0.0.1:18081';payload=json.dumps({'username':'demo_student','password':credentials()['demoPassword']}).encode()
with urllib.request.urlopen(urllib.request.Request(base+'/api/public/auth/login/password',data=payload,headers={'Content-Type':'application/json'}),timeout=30) as r: token=json.load(r)['data']
def request(path,data=None):
 headers={'satoken':token}
 if data is not None:headers['Content-Type']='application/json';data=json.dumps(data).encode()
 started=time.perf_counter()
 try:
  with urllib.request.urlopen(urllib.request.Request(base+path,data=data,headers=headers),timeout=40) as r:body=json.load(r);status=r.status
  return (time.perf_counter()-started)*1000,status,body.get('data')
 except Exception:return (time.perf_counter()-started)*1000,0,None
start=time.monotonic();warm_until=start+30;end=warm_until+60;records=[]
def worker(index):
 local=[]
 while time.monotonic()<end:
  for kind,path,body in [('list','/api/jobs?page=1&pageSize=10',None),('batch','/api/student/recommendation-batches',{})]:
   if time.monotonic()>=end:break
   now=time.monotonic();ms,status,result=request(path,body)
   if now>=warm_until:local.append((kind,ms,status))
   if kind=='batch' and status==200 and isinstance(result,dict):
    now=time.monotonic();ms,status,_=request('/api/student/recommendation-batches/'+result['batchId']+'?page=1&pageSize=10')
    if now>=warm_until:local.append(('page',ms,status))
 return local
with concurrent.futures.ThreadPoolExecutor(max_workers=10) as pool:
 for result in pool.map(worker,range(10)):records.extend(result)
def p95(values):
 values=sorted(values);return values[max(0,int((len(values)-1)*.95))] if values else None
summary={}
for kind in ('list','batch','page'):
 values=[ms for k,ms,status in records if k==kind and status==200];allrows=[(ms,status) for k,ms,status in records if k==kind]
 summary[kind]={'requests':len(allrows),'success':len(values),'errors':len(allrows)-len(values),'p95_ms':round(p95(values),2) if values else None}
result={'synthetic':True,'students':1000,'jobs':2000,'behaviorEvents':20000,'concurrency':10,'warmupSeconds':30,'sampleSeconds':60,'wallSeconds':round(time.monotonic()-start,2),'platform':platform.platform(),'python':platform.python_version(),'results':summary,'errorRate':round(sum(x['errors'] for x in summary.values())/max(1,len(records)),6)}
(ROOT/'docs/evidence/delivery/performance.json').write_text(json.dumps(result,indent=2)+'\n')
print(json.dumps(result,indent=2),flush=True)
