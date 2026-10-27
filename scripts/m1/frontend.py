#!/usr/bin/env python3
"""Start an isolated M1 Vite instance on loopback."""
import json, os, subprocess, sys, time, urllib.request
from pathlib import Path
sys.path.insert(0,str(Path(__file__).resolve().parents[1]/'m0'))
from local import ROOT,LOCAL,process_start
pidfile=LOCAL/'m1-frontend.pid'
if pidfile.exists(): raise SystemExit('M1 frontend pidfile exists; inspect before starting')
log=(LOCAL/'m1-frontend.log').open('a')
p=subprocess.Popen(['npm','run','dev','--','--host','127.0.0.1','--port','15174','--strictPort'],cwd=ROOT/'frontend',
    env=dict(os.environ,VITE_API_BASE_URL='http://127.0.0.1:18081',VITE_DEMO='true'),stdout=log,stderr=subprocess.STDOUT,start_new_session=True)
pidfile.write_text(str(p.pid))
(LOCAL/'m1-frontend.identity.json').write_text(json.dumps(dict(pid=p.pid,start=process_start(p.pid),marker='npm run dev --host 127.0.0.1 --port 15174 --strictPort')))
for _ in range(30):
    if p.poll() is not None: raise RuntimeError('M1 frontend exited; inspect .local/m1-frontend.log')
    try:
        with urllib.request.urlopen('http://127.0.0.1:15174/',timeout=1): print('M1 frontend ready at http://127.0.0.1:15174/ pid',p.pid); break
    except Exception: time.sleep(.5)
else: raise RuntimeError('M1 frontend startup timeout')
