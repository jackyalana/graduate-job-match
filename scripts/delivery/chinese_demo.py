#!/usr/bin/env python3
"""Add fictitious Chinese demonstration data via the local demo application's APIs.

Set DELIVERY_DEMO_PASSWORD locally; no password or database content is stored in Git.
Completed runs are not replayed, preserving subsequent user edits.
"""
import datetime, getpass, json, os, subprocess, sys, urllib.request, urllib.error
from pathlib import Path
ROOT=Path(__file__).resolve().parents[2]; sys.path.insert(0,str(ROOT/'scripts/m0'))
from local import LOCAL,mysql
DB=json.loads((LOCAL/'m1-schemas.json').read_text())['runtime']
assert DB.startswith('graduate_job_match_m1_runtime_') and DB.replace('_','').isalnum()
BASE='http://127.0.0.1:18081'
statefile=LOCAL/('chinese-demo-'+DB+'.json')
state=json.loads(statefile.read_text()) if statefile.exists() else {'jobs':{},'users':{},'applications':{}}
def save():
    temp=statefile.with_suffix('.tmp'); temp.write_text(json.dumps(state,ensure_ascii=False,indent=2)); temp.chmod(0o600); temp.replace(statefile)
def sql(q): return mysql('USE `'+DB+'`; '+q)
def api(path,token=None,data=None,method=None):
    headers={}
    if token: headers['satoken']=token
    if data is not None: headers['Content-Type']='application/json'; data=json.dumps(data,ensure_ascii=False).encode()
    req=urllib.request.Request(BASE+path,data=data,headers=headers,method=method)
    try:
        with urllib.request.urlopen(req,timeout=60) as response: result=json.load(response)
    except urllib.error.HTTPError as e: raise RuntimeError(path+' HTTP '+str(e.code)+' '+e.read().decode()[:500]) from None
    if result.get('code')!='OK': raise RuntimeError(path+' '+str(result))
    return result.get('data')
def login(name): return api('/api/public/auth/login/password',data={'username':name,'password':password})
def tally():
    names=['sys_user','job','resume','job_apply','application_event','user_job_action','behavior_event']
    out={}
    for name in names:
        try: out[name]=int(sql('SELECT COUNT(*) FROM `'+name+'`;').splitlines()[-1])
        except subprocess.CalledProcessError: pass
    return out
if state.get('complete'):
    print('演示数据已初始化；保留之后的用户操作。',json.dumps(state.get('result',{}),ensure_ascii=False)); sys.exit(0)
password=os.environ.get('DELIVERY_DEMO_PASSWORD')
if not password and sys.stdin.isatty(): password=getpass.getpass('请设置本地演示账号密码（不会显示或写入仓库）：')
if not password: raise SystemExit('请在终端交互输入，或通过 DELIVERY_DEMO_PASSWORD 提供演示密码。')
pid=int((LOCAL/'m1-backend.pid').read_text())
command=subprocess.run(['/bin/ps','-p',str(pid),'-o','command='],capture_output=True,text=True,check=True).stdout
if '--server.port=18081' not in command or '/'+DB+'?' not in command: raise SystemExit('后端进程与预期演示库不匹配，停止。')
if not state.get('namespace_checked'):
    # Refuse to adopt an unrelated account or existing job on the first run.
    # Resumed runs keep this local marker so interrupted API operations can be recovered.
    prefix_count=int(sql("SELECT COUNT(*) FROM sys_user WHERE LEFT(username,7)='zhdemo_';").splitlines()[-1])
    if prefix_count: raise SystemExit('发现已有 zhdemo_ 账号，无法确认其归属；未导入。')
    titles=['Java 后端开发工程师','Vue 前端开发工程师','软件测试工程师','数据分析助理','运维工程师','产品助理']
    title_sql=','.join("'"+title+suffix+"'" for title in titles for suffix in ('（校招）','（成长岗位）'))
    collision=int(sql("SELECT COUNT(*) FROM job WHERE hr_id IN (SELECT id FROM sys_user WHERE username IN ('demo_hr','demo_hr2')) AND job_name IN ("+title_sql+");").splitlines()[-1])
    if collision: raise SystemExit('发现同名既有岗位，无法确认其归属；未导入。')
    state['namespace_checked']=True; save()
