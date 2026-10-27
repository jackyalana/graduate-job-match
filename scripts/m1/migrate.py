#!/usr/bin/env python3
"""Apply the M1 migration only to a named project-local MySQL M1 schema."""
import re, sys
from pathlib import Path
sys.path.insert(0,str(Path(__file__).resolve().parents[1]/'m0'))
from local import ROOT, mysql
name=sys.argv[1]
if not re.fullmatch(r'graduate_job_match_m1_[a-z0-9_]+',name): raise SystemExit('Refusing non-M1 schema name')
port=mysql('SELECT @@port;').strip().splitlines()[-1]
if port!='13306': raise SystemExit('Unexpected server')
exists=mysql("SELECT SCHEMA_NAME FROM information_schema.SCHEMATA WHERE SCHEMA_NAME='"+name+"';").strip().splitlines()
if not exists or exists[-1]!=name: raise SystemExit('Schema does not exist')
check=mysql("SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='"+name+"' AND table_name IN ('sys_user','job','resume','student_profile');").strip().splitlines()[-1]
if check!='3': raise SystemExit('Expected untouched upstream schema; refusing repeat or partial migration')
sql=(ROOT/'backend/sql/migrations/V001__m1_foundation.sql').read_text()
mysql('USE `'+name+'`;\n'+sql)
print('Applied V001 to',name,'on isolated 127.0.0.1:13306; repeat is rejected')
