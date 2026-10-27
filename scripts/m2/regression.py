#!/usr/bin/env python3
"""Exercise M2 using only the uniquely named local synthetic runtime database."""
import concurrent.futures, json, math, sys, urllib.error, urllib.request
from pathlib import Path
ROOT=Path(__file__).resolve().parents[2]
sys.path.insert(0,str(ROOT/'scripts/m0'))
from local import LOCAL,credentials,mysql
DB=json.loads((LOCAL/'m1-schemas.json').read_text())['runtime']
assert DB.startswith('graduate_job_match_m1_runtime_')
base='http://127.0.0.1:18081'; records=[]; tokens={}
def call(name,path,expected=200,role=None,data=None,method=None):
    headers={}; raw=None
    if role: headers['satoken']=tokens[role]
    if data is not None:headers['Content-Type']='application/json';raw=json.dumps(data,ensure_ascii=False).encode()
    req=urllib.request.Request(base+path,data=raw,headers=headers,method=method)
    try:r=urllib.request.urlopen(req,timeout=30)
    except urllib.error.HTTPError as e:r=e
    body=json.load(r);record={'name':name,'expected':expected,'actual':r.status,'code':body.get('code'),'requestId':bool(r.headers.get('X-Request-Id'))};records.append(record)
    assert r.status==expected and record['requestId'],(record,body)
    return body
for role in ('student','student2','admin','hr'):
    tokens[role]=call('login '+role,'/api/public/auth/login/password',data={'username':'demo_'+role,'password':credentials()['demoPassword']})['data']
assert mysql('USE `'+DB+'`; SELECT COUNT(*) FROM job;').strip().splitlines()[-1]=='0', 'Use a fresh synthetic schema'
rows=[]
for i in range(205):
    if i==0: rows.append("(4,'M2 oldest best','SyntheticCo','Shanghai','8000-12000','Dev','Synthetic',1,1,DATE_SUB(UTC_TIMESTAMP(),INTERVAL 10 DAY),1,'Shanghai',8000,12000,0,1)")
    else: rows.append("(4,'M2 synthetic %d','SyntheticCo','Beijing','3000-4000','Dev','Synthetic',1,1,UTC_TIMESTAMP(),2,'Beijing',3000,4000,0,1)"%i)
