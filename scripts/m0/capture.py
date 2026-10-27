#!/usr/bin/env python3
"""Capture reproducible environment/source identity; never serialize credentials."""
import hashlib
import json
import os
from pathlib import Path
import shutil
import subprocess
from local import ROOT,LOCAL

evidence=ROOT/'docs/evidence/M0'
env=dict(os.environ,JAVA_HOME=str(LOCAL/'jdk'))
commands=[['sw_vers'],['uname','-m'],['sysctl','-n','machdep.cpu.brand_string','hw.memsize'],
          [str(LOCAL/'jdk/bin/java'),'-version'],[str(LOCAL/'maven/bin/mvn'),'-v'],
          ['node','--version'],['npm','--version'],['python3','--version'],
          [str(LOCAL/'mysql/bin/mysqld'),'--version'],['git','--version']]
with (evidence/'environment.txt').open('w') as out:
    out.write('Initial PATH: Java stub with no JVM; Maven/MySQL/Redis/Docker/Podman absent; Node 26.8.2, npm 11.19.1.\n')
    out.write('M0 uses project-private tools; no system service installed. Redis absent and memory mode verified.\n')
    for command in commands:
        result=subprocess.run(command,capture_output=True,text=True,env=env)
        out.write('\n$ '+json.dumps(command)+'\n'+result.stdout+result.stderr+'exit_code='+str(result.returncode)+'\n')
digests={name:hashlib.sha256((LOCAL/'downloads'/(name+'.tar.gz')).read_bytes()).hexdigest() for name in ['jdk','maven','mysql']}
(evidence/'downloads-sha256.json').write_text(json.dumps(digests,indent=2))
paths=['backend/pom.xml','frontend/package.json','frontend/package-lock.json','backend/sql/init.sql',
       'backend/src/main/resources/application-demo.yml','backend/target/backend-1.0.0-SNAPSHOT.jar']
hashes={p:hashlib.sha256((ROOT/p).read_bytes()).hexdigest() for p in paths}
source_paths=['backend/src/main/java','backend/src/test','frontend/src','backend/pom.xml','frontend/package.json','frontend/package-lock.json']
diff=subprocess.run(['git','diff','cd755d13443ea4b4c98be86e6c692238ed566d31','--']+source_paths,cwd=ROOT,capture_output=True,text=True,check=True).stdout
assert not diff,'Unexpected production source/algorithm/dependency change'
(evidence/'artifact-manifest.json').write_text(json.dumps(dict(upstream='cd755d13443ea4b4c98be86e6c692238ed566d31',
    unchangedUpstreamPaths=source_paths,sha256=hashes,scope='demo config and M0 harness only'),indent=2))
for source,target in [('backend.log','backend-runtime.log'),('frontend.log','frontend-runtime.log'),('mysql-server.log','mysql-runtime.log')]:
    text=(LOCAL/source).read_text()
    # Defensive check before copying logs: no private credentials/tokens may leave .local.
    secrets=json.loads((LOCAL/'credentials.json').read_text())
    for secret in secrets.values():
        assert secret not in text,'Runtime log contains a credential'
    (evidence/target).write_text(text.replace('guanghaodeMacBook-Pro.local','[LOCAL-HOST]'))
print('Environment, binary hashes, unchanged business sources, runtime logs captured; no credentials.')
