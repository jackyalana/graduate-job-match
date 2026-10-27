#!/usr/bin/env python3
"""Stop only the matching M1 process, after checking PID, start time, command and group."""
import json, os, signal, subprocess, sys
from pathlib import Path
sys.path.insert(0,str(Path(__file__).resolve().parents[1]/'m0'))
from local import ROOT,LOCAL,process_start

def stop(name):
    pidfile=LOCAL/('m1-'+name+'.pid'); identityfile=LOCAL/('m1-'+name+'.identity.json')
    if not pidfile.exists(): print(name, 'is not recorded'); return
    if not identityfile.exists(): raise RuntimeError('Missing identity; refusing signal')
    data=json.loads(identityfile.read_text()); pid=int(pidfile.read_text())
    marker={'backend':str(ROOT/'backend/target/backend-1.0.0-SNAPSHOT.jar'),
            'frontend':'npm run dev --host 127.0.0.1 --port 15174 --strictPort'}[name]
    command=subprocess.run(['/bin/ps','-p',str(pid),'-o','command='],capture_output=True,text=True)
    if command.returncode!=0: raise RuntimeError('PID missing; inspect pidfile manually')
    if (data['pid']!=pid or data['start']!=process_start(pid) or data['marker']!=marker
            or marker not in command.stdout or os.getpgid(pid)!=pid
            or (name=='backend' and '--server.port=18081' not in command.stdout)):
        raise RuntimeError('Process identity mismatch; refusing signal')
    os.killpg(pid,signal.SIGTERM); pidfile.unlink(); identityfile.unlink(); print('Stopped verified M1',name,pid)

if __name__=='__main__':
    selected=sys.argv[1] if len(sys.argv)>1 else 'all'
    if selected not in ['all','backend','frontend']: raise SystemExit('Use all, backend or frontend')
    for item in (['frontend','backend'] if selected=='all' else [selected]): stop(item)
