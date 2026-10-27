#!/usr/bin/env python3
"""M0-only local runtime. Never targets a user/system database."""
from pathlib import Path
import json
import os
import secrets
import signal
import subprocess
import sys
import time
import urllib.request

ROOT = Path(__file__).resolve().parents[2]
LOCAL = ROOT / '.local'
SOCKET = str(LOCAL / 'mysql.sock')
MYSQL = str(LOCAL / 'mysql/bin/mysql')
DATABASE = 'graduate_job_match_m0'

def spawn(name, command, cwd=ROOT, env=None):
    pidfile = LOCAL / (name + '.pid')
    if pidfile.exists():
        try:
            os.kill(int(pidfile.read_text()), 0)
            raise RuntimeError(name + ' already running; use status')
        except ProcessLookupError:
            pass
    log = (LOCAL / (name + '.log')).open('a')
    process = subprocess.Popen(command, cwd=cwd, env=env, stdout=log,
                               stderr=subprocess.STDOUT, start_new_session=True)
    pidfile.write_text(str(process.pid))
    print(name, 'pid', process.pid, 'log', str(log.name))
    return process

def mysql(sql, initial=False):
    command = [MYSQL, '--no-defaults' if initial else '--defaults-extra-file='+str(LOCAL/'mysql-root.cnf'),
               '--protocol=SOCKET', '--socket='+SOCKET, '-uroot', '--batch']
    return subprocess.run(command, input=sql, text=True, capture_output=True, check=True).stdout

def credentials():
    return json.loads((LOCAL / 'credentials.json').read_text())

def main():
    action = sys.argv[1]
    if action == 'db-start':
        spawn('mysql', [str(LOCAL/'mysql/bin/mysqld'), '--no-defaults',
              '--basedir='+str(LOCAL/'mysql'), '--datadir='+str(LOCAL/'mysql-data-retry'),
              '--bind-address=127.0.0.1', '--port=13306', '--socket='+SOCKET,
              '--mysqlx=OFF', '--default-time-zone=+00:00', '--log-error='+str(LOCAL/'mysql-server.log')])
        for _ in range(60):
            if Path(SOCKET).exists():
                print('socket ready'); return
            time.sleep(.5)
        raise RuntimeError('MySQL not ready; inspect .local/mysql-server.log')
    elif action == 'db-setup':
        if (LOCAL/'credentials.json').exists():
            raise RuntimeError('Already initialized: refusing to overwrite credentials or database')
        # Only our freshly initialized server; fail if any application database exists.
        existing = mysql('SHOW DATABASES;', initial=True)
        allowed = {'Database', 'information_schema', 'mysql', 'performance_schema', 'sys'}
        if set(existing.split()) - allowed:
            raise RuntimeError('Unexpected database: refusing setup')
        data = {key: secrets.token_urlsafe(24) for key in ['root', 'database', 'demoPassword']}
        schema = (ROOT/'backend/sql/init.sql').read_text().replace('lan_recruitment', DATABASE)
        # init.sql reviewed: CREATE/USE only, no DROP/TRUNCATE/DELETE.
        mysql(schema + "\nCREATE USER 'graduate_demo'@'127.0.0.1' IDENTIFIED BY '"+data['database']+"';\n"
              + "GRANT SELECT,INSERT,UPDATE,DELETE ON "+DATABASE+".* TO 'graduate_demo'@'127.0.0.1';\n"
              + "ALTER USER 'root'@'localhost' IDENTIFIED BY '"+data['root']+"';", initial=True)
        for name, content in [('credentials.json', json.dumps(data)),
                              ('mysql-root.cnf', '[client]\npassword='+data['root']+'\n')]:
            path=LOCAL/name; path.write_text(content); path.chmod(0o600)
        print(mysql('SELECT @@version,@@port,@@hostname,@@global.time_zone; SHOW DATABASES;'))
        print('Schema imported into fresh private database; random credentials in .local/credentials.json')
    elif action == 'backend':
        data = credentials()
        env = dict(os.environ, M0_DB_PASSWORD=data['database'], M0_STORAGE_DIR=str(LOCAL/'uploads'))
        jars=list((ROOT/'backend/target').glob('backend-*.jar'))
        if len(jars)!=1: raise RuntimeError('Package backend first')
        spawn('backend', [str(LOCAL/'jdk/bin/java'), '-Duser.timezone=UTC', '-jar', str(jars[0]),
              '--spring.profiles.active=demo'], cwd=ROOT/'backend', env=env)
    elif action == 'frontend':
        spawn('frontend', ['npm', 'run', 'dev', '--', '--host', '127.0.0.1', '--port', '15173', '--strictPort'],
              cwd=ROOT/'frontend', env=dict(os.environ, VITE_API_BASE_URL='http://127.0.0.1:18080'))
    elif action == 'status':
        print(mysql('SELECT @@version,@@port,@@global.time_zone; USE '+DATABASE+'; SHOW TABLES;'))
        for url in ['http://127.0.0.1:18080/api/health', 'http://127.0.0.1:15173']:
            try:
                with urllib.request.urlopen(url, timeout=3) as response: print(url,response.status)
            except Exception as error: print(url,str(error))
    elif action == 'accounts':
        print('LOCAL DEMO ONLY; do not paste this output into reports or GitHub.')
        print('Usernames: demo_admin / demo_student / demo_student2 / demo_hr / demo_hr2 / demo_hr_pending')
        print('Random password shared by these synthetic accounts:', credentials()['demoPassword'])
    elif action == 'backend-stop':
        path=LOCAL/'backend.pid'
        if path.exists():
            os.killpg(int(path.read_text()), signal.SIGTERM)
            path.unlink()
    elif action == 'stop':
        # PIDs belong to this script's detached process groups, never kill arbitrary ports.
        for name in ['frontend','backend','mysql']:
            path=LOCAL/(name+'.pid')
            if path.exists():
                try: os.killpg(int(path.read_text()), signal.SIGTERM)
                except ProcessLookupError: pass
                path.unlink()
    else: raise ValueError(action)

if __name__=='__main__': main()
