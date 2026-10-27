#!/usr/bin/env python3
"""Create uniquely named synthetic M1 schemas, leaving M0 and other databases untouched."""
import json, secrets, subprocess, sys
from pathlib import Path
sys.path.insert(0,str(Path(__file__).resolve().parents[1]/'m0'))
from local import ROOT,LOCAL,mysql,credentials
suffix=secrets.token_hex(4)
names={kind:'graduate_job_match_m1_'+kind+'_'+suffix for kind in ('empty','upgrade','runtime')}
assert mysql('SELECT @@port;').strip().splitlines()[-1]=='13306'
existing=set(mysql('SHOW DATABASES;').split())
assert not any(name in existing for name in names.values())
init=(ROOT/'backend/sql/init.sql').read_text().splitlines()
assert init[0].startswith('CREATE DATABASE') and init[1].strip()=='USE lan_recruitment;'
schema='\n'.join(init[2:])
for name in names.values(): mysql('CREATE DATABASE `'+name+'` CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci; USE `'+name+'`;\n'+schema)
jar=next((LOCAL/'m2/org/springframework/security/spring-security-crypto').glob('*/*.jar'))
subprocess.run([str(LOCAL/'jdk/bin/javac'),'-cp',str(jar),'-d',str(LOCAL),str(ROOT/'scripts/m0/HashPassword.java')],check=True)
hashval=subprocess.run([str(LOCAL/'jdk/bin/java'),'-cp',str(LOCAL)+':'+str(jar),'HashPassword'],input=credentials()['demoPassword']+'\n',text=True,capture_output=True,check=True).stdout
rows=[]
for username,role,audit,company in [('demo_admin','ADMIN',1,None),('demo_student','USER',1,None),('demo_student2','USER',1,None),('demo_hr','HR',1,'合成企业A'),('demo_hr2','HR',1,'合成企业B')]:
    company="NULL" if company is None else "'"+company+"'"
    rows.append("('"+username+"','"+hashval+"','"+username+"@example.invalid','"+role+"',"+company+",1,"+str(audit)+")")
for key in ['upgrade','runtime']:
    name=names[key]
    mysql('USE `'+name+'`; INSERT INTO sys_user(username,password,email,role,company_name,status,audit_status) VALUES '+','.join(rows)+"; INSERT INTO resume(user_id,resume_name,real_name,education,is_default,status) VALUES (2,'合成默认简历','合成学生A','本科',1,1),(2,'合成旧简历','合成学生A','博士',0,1),(3,'未知学历简历','合成学生B','未知学历',1,1); INSERT INTO tag(tag_name,tag_type,base_weight,status) VALUES ('Java','SKILL',3,1);")
for key,name in names.items():
    result=subprocess.run(['python3',str(ROOT/'scripts/m1/migrate.py'),name],text=True,capture_output=True)
    print(key, 'migration exit',result.returncode,result.stdout.strip(),result.stderr.strip())
    assert result.returncode==0
    repeat=subprocess.run(['python3',str(ROOT/'scripts/m1/migrate.py'),name],text=True,capture_output=True)
    print(key,'repeat exit',repeat.returncode,repeat.stderr.strip())
    assert repeat.returncode!=0
    if key!='empty':
        observed=mysql('USE `'+name+'`; SELECT user_id,COALESCE(education_rank,0),profile_version FROM student_profile ORDER BY user_id;').strip()
        print(key,'backfill',observed.replace('\n','; '))
        assert '2\t2\t1' in observed and '3\t0\t1' in observed
    else:
        assert mysql('USE `'+name+'`; SELECT COUNT(*) FROM student_profile;').strip().splitlines()[-1]=='0'
mysql('GRANT SELECT,INSERT,UPDATE,DELETE ON `'+names['runtime']+'`.* TO \'graduate_demo\'@\'127.0.0.1\';')
(LOCAL/'m1-schemas.json').write_text(json.dumps(names)); print('M1 database names',json.dumps(names)); print('M0 database was not changed.')
