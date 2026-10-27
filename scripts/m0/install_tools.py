#!/usr/bin/env python3
"""Optional project-private macOS arm64 tools, no system settings/services.
Downloads are pinned to the actual M0 binaries and their observed SHA256.
These hashes identify M0 bytes; they are not a claim of vendor signature verification.
"""
from pathlib import Path
import hashlib
import platform
import posixpath
import subprocess
import sys
import tarfile
from local import LOCAL

if (platform.system(),platform.machine())!=('Darwin','arm64'):
    raise RuntimeError('This bootstrap is for the verified macOS arm64 route only')
packages=[
 ('jdk','https://cdn.azul.com/zulu/bin/zulu8.98.0.107-ca-jdk8.0.512-macosx_aarch64.tar.gz',
  'f26e2a91062542cf22ef2171b2556e1c3f42cf87cf174258b3df7840365892aa',
  'zulu8.98.0.107-ca-jdk8.0.512-macosx_aarch64/Contents/Home'),
 ('maven','https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/3.9.11/apache-maven-3.9.11-bin.tar.gz',
  None,'apache-maven-3.9.11'),
 ('mysql','https://cdn.mysql.com/Downloads/MySQL-8.4/mysql-8.4.11-macos15-arm64.tar.gz',
  None,'mysql-8.4.11-macos15-arm64')]
# Other two observed hashes are stored in downloads-sha256.json by capture.py.
observed=__import__('json').loads((LOCAL.parent/'docs/evidence/M0/downloads-sha256.json').read_text())
(LOCAL/'downloads').mkdir(parents=True,exist_ok=True)
for name,url,digest,target in packages:
    archive=LOCAL/'downloads'/(name+'.tar.gz')
    digest=digest or observed[name]
    if not archive.exists(): subprocess.run(['curl','-fL','--retry','2','-o',str(archive),url],check=True)
    if hashlib.sha256(archive.read_bytes()).hexdigest()!=digest: raise RuntimeError(name+' SHA256 mismatch')
    link=LOCAL/name
    validate_only='--validate-only' in sys.argv
    if link.exists() and not validate_only: print(name,'already available'); continue
    # tar from official vendor archive; reject absolute or parent paths, including link targets.
    with tarfile.open(archive) as tar:
        for member in tar.getmembers():
            if member.name.startswith('/') or '..' in Path(member.name).parts: raise RuntimeError('Unsafe archive path')
            if member.issym() or member.islnk():
                target_name=posixpath.normpath(posixpath.join(posixpath.dirname(member.name),member.linkname)
                                               if member.issym() else member.linkname)
                if (member.linkname.startswith('/') or target_name.startswith('../')
                        or target_name.split('/')[0]!=member.name.split('/')[0]):
                    raise RuntimeError('Unsafe archive link')
        if not validate_only: tar.extractall(LOCAL)
    if validate_only:
        print(name,'archive paths and confined links validated'); continue
    link.symlink_to(target)
    print(name,'installed privately')
