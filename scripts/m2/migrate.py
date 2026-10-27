#!/usr/bin/env python3
"""Apply V002 only to an explicitly named project-local M1 demo database."""
import re,sys
from pathlib import Path
sys.path.insert(0,str(Path(__file__).resolve().parents[1]/'m0'))
from local import ROOT,mysql
if len(sys.argv)!=2 or not re.fullmatch(r'graduate_job_match_m1_[a-z0-9_]+',sys.argv[1]):
    raise SystemExit('Expected isolated M1 schema name')
name=sys.argv[1]
assert mysql('SELECT @@port;').strip().splitlines()[-1]=='13306'
existing=mysql("SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='"+name+"' AND table_name IN ('sys_user','job','resume','student_profile');").strip().splitlines()[-1]
new=mysql("SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='"+name+"' AND table_name IN ('recommendation_batch','skill_tag_alias');").strip().splitlines()[-1]
if existing!='4' or new!='0':raise SystemExit('Expected complete M1 schema with no V002 tables; refusing repeat or partial migration')
sql=(ROOT/'backend/sql/migrations/V002__m2_recommendation_batches.sql').read_text()
assert 'DROP ' not in sql.upper() and 'TRUNCATE ' not in sql.upper()
mysql('USE `'+name+'`;\n'+sql)
print('Applied V002 to isolated demo schema',name)
