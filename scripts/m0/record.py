#!/usr/bin/env python3
"""Run an explicit command, preserving output, cwd, timestamps and exit code."""
import datetime
import json
import os
from pathlib import Path
import subprocess
import sys

ROOT = Path(__file__).resolve().parents[2]
EVIDENCE = ROOT / 'docs/evidence/M0'
EVIDENCE.mkdir(parents=True, exist_ok=True)
name, cwd, *command = sys.argv[1:]
start = datetime.datetime.now(datetime.timezone.utc).isoformat()
with (EVIDENCE / name).open('w') as log:
    log.write(f'cwd={cwd}\ncommand={json.dumps(command)}\nstart={start}\n')
    log.flush()
    try:
        result = subprocess.run(command, cwd=cwd, stdout=log, stderr=subprocess.STDOUT)
        code = result.returncode
    except OSError as error:
        log.write(str(error) + '\n')
        code = 127
    log.write(f'\nexit_code={code}\n')
entry = dict(start=start, end=datetime.datetime.now(datetime.timezone.utc).isoformat(),
             cwd=cwd, command=command, exit_code=code, log=name)
with (EVIDENCE / 'commands.jsonl').open('a') as out:
    out.write(json.dumps(entry, ensure_ascii=False) + '\n')
print(json.dumps(entry, ensure_ascii=False))
sys.exit(code)
