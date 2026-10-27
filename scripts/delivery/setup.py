#!/usr/bin/env python3
"""Fresh project-local synthetic schema for M3/M4; never operates on external databases."""
import json,subprocess,sys
from pathlib import Path
ROOT=Path(__file__).resolve().parents[2];sys.path.insert(0,str(ROOT/'scripts/m0'))
from local import LOCAL,mysql
subprocess.run(['python3',str(ROOT/'scripts/m2/setup.py')],check=True)
name=json.loads((LOCAL/'m1-schemas.json').read_text())['runtime'];assert name.startswith('graduate_job_match_m1_runtime_')
subprocess.run(['python3',str(ROOT/'scripts/delivery/migrate.py'),name],check=True)
subprocess.run(['python3',str(ROOT/'scripts/delivery/audit_migrate.py'),name],check=True)
print('Prepared isolated synthetic schema',name)
