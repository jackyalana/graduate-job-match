#!/usr/bin/env python3
"""Fresh project-local synthetic schema for M3/M4; never operates on external databases."""
import json,subprocess,sys
from pathlib import Path
ROOT=Path(__file__).resolve().parents[2];sys.path.insert(0,str(ROOT/'scripts/m0'))
from local import LOCAL,mysql
subprocess.run(['python3',str(ROOT/'scripts/m2/setup.py')],check=True)
name=json.loads((LOCAL/'m1-schemas.json').read_text())['runtime'];assert name.startswith('graduate_job_match_m1_runtime_')
assert mysql('SELECT @@port;').strip().splitlines()[-1]=='13306'
assert mysql('USE `'+name+'`; SELECT COUNT(*) FROM (SELECT user_id,job_id FROM job_apply GROUP BY user_id,job_id HAVING COUNT(*)>1) duplicates;').strip().splitlines()[-1]=='0'
sql=(ROOT/'backend/sql/migrations/V003__delivery_workflow.sql').read_text();assert 'DROP ' not in sql.upper() and 'TRUNCATE ' not in sql.upper()
mysql('USE `'+name+'`;\n'+sql);print('Applied V003 only to isolated schema',name)