if not state.get('before'): state['before']=tally(); save()
if not state.get('passwords_initialized'):
    backup=LOCAL/('chinese-demo-password-hashes-'+DB+'.json')
    names="'demo_admin','demo_student','demo_student2','demo_hr','demo_hr2'"
    rows=sql('SELECT username,password FROM sys_user WHERE username IN ('+names+') ORDER BY username;')
    assert len(rows.strip().splitlines())==6, '五个既有演示账号不完整'
    if not backup.exists():
        fd=os.open(str(backup),os.O_WRONLY|os.O_CREAT|os.O_EXCL,0o600)
        with os.fdopen(fd,'w') as f: json.dump(rows,f)
    jar=next((LOCAL/'m2/org/springframework/security/spring-security-crypto').glob('*/*.jar'))
    subprocess.run([str(LOCAL/'jdk/bin/javac'),'-cp',str(jar),'-d',str(LOCAL),str(ROOT/'scripts/m0/HashPassword.java')],check=True)
    h=subprocess.run([str(LOCAL/'jdk/bin/java'),'-cp',str(LOCAL)+':'+str(jar),'HashPassword'],input=password+'\n',text=True,capture_output=True,check=True).stdout
    assert h.startswith('$2') and "'" not in h
    sql("UPDATE sys_user SET password='"+h+"' WHERE username IN ("+names+");")
    state['passwords_initialized']=True; save()
admin=login('demo_admin')
print('账号连接正常，开始补充基础资料。',flush=True)
roles=[('Java 后端','Java 后端开发工程师',['Java','Spring Boot','MySQL'],8000,13000),('Vue 前端','Vue 前端开发工程师',['Vue','JavaScript'],7500,12000),('软件测试','软件测试工程师',['软件测试','SQL'],6500,10500),('数据分析','数据分析助理',['Python','SQL','数据分析'],7500,12000),('运维工程','运维工程师',['Linux','Redis'],7000,11500),('产品助理','产品助理',['产品设计','数据分析'],6500,10000)]
tags=api('/api/public/tag/list')
for name in sorted(set(x for r in roles for x in r[2])):
    if not any(t['tagName']==name and t.get('tagType')=='SKILL' and t.get('status')==1 for t in tags):
        api('/api/admin/tag/save',admin,{'tagName':name,'tagType':'SKILL','baseWeight':3,'status':1})
        tags=api('/api/public/tag/list')
tagids={t['tagName']:t['id'] for t in tags if t.get('tagType')=='SKILL' and t.get('status')==1}
cats=api('/api/public/job/categories')
for name,*_ in roles:
    if not any(c['name']==name for c in cats): api('/api/admin/job/categories',admin,{'name':name}); cats=api('/api/public/job/categories')
catids={c['name']:c['id'] for c in cats}
companies=['澄川数字科技（虚构）','云杉信息技术（虚构）','星港软件（虚构）','青禾智数（虚构）','远山网络科技（虚构）','明途科技（虚构）']
hr_names=['demo_hr','demo_hr2']+['zhdemo_hr%02d'%i for i in range(1,7)]
student_names=['demo_student','demo_student2']+['zhdemo_student%02d'%i for i in range(1,19)]
for i,name in enumerate(hr_names[2:]):
    if name not in state['users']:
        if int(sql("SELECT COUNT(*) FROM sys_user WHERE username='"+name+"';").splitlines()[-1])==0:
            api('/api/public/auth/register',data={'username':name,'password':password,'email':name+'@example.invalid','role':'HR','companyName':companies[i]})
        pending=api('/api/admin/audit/hr/list',admin)
        row=next((x for x in pending if x['username']==name),None)
        if row: api('/api/admin/audit/hr/decision',admin,{'id':row['id'],'pass':True})
        state['users'][name]=True; save()