sql="""INSERT INTO job_category(id,name) VALUES (1,'Synthetic Category A'),(2,'Synthetic Category B');
INSERT INTO job(hr_id,job_name,company_name,city,salary_range,job_type,description,status,audit_status,created_at,category_id,city_code,salary_min,salary_max,salary_negotiable,accept_fresh_graduate) VALUES """+','.join(rows)+";"
mysql('USE `'+DB+'`; '+sql)
try:
    cold=call('cold batch','/api/student/recommendation-batches',role='student2',data={})['data'];assert cold['totalEligible']==205 and cold['totalRanked']==200
    assert all(item['match']['matchScore'] is None and item['match']['strategy']=='HOT_FALLBACK' for item in cold['items'])
    cold_id=cold['batchId']; page1=call('cold stable page','/api/student/recommendation-batches/'+cold_id+'?page=1',role='student2')['data'];assert page1['items']==cold['items']
    page2=call('cold page two','/api/student/recommendation-batches/'+cold_id+'?page=2',role='student2')['data'];assert not set(x['id'] for x in page1['items']) & set(x['id'] for x in page2['items'])
    assert call('cross user batch denied','/api/student/recommendation-batches/'+cold_id,404,'student')['code']=='BATCH_NOT_FOUND'
    assert call('invalid page size','/api/student/recommendation-batches/'+cold_id+'?pageSize=51',422,'student2')['code']=='VALIDATION_FAILED'
    profile={'school':'Synthetic','majorCode':'CS','educationRank':2,'graduationYear':2026,'expectedCategoryId':1,'salaryMin':8000,'cityCodes':['Shanghai']}
    call('set profile','/api/student/profile',role='student',data=profile,method='PUT')
    warm=call('rank all 205','/api/student/recommendation-batches',role='student',data={})['data'];assert warm['totalEligible']==205 and warm['totalRanked']==200
    assert warm['items'][0]['id']==1 and warm['items'][0]['match']['matchScore']==100.0, warm['items'][0]
    warm_id=warm['batchId']; detail=call('matching detail','/api/student/matches/1',role='student')['data'];assert detail['matchScore']==100.0 and detail['strategy']=='CONTENT_HOT'
    old=call('legacy recommendation','/api/user/job/recommend?pageSize=10',role='student')['data'];assert old[0]['id']==1 and float(old[0]['matchScore'])==100.0
    p1=call('warm page one','/api/student/recommendation-batches/'+warm_id+'?page=1',role='student')['data']; p2=call('warm page two','/api/student/recommendation-batches/'+warm_id+'?page=2',role='student')['data']; assert p1['items']==warm['items'] and len(p2['items'])==10
    profile['salaryMin']=9000; call('change profile','/api/student/profile',role='student',data=profile,method='PUT')
    assert call('profile change invalidates batch','/api/student/recommendation-batches/'+warm_id,409,'student')['code']=='BATCH_STALE'
    warm=call('new batch','/api/student/recommendation-batches',role='student',data={})['data'];
    mysql('USE `'+DB+'`; UPDATE job SET status=0,version=version+1 WHERE id=1;')
    assert call('job closed invalidates batch','/api/student/recommendation-batches/'+warm['batchId'],409,'student')['code']=='BATCH_STALE'
    call('closed match hidden','/api/student/matches/1',404,'student')
    mysql('USE `'+DB+'`; UPDATE job SET status=1,version=version+1 WHERE id=1;')
    # Server computes in parallel without inserting old unversioned match_score rows.
    def parallel(i): return call('parallel batch '+str(i),'/api/student/recommendation-batches',role='student',data={})['code']
    with concurrent.futures.ThreadPoolExecutor(max_workers=8) as pool: assert list(pool.map(parallel,range(8)))==['OK']*8
    assert mysql('USE `'+DB+'`; SELECT COUNT(*) FROM match_score;').strip().splitlines()[-1]=='0'
    # Required and student skill levels are separate from upstream derived weight (3 * proficiency).
    mysql('USE `'+DB+"`; INSERT INTO tag(tag_name,tag_type,base_weight,status) VALUES ('Python','SKILL',3,1); INSERT INTO resume_tag(resume_id,tag_id,proficiency,weight) VALUES (1,2,2,6); INSERT INTO job_tag(job_id,tag_id,proficiency,weight) VALUES (1,1,3,9);")
    mismatch=call('filled skills without intersection','/api/student/matches/1',role='student')['data']
    assert mismatch['dimensions']['skill']==0 and mismatch['missingSkills'][0]['actual']==0 and mismatch['missingSkills'][0]['required']==3
    alias_batch=call('batch before alias change','/api/student/recommendation-batches',role='student',data={})['data']
    mysql('USE `'+DB+'`; INSERT INTO skill_tag_alias(alias_tag_id,canonical_tag_id) VALUES (2,1);')
    assert call('alias change invalidates batch','/api/student/recommendation-batches/'+alias_batch['batchId'],409,'student')['code']=='BATCH_STALE'
    alias_match=call('canonical alias matches requirement','/api/student/matches/1',role='student')['data']
    assert alias_match['missingSkills'][0]['actual']==2 and alias_match['dimensions']['skill']>0
    mysql('USE `'+DB+'`; DELETE FROM skill_tag_alias WHERE alias_tag_id=2;')
    mysql('USE `'+DB+'`; INSERT INTO resume_tag(resume_id,tag_id,proficiency,weight) VALUES (1,1,2,6);')
    partial=call('skill partial proficiency','/api/student/matches/1',role='student')['data']
    assert partial['dimensions']['skill']>0 and partial['missingSkills'][0]['actual']==2 and partial['missingSkills'][0]['required']==3
    mysql('USE `'+DB+'`; UPDATE resume_tag SET proficiency=3,weight=9 WHERE resume_id=1 AND tag_id=1;')
    complete=call('skill satisfied','/api/student/matches/1',role='student')['data'];assert 'Java' in complete['matchedSkills']
    # Qualification and recent applicant popularity use real deduplicated application rows.
    mysql('USE `'+DB+"`; INSERT INTO resume(user_id,resume_name,real_name,status,is_default) VALUES (3,'Synthetic second resume','Synthetic',1,0); INSERT INTO job_apply(job_id,resume_id,user_id,status,created_at) VALUES (1,3,3,0,UTC_TIMESTAMP());")
    unique=call('batch after first application','/api/student/recommendation-batches',role='student',data={})['data']
    mysql('USE `'+DB+'`; INSERT INTO job_apply(job_id,resume_id,user_id,status,created_at) VALUES (1,4,3,0,UTC_TIMESTAMP());')
    call('duplicate event does not invalidate batch','/api/student/recommendation-batches/'+unique['batchId'],role='student')
    mysql('USE `'+DB+"`; UPDATE job SET education_min_rank=3,version=version+1 WHERE id=2; UPDATE job SET deadline=DATE_SUB(UTC_TIMESTAMP(),INTERVAL 1 DAY),version=version+1 WHERE id=3; INSERT INTO sys_user(username,password,email,role,status,audit_status) VALUES ('synthetic_extra1','test-only','extra1@example.invalid','USER',1,1),('synthetic_extra2','test-only','extra2@example.invalid','USER',1,1); INSERT INTO resume(user_id,resume_name,real_name,status,is_default) VALUES (6,'Synthetic','Synthetic',1,1),(7,'Synthetic','Synthetic',1,1); INSERT INTO job_apply(job_id,resume_id,user_id,status,created_at) VALUES (4,5,6,0,UTC_TIMESTAMP()),(4,6,7,0,UTC_TIMESTAMP()),(5,5,6,0,DATE_SUB(UTC_TIMESTAMP(),INTERVAL 31 DAY));")
    filtered=call('qualification expired and applied filtered','/api/student/recommendation-batches',role='student',data={})['data']
    assert filtered['totalEligible']==203
    first=next(x for x in filtered['items'] if x['id']==1)['match']
    expected_hot=0.8*math.log1p(5)/math.log1p(10)+0.2*math.exp(-10/30)
    assert abs(first['components']['hot']-expected_hot)<0.001,(first['components']['hot'],expected_hot)
    excluded=call('unqualified detail reason','/api/student/matches/2',role='student')['data'];assert excluded['eligible'] is False and 'EDUCATION_REQUIREMENT_NOT_MET' in excluded['reasons']
    assert call('expired detail hidden','/api/student/matches/3',404,'student')['code']=='JOB_NOT_FOUND'
    applied=call('applied excluded for student2','/api/student/recommendation-batches',role='student2',data={})['data'];assert applied['totalEligible']==202
    assert all(x['id']!=1 for x in applied['items'])
    def parallel_hr(i): return call('parallel applicant list '+str(i),'/api/hr/job/applications?jobId=1',role='hr')['code']
    with concurrent.futures.ThreadPoolExecutor(max_workers=8) as pool: assert list(pool.map(parallel_hr,range(8)))==['OK']*8
    assert mysql('USE `'+DB+'`; SELECT COUNT(*) FROM match_score;').strip().splitlines()[-1]=='0'
    mysql('USE `'+DB+'`; UPDATE recommendation_batch SET expires_at=DATE_SUB(UTC_TIMESTAMP(),INTERVAL 1 SECOND) WHERE id=\''+filtered['batchId']+"';")
    assert call('expired batch','/api/student/recommendation-batches/'+filtered['batchId'],409,'student')['code']=='BATCH_STALE'
    print('PASS',len(records),'real HTTP requests; 205 eligible jobs; concurrent batches=8; schema',DB)
finally:
    (ROOT/'docs/evidence/M2/http-results.json').write_text(json.dumps({'schema':DB,'tests':records,'total':len(records)},ensure_ascii=False,indent=2))
