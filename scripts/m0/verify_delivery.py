#!/usr/bin/env python3
"""Pre-push audit of M0 evidence and private credential exclusion."""
from pathlib import Path
import ast
import json
import subprocess
from local import ROOT,LOCAL

required=['docs/BASELINE.md','docs/REUSE_MATRIX.md','docs/M0_REPORT.md','docs/UPSTREAM_API_MAPPING.md',
          'THIRD_PARTY_NOTICES.md','LICENSE','docs/adr/0001-m0-runtime-and-upgrade.md']
for p in required: assert (ROOT/p).stat().st_size>0,p
for p in (ROOT/'scripts/m0').glob('*.py'): ast.parse(p.read_text())
paths=[]
for folder in ['docs','scripts/m0','backend/src/main/resources']:
    paths.extend(p for p in (ROOT/folder).rglob('*') if p.is_file() and '__pycache__' not in str(p))
paths.extend(ROOT/p for p in ['THIRD_PARTY_NOTICES.md','README.md','.gitignore'])
secrets=json.loads((LOCAL/'credentials.json').read_text())
for p in paths:
    raw=p.read_bytes()
    for secret in secrets.values(): assert secret.encode() not in raw,'Private credential found: '+str(p)
for path in ['.local/credentials.json','.local/mysql-root.cnf','.local/mysql-data-retry/ibdata1']:
    assert subprocess.run(['git','check-ignore','-q',path],cwd=ROOT).returncode==0,path
unchanged=['backend/src/main/java','backend/src/test','frontend/src','backend/pom.xml','frontend/package.json','frontend/package-lock.json']
subprocess.run(['git','diff','--exit-code','cd755d13443ea4b4c98be86e6c692238ed566d31','--']+unchanged,cwd=ROOT,check=True)
for log in ['backend-tests-retry.log','backend-package.log','frontend-install-final.log','frontend-build-retry.log',
            'api-smoke.log','supplemental-probes.log','restart-check.log']:
    assert 'exit_code=0' in (ROOT/'docs/evidence/M0'/log).read_text(),log
assert len(json.loads((ROOT/'docs/evidence/M0/smoke-results.json').read_text())['records'])==59
print('Pre-push check: required reports present; scripts parse; private credential scan passed for',len(paths),'files;')
print('private runtime/database ignored; upstream business code/tests/dependency manifests unchanged; evidence checks passed.')
print('This verifies packaging, not SDD acceptance. Status: pending review.')
