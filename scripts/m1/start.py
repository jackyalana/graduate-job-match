#!/usr/bin/env python3
"""Start M1 on its private database and port, independent of M0 services."""
import json, os, subprocess, sys, time, urllib.request
from pathlib import Path
sys.path.insert(0,str(Path(__file__).resolve().parents[1]/'m0'))
from local import ROOT,LOCAL,credentials,process_start
if (LOCAL/'m1-backend.pid').exists(): raise SystemExit('M1 pidfile exists; inspect before starting another')
name=json.loads((LOCAL/'m1-schemas.json').read_text())['runtime']
assert name.startswith('graduate_job_match_m1_runtime_')
jar=ROOT/'backend/target/backend-1.0.0-SNAPSHOT.jar'
url='jdbc:mysql://127.0.0.1:13306/'+name+'?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true'
env=dict(os.environ,M0_DB_PASSWORD=credentials()['database'],M0_STORAGE_DIR=str(LOCAL/'m1-uploads'))
command=[str(LOCAL/'jdk/bin/java'),'-Duser.timezone=UTC','-jar',str(jar),'--spring.profiles.active=demo','--server.port=18081','--spring.datasource.url='+url]
log=(LOCAL/'m1-backend.log').open('a')
p=subprocess.Popen(command,cwd=ROOT/'backend',env=env,stdout=log,stderr=subprocess.STDOUT,start_new_session=True)
(LOCAL/'m1-backend.pid').write_text(str(p.pid))
(LOCAL/'m1-backend.identity.json').write_text(json.dumps(dict(pid=p.pid,start=process_start(p.pid),marker=str(jar))))
for _ in range(40):
    if p.poll() is not None: raise RuntimeError('M1 process exited; see .local/m1-backend.log')
    try:
        with urllib.request.urlopen('http://127.0.0.1:18081/api/health',timeout=1) as response:
            print('M1 backend ready on 127.0.0.1:18081; schema',name,'pid',p.pid); break
    except Exception: time.sleep(.5)
else: raise RuntimeError('M1 startup timeout; see .local/m1-backend.log')
