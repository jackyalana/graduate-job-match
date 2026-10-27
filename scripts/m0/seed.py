#!/usr/bin/env python3
"""Seed only synthetic local accounts. Refuses a nonempty application schema."""
from pathlib import Path
import subprocess
from local import ROOT, LOCAL, DATABASE, mysql, credentials

jar=next((LOCAL/'m2/org/springframework/security/spring-security-crypto').glob('*/*.jar'))
subprocess.run([str(LOCAL/'jdk/bin/javac'), '-cp', str(jar), '-d', str(LOCAL),
                str(ROOT/'scripts/m0/HashPassword.java')], check=True)
password=credentials()['demoPassword']
hashed=subprocess.run([str(LOCAL/'jdk/bin/java'), '-cp', str(LOCAL)+':'+str(jar), 'HashPassword'],
                      input=password+'\n', text=True, capture_output=True, check=True).stdout
count=mysql('USE '+DATABASE+'; SELECT COUNT(*) FROM sys_user;').strip().splitlines()[-1]
if count!='0': raise RuntimeError('Refusing to seed a nonempty schema')
rows=[]
for username,role,audit,company in [('demo_admin','ADMIN',1,None),('demo_student','USER',1,None),
                                  ('demo_student2','USER',1,None),('demo_hr','HR',1,'合成企业A'),
                                  ('demo_hr2','HR',1,'合成企业B'),('demo_hr_pending','HR',0,'合成待审企业')]:
    company="NULL" if company is None else "'"+company+"'"
    rows.append("('"+username+"','"+hashed+"','"+username+"@example.invalid','"+role+"',"+company+",1,"+str(audit)+")")
mysql('USE '+DATABASE+'; INSERT INTO sys_user(username,password,email,role,company_name,status,audit_status) VALUES '
      + ','.join(rows) + "; INSERT INTO tag(tag_name,tag_type,base_weight,status) VALUES ('Java','SKILL',3,1),('MySQL','SKILL',3,1),('Vue','SKILL',3,1);"
      + "INSERT INTO notice(title,content,status) VALUES ('M0 合成演示环境','全部账号、岗位与简历均为合成测试数据。仅供本地底座验证。',1);")
print('Seeded 6 synthetic accounts, 3 skills and demo notice; no upstream personal/company/avatar data imported.')