for name in student_names[2:]:
    if name not in state['users']:
        if int(sql("SELECT COUNT(*) FROM sys_user WHERE username='"+name+"';").splitlines()[-1])==0:
            api('/api/public/auth/register',data={'username':name,'password':password,'email':name+'@example.invalid','role':'USER'})
        state['users'][name]=True; save()
hrtokens={n:login(n) for n in hr_names}
students={n:login(n) for n in student_names}
chinese_names=['林予安','陈思远','周知夏','许一宁','沈嘉禾','唐书言','顾清和','程以南','叶星河','宋知遥','江雨桐','陆明轩','方子衿','苏沐阳','吴嘉宁','孟清扬','乔以安','徐若溪']
year=datetime.date.today().year
profilekeys=['school','majorCode','educationRank','graduationYear','expectedCategoryId','salaryMin','cityCodes']
for i,(name,token) in enumerate(students.items()):
    if state.get('profile_'+name): continue
    role=roles[i%len(roles)]; p=api('/api/student/profile',token) or {}
    defaults={'school':'云川大学（虚构）','majorCode':'计算机科学与技术' if i%6!=5 else '信息管理','educationRank':2,'graduationYear':year,'expectedCategoryId':catids[role[0]],'salaryMin':6000,'cityCodes':['上海','杭州','南京']}
    profile={k:(p.get(k) if p.get(k) not in (None,'',[]) else defaults[k]) for k in profilekeys}
    api('/api/student/profile',token,profile,method='PUT')
    resumes=api('/api/user/resume/list',token)
    if not resumes:
        api('/api/user/resume/save',token,{'resumeName':'校园项目与实习经历','realName':chinese_names[i-2], 'gender':'女' if i%2 else '男','city':['上海','杭州','南京','苏州'][i%4], 'education':'本科','workYears':'应届','expectJobType':role[0],'expectSalary':'7000-12000','workDesc':'参与校园项目与团队实践，负责需求沟通、任务推进和结果复盘；具备持续学习与协作能力。以上为虚构演示经历。','status':1})
        resumes=api('/api/user/resume/list',token)
    resume=next((r for r in resumes if r.get('isDefault')==1),resumes[0]); rid=resume['id']
    detail=api('/api/user/resume/detail?id='+str(rid),token)
    items=[{'tagId':t['tagId'],'proficiency':t.get('proficiency') or 2} for t in detail.get('tags',[])]
    for skill in role[2]:
        if not any(x['tagId']==tagids[skill] for x in items): items.append({'tagId':tagids[skill],'proficiency':2})
    api('/api/user/resume/tags/save',token,{'resumeId':rid,'items':items})
    state['profile_'+name]=rid; save()
print('学生画像与企业资料已准备，开始创建岗位。',flush=True)
created=[]; index=0
for owner in hr_names:
    for j in range(10 if owner=='demo_hr' else 4 if owner=='demo_hr2' else 6):
        key=owner+'-'+str(j); role=roles[j%6]; title=role[1]+('（校招）' if j<6 else '（成长岗位）')
        if key not in state['jobs']:
            jobs=api('/api/hr/job/list',hrtokens[owner]); found=next((x for x in jobs if x['jobName']==title),None)
            if not found:
                city=['上海','杭州','南京','苏州','北京','深圳','成都','武汉'][index%8]; lo=role[3]+(index%3)*500; hi=role[4]+(index%3)*500
                api('/api/hr/job/save',hrtokens[owner],{'jobName':title,'city':city,'cityCode':city,'jobType':role[0],'categoryId':catids[role[0]],'salaryRange':str(lo)+'-'+str(hi),'salaryMin':lo,'salaryMax':hi,'salaryNegotiable':False,'educationMinRank':3 if index%13==12 else (1 if index%4==0 else 2),'graduateYearMin':year-1,'graduateYearMax':year+2,'acceptFreshGraduate':True,'deadline':str(year+2)+'-12-31 18:00','description':'虚构演示岗位。参与团队项目，结合岗位方向完成需求分析、协作交付和持续改进；欢迎认真学习、善于沟通的应届毕业生加入。','status':0 if index>=47 else 1})
                found=next(x for x in api('/api/hr/job/list',hrtokens[owner]) if x['jobName']==title)
            state['jobs'][key]={'id':found['id'],'owner':owner,'index':index}; save()
        rec=state['jobs'][key]; created.append(rec)
        if not rec.get('skills'):
            api('/api/hr/job/tags/save',hrtokens[owner],{'jobId':rec['id'],'items':[{'tagId':tagids[s],'proficiency':2} for s in role[2]]}); rec['skills']=True; save()
        if index<42 or index>=47:
            if not rec.get('approved'):
                pending=api('/api/admin/audit/job/list',admin); row=next((x for x in pending if x['id']==rec['id']),None)
                if row: api('/api/admin/audit/job/decision',admin,{'id':rec['id'],'pass':True,'version':row['version']})
                rec['approved']=True; save()
        index+=1
