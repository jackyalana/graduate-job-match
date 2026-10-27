#!/usr/bin/env python3
"""Verify the normal profile refuses code-less registration even when the demo flag is supplied."""
import json, os, subprocess, sys, time, urllib.error, urllib.request
from pathlib import Path
sys.path.insert(0,str(Path(__file__).resolve().parents[1]/'m0'))
from local import ROOT,LOCAL,credentials
name=json.loads((LOCAL/'m1-schemas.json').read_text())['runtime']
assert name.startswith('graduate_job_match_m1_runtime_')
jar=ROOT/'backend/target/backend-1.0.0-SNAPSHOT.jar'
url='jdbc:mysql://127.0.0.1:13306/'+name+'?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true'
env=dict(os.environ,APP_DB_URL=url,APP_DB_USER='graduate_demo',APP_DB_PASSWORD=credentials()['database'])
command=[str(LOCAL/'jdk/bin/java'),'-Duser.timezone=UTC','-jar',str(jar),'--spring.profiles.active=dev',
         '--server.address=127.0.0.1','--server.port=18082','--lan.storage.base-dir='+str(LOCAL/'m1-normal-uploads'),
         '--lan.demo.local-registration=true']
with (LOCAL/'m1-normal-auth.log').open('w') as log:
    process=subprocess.Popen(command,cwd=ROOT/'backend',env=env,stdout=log,stderr=subprocess.STDOUT,start_new_session=True)
    try:
        for _ in range(40):
            if process.poll() is not None: raise RuntimeError('normal-profile process exited')
            try:
                with urllib.request.urlopen('http://127.0.0.1:18082/api/health',timeout=1): break
            except Exception: time.sleep(.5)
        else: raise RuntimeError('normal-profile startup timeout')
        payload=json.dumps({'username':'m1_normal_blocked','password':credentials()['demoPassword'],'email':'m1_normal@example.invalid','role':'USER'}).encode()
        req=urllib.request.Request('http://127.0.0.1:18082/api/public/auth/register',data=payload,headers={'Content-Type':'application/json'})
        try: response=urllib.request.urlopen(req,timeout=10)
        except urllib.error.HTTPError as e: response=e
        result=json.load(response)
        assert response.status==400 and result['code']=='EMAIL_CODE_INVALID',(response.status,result)
        assert result.get('requestId')==response.headers.get('X-Request-Id')
        payload=json.dumps({'username':'m1_normal_blocked','password':credentials()['demoPassword'],'email':'m1_normal@example.invalid','role':'USER','code':'000000'}).encode()
        req=urllib.request.Request('http://127.0.0.1:18082/api/public/auth/register',data=payload,headers={'Content-Type':'application/json'})
        try: response=urllib.request.urlopen(req,timeout=10)
        except urllib.error.HTTPError as e: response=e
        result=json.load(response)
        assert response.status==400 and result['code']=='EMAIL_CODE_INVALID',(response.status,result)
        print('PASS: dev profile rejects absent and fixed 000000 codes even when demo flag is true; HTTP 400, EMAIL_CODE_INVALID.')
    finally:
        process.terminate()
        try: process.wait(timeout=10)
        except subprocess.TimeoutExpired: process.kill(); process.wait()
