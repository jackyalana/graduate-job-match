#!/usr/bin/env python3
"""Check a mismatched identity cannot signal even a child process we own."""
import importlib.util, json, subprocess, tempfile
from pathlib import Path
spec=importlib.util.spec_from_file_location('m1stop',Path(__file__).with_name('stop.py'))
module=importlib.util.module_from_spec(spec); spec.loader.exec_module(module)
child=subprocess.Popen(['/bin/sleep','30'],start_new_session=True)
try:
    with tempfile.TemporaryDirectory() as d:
        module.LOCAL=Path(d)
        (module.LOCAL/'m1-frontend.pid').write_text(str(child.pid))
        (module.LOCAL/'m1-frontend.identity.json').write_text(json.dumps({'pid':child.pid,'start':module.process_start(child.pid),'marker':'npm run dev --host 127.0.0.1 --port 15174 --strictPort'}))
        try: module.stop('frontend')
        except RuntimeError as e: assert 'identity mismatch' in str(e)
        else: raise AssertionError('mismatched process should be rejected')
        assert child.poll() is None
        print('PASS: unrelated command not signalled; no M1 process group affected')
finally:
    child.terminate(); child.wait()
