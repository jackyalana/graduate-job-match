#!/usr/bin/env python3
"""Back up and restore the synthetic M1 DB to a new isolated schema, never overwrite."""
import json, secrets, subprocess, sys
from pathlib import Path
sys.path.insert(0,str(Path(__file__).resolve().parents[1]/'m0'))
from local import ROOT,LOCAL,SOCKET,mysql
source=json.loads((LOCAL/'m1-schemas.json').read_text())['runtime']
assert source.startswith('graduate_job_match_m1_runtime_')
target='graduate_job_match_m1_restore_'+secrets.token_hex(4)
assert target not in set(mysql('SHOW DATABASES;').split())
folder=LOCAL/'m1-backups'; folder.mkdir(exist_ok=True)
backup=folder/(target+'.sql')
args=[str(LOCAL/'mysql/bin/mysqldump'),'--defaults-extra-file='+str(LOCAL/'mysql-root.cnf'),
      '--protocol=SOCKET','--socket='+SOCKET,'-uroot','--single-transaction','--skip-add-drop-table',
      '--skip-lock-tables','--no-tablespaces',source]
with backup.open('wb') as out: subprocess.run(args,stdout=out,stderr=subprocess.PIPE,check=True)
backup.chmod(0o600)
mysql('CREATE DATABASE `'+target+'` CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;')
command=[str(LOCAL/'mysql/bin/mysql'),'--defaults-extra-file='+str(LOCAL/'mysql-root.cnf'),
         '--protocol=SOCKET','--socket='+SOCKET,'-uroot',target]
with backup.open('rb') as stream: subprocess.run(command,stdin=stream,stdout=subprocess.PIPE,stderr=subprocess.PIPE,check=True)
for table in ['sys_user','student_profile','student_city_preference','resume','job','job_apply']:
    left=mysql('SELECT COUNT(*) FROM `'+source+'`.`'+table+'`;').strip().splitlines()[-1]
    right=mysql('SELECT COUNT(*) FROM `'+target+'`.`'+table+'`;').strip().splitlines()[-1]
    assert left==right,(table,left,right)
    print(table,'source count',left,'restored count',right)
print('PASS: synthetic backup restored into new schema',target,'backup is private under .local; no existing database overwritten.')
