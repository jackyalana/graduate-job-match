#!/usr/bin/env python3
"""Exercise review fixes against the synthetic schema populated by m2/regression.py."""
import json,sys,urllib.parse,urllib.request,urllib.error
from pathlib import Path
ROOT=Path(__file__).resolve().parents[2];sys.path.insert(0,str(ROOT/'scripts/m0'))
from local import LOCAL,credentials,mysql
DB=json.loads((LOCAL/'m1-schemas.json').read_text())['runtime'];assert DB.startswith('graduate_job_match_m1_runtime_')
records=[];tokens={};base='http://127.0.0.1:18081'
def call(name,path,expected=200,role=None,data=None,code=None):
 h={}; raw=None
 if role:h['satoken']=tokens[role]
 if data is not None:h['Content-Type']='application/json';raw=json.dumps(data,ensure_ascii=False).encode()
 req=urllib.request.Request(base+path,data=raw,headers=h)
 try:r=urllib.request.urlopen(req,timeout=30)
 except urllib.error.HTTPError as e:r=e
 body=json.load(r);item={'name':name,'expected':expected,'actual':r.status,'code':body.get('code'),'requestId':bool(r.headers.get('X-Request-Id'))};records.append(item)
 assert r.status==expected and item['requestId'] and (code is None or body.get('code')==code),(item,body)
 return body
try:
 for role in ('student','admin','hr'):
  tokens[role]=call('login '+role,'/api/public/auth/login/password',data={'username':'demo_'+role,'password':credentials()['demoPassword']})['data']
 for value in (None,'','   ', '1','1,2',' 1 , 2 '):
  data={} if value is None else {'tagIds':value}
  call('batch valid '+repr(value),'/api/student/recommendation-batches',role='student',data=data,code='OK')
  query='' if value is None else '?tagIds='+urllib.parse.quote(value,safe='')
  call('legacy valid '+repr(value),'/api/user/job/recommend'+query,role='student',code='OK')
  call('public valid '+repr(value),'/api/public/job/list'+query,code='OK')
 for value in (',',',,','1,,2','1,',' ,1','abc','1,abc','0','-1','9223372036854775808'):
  query='?tagIds='+urllib.parse.quote(value,safe='')
  call('batch invalid '+value,'/api/student/recommendation-batches',422,'student',{'tagIds':value},'VALIDATION_FAILED')
  call('legacy invalid '+value,'/api/user/job/recommend'+query,422,'student',code='VALIDATION_FAILED')
  call('public invalid '+value,'/api/public/job/list'+query,422,code='VALIDATION_FAILED')
 for name in ('Alias Source','Alias Middle'):
  call('create '+name,'/api/admin/tag/save',role='admin',data={'tagName':name,'tagType':'SKILL','baseWeight':3,'status':1},code='OK')
 taglist=call('list tags','/api/admin/tag/list',role='admin',code='OK')['data']
 src=next(t for t in taglist if t['tagName']=='Alias Source'); mid=next(t for t in taglist if t['tagName']=='Alias Middle'); canonical=next(t for t in taglist if t['id']==1)
 mysql('USE `'+DB+'`; INSERT INTO skill_tag_alias(alias_tag_id,canonical_tag_id) VALUES ('+str(src['id'])+','+str(mid['id'])+'),('+str(mid['id'])+',1);')
 for label,t in [('source',src),('chain node',mid),('canonical',canonical)]:
  call('reject delete '+label,'/api/admin/tag/delete',409,'admin',{'id':t['id']},'TAG_IN_USE')
  body={'id':t['id'],'tagName':t['tagName'],'tagType':t['tagType'],'baseWeight':t['baseWeight'],'status':0}
  denied=call('reject disable '+label,'/api/admin/tag/save',409,'admin',body,'TAG_IN_USE');assert '引用' in denied['message']
  body['status']=1;body['tagType']='INDUSTRY'
  call('reject type change '+label,'/api/admin/tag/save',409,'admin',body,'TAG_IN_USE')
 safe={'id':src['id'],'tagName':'Alias Source Renamed','tagType':'SKILL','baseWeight':2,'status':1}
 call('safe rename and weight update','/api/admin/tag/save',role='admin',data=safe,code='OK')
 match=call('match still usable','/api/student/matches/1',role='student',code='OK')['data'];assert match['matchScore'] is not None
 batch=call('recommendations still usable','/api/student/recommendation-batches',role='student',data={},code='OK')['data'];assert batch['totalEligible']>0
 mysql('USE `'+DB+'`; INSERT INTO job_apply(job_id,resume_id,user_id,status,created_at) VALUES (4,1,2,0,UTC_TIMESTAMP());')
 applicants=call('HR matches still usable','/api/hr/job/applications?jobId=4',role='hr',code='OK')['data'];assert any(x.get('matchScore') is not None for x in applicants)
 call('referenced skill cannot be removed','/api/admin/tag/delete',409,'admin',{'id':2},'TAG_IN_USE')
 python_tag=next(t for t in taglist if t['id']==2)
 for change in ({'status':0},{'tagType':'INDUSTRY'}):
  payload={'id':2,'tagName':python_tag['tagName'],'tagType':'SKILL','baseWeight':3,'status':1};payload.update(change)
  call('resume referenced skill cannot be disabled or retyped','/api/admin/tag/save',409,'admin',payload,'TAG_IN_USE')
 call('invalid skill proficiency rejected','/api/hr/job/tags/save',422,'hr',{'jobId':1,'items':[{'tagId':1,'proficiency':4}]},'VALIDATION_FAILED')
 mysql('USE `'+DB+'`; UPDATE tag SET status=0 WHERE id=1;')
 try:
  call('corrupt out-of-band alias fails clearly','/api/student/recommendation-batches',409,'student',{},'TAG_DATA_INVALID')
 finally:
  mysql('USE `'+DB+'`; UPDATE tag SET status=1 WHERE id=1;')
 call('recommendation works after repair','/api/student/recommendation-batches',role='student',data={},code='OK')
 print('PASS',len(records),'real HTTP requests; tagIds, alias chain protection and recommendation/detail/HR verification; schema',DB)
finally:
 (ROOT/'docs/evidence/M2-review/review-http-results.json').write_text(json.dumps({'schema':DB,'total':len(records),'tests':records},ensure_ascii=False,indent=2))
