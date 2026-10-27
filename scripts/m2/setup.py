#!/usr/bin/env python3
"""Create fresh isolated synthetic M1 schemas and migrate the new runtime schema to M2."""
import json, subprocess, sys
from pathlib import Path
ROOT=Path(__file__).resolve().parents[2]
sys.path.insert(0,str(ROOT/'scripts/m0'))
from local import LOCAL,mysql
subprocess.run(['python3',str(ROOT/'scripts/m1/setup_test_databases.py')],check=True)
name=json.loads((LOCAL/'m1-schemas.json').read_text())['runtime']
assert name.startswith('graduate_job_match_m1_runtime_')
subprocess.run(['python3',str(ROOT/'scripts/m2/migrate.py'),name],check=True)
print('M2 migration applied to isolated synthetic runtime schema',name)
