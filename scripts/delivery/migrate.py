#!/usr/bin/env python3
"""Apply V003 only to an explicitly selected project-local M2 demo schema."""
import re,sys
from pathlib import Path
sys.path.insert(0,str(Path(__file__).resolve().parents[1]/'m0'))
from local import ROOT,mysql
if len(sys.argv)!=2 or not re.fullmatch(r'graduate_job_match_m1_[a-z0-9_]+',sys.argv[1]):raise SystemExit('Expected isolated project schema')
name=sys.argv[1]
if mysql('SELECT @@port;').strip().splitlines()[-1]!='13306':raise SystemExit('Unexpected server')
existing=mysql("SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='"+name+"' AND table_name IN ('recommendation_batch','skill_tag_alias','job_apply');").strip().splitlines()[-1]
new=mysql("SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='"+name+"' AND table_name IN ('application_event','user_job_action','behavior_event');").strip().splitlines()[-1]
column=mysql("SELECT COUNT(*) FROM information_schema.columns WHERE table_schema='"+name+"' AND table_name='job_apply' AND column_name='resume_snapshot';").strip().splitlines()[-1]
if existing!='3' or new!='0' or column!='0':raise SystemExit('Expected complete M2 and no M3 schema; refusing repeat or partial migration')
dups=mysql('USE `'+name+'`; SELECT COUNT(*) FROM (SELECT user_id,job_id FROM job_apply GROUP BY user_id,job_id HAVING COUNT(*)>1) d;').strip().splitlines()[-1]
if dups!='0':raise SystemExit('Duplicate user/job records require manual, non-destructive audit; no data changed')
sql=(ROOT/'backend/sql/migrations/V003__delivery_workflow.sql').read_text();assert 'DROP ' not in sql.upper() and 'TRUNCATE ' not in sql.upper()
mysql('USE `'+name+'`;\n'+sql);print('Applied V003 to isolated demo schema',name,'existing applications retain legacy null snapshots')
