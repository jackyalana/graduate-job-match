#!/usr/bin/env python3
"""Apply V004 only to a project-local M3 demo schema."""
import re,sys
from pathlib import Path
sys.path.insert(0,str(Path(__file__).resolve().parents[1]/'m0'))
from local import ROOT,mysql
if len(sys.argv)!=2 or not re.fullmatch(r'graduate_job_match_m1_[a-z0-9_]+',sys.argv[1]):raise SystemExit('Expected isolated project schema')
n=sys.argv[1]
if mysql('SELECT @@port;').strip().splitlines()[-1]!='13306':raise SystemExit('Unexpected server')
count=mysql("SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='"+n+"' AND table_name IN ('application_event','user_job_action','behavior_event');").strip().splitlines()[-1]
existing=mysql("SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='"+n+"' AND table_name='audit_log';").strip().splitlines()[-1]
if count!='3' or existing!='0':raise SystemExit('Expected complete V003 and no V004 table')
sql=(ROOT/'backend/sql/migrations/V004__audit_log.sql').read_text();mysql('USE `'+n+'`;\n'+sql);print('Applied V004 to isolated demo schema',n)