print('岗位已准备，开始通过业务接口生成投递和行为。',flush=True)
openjobs=[x for x in created if x['index']<42 and x['index']%13!=12]
hrjobs=[x for x in openjobs if x['owner']=='demo_hr']
for i,(name,token) in enumerate(students.items()):
    selected=[hrjobs[i%len(hrjobs)],openjobs[(i*3+12)%len(openjobs)],openjobs[(i*3+23)%len(openjobs)]]
    # Keep each application unique per student.
    seen=set(); selected=[x for x in selected if not (x['id'] in seen or seen.add(x['id']))]
    for k,rec in enumerate(selected):
        key=name+'-'+str(rec['id'])
        if key in state['applications']: continue
        existing=api('/api/user/apply/list',token); row=next((x for x in existing if x['jobId']==rec['id']),None)
        if not row:
            api('/api/user/job/apply',token,{'jobId':rec['id'],'resumeId':state['profile_'+name]})
            row=next(x for x in api('/api/user/apply/list',token) if x['jobId']==rec['id'])
        aid=row['id']; status=row['status']; version=row['version']; target=(i*3+k)%8
        path={0:[],1:[1],2:[2],3:[3],4:[2,4],5:[2,4,5],6:[2,4,6],7:[7]}[target]
        if status==0:
            for s in path:
                api('/api/user/apply/status' if s in (5,6,7) else '/api/hr/job/apply/status', token if s in (5,6,7) else hrtokens[rec['owner']],{'applyId':aid,'status':s,'version':version}); version+=1
        state['applications'][key]=aid; save()
    if not state.get('behavior_'+name):
        for rec in openjobs[4:7]: api('/api/student/jobs/'+str(rec['id'])+'/view',token,{})
        for rec in [openjobs[7+(i%4)],openjobs[11+(i%4)]]: api('/api/student/jobs/'+str(rec['id'])+'/actions/FAVORITE?active=true',token,{})
        api('/api/student/jobs/'+str(openjobs[20+(i%5)]['id'])+'/actions/HIDE?active=true',token,{})
        state['behavior_'+name]=True; save()
print('检查推荐及一次新的真实投递。',flush=True)
token=students['demo_student']; batch=api('/api/student/recommendation-batches',token,{})
assert batch['totalEligible']>=15, ('推荐岗位不足',batch['totalEligible'])
if not state.get('verification_application'):
    ids={x['id'] for x in openjobs}; choice=next(x for x in batch['items'] if x['id'] in ids)
    api('/api/user/job/apply',token,{'jobId':choice['id'],'resumeId':state['profile_demo_student']})
    state['verification_application']=choice['id']; save()
batch=api('/api/student/recommendation-batches',token,{})
assert batch['totalEligible']>=15
state['result']={'before':state['before'],'after':tally(),'seed_jobs':len(created),'seed_students':len(students),'seed_companies':len(hr_names),'seed_applications':len(state['applications']),'verification_job_id':state['verification_application'],'student_eligible':batch['totalEligible']}
state['complete']=True; save()
print('完成：'+json.dumps(state['result'],ensure_ascii=False),flush=True)
