#!/usr/bin/env python3
"""Run the deterministic synthetic evaluation twice and record reproducibility metadata."""
import csv,hashlib,json,subprocess
from pathlib import Path
ROOT=Path(__file__).resolve().parents[2];out=ROOT/'evaluation/output/synthetic-20261027';out.mkdir(parents=True,exist_ok=True)
cmd=[str(ROOT/'.local/jdk/bin/java'),'-cp',str(ROOT/'backend/target/classes'),'com.lanrecruitment.evaluation.EvaluationMain']
a=subprocess.run(cmd,capture_output=True,text=True,check=True);b=subprocess.run(cmd,capture_output=True,text=True,check=True)
assert a.stdout==b.stdout,'Non-deterministic output';metrics=json.loads(a.stdout)
(out/'metrics.json').write_text(json.dumps(metrics,ensure_ascii=False,indent=2)+'\n')
with (out/'metrics.csv').open('w',newline='') as f:
 w=csv.writer(f);w.writerow(['group','method','users','precision5','precision10','recall5','recall10','ndcg5','ndcg10','hit5','hit10','coverage10'])
 for group in ('metrics','coldMetrics','warmMetrics'):
  for method,values in metrics[group].items():w.writerow([group,method]+[values[k] for k in ('users','precision5','precision10','recall5','recall10','ndcg5','ndcg10','hit5','hit10','coverage10')])
paths=['backend/src/main/java/com/lanrecruitment/evaluation/EvaluationMain.java','backend/src/main/java/com/lanrecruitment/matching/CollaborativeScorer.java','backend/src/main/java/com/lanrecruitment/matching/ContentScorer.java','backend/src/main/java/com/lanrecruitment/matching/RecommendationCalculator.java']
manifest={'sourceCommit':subprocess.check_output(['git','rev-parse','HEAD'],cwd=ROOT,text=True).strip(),'sourceFilesSha256':{p:hashlib.sha256((ROOT/p).read_bytes()).hexdigest() for p in paths},'datasetSha256':metrics['datasetSha256'],'seed':metrics['seed'],'algorithmVersion':'hybrid-v1','command':'python3 scripts/delivery/evaluate.py','repeatedOutputIdentical':True,'split':'70/15/15 time ranges; validation events held out and unused; test positives never used in scoring','note':'All data synthetic; category preferences and positives intentionally correlate; no real-world inference.'}
(out/'manifest.json').write_text(json.dumps(manifest,indent=2)+'\n')
print('PASS deterministic evaluation; source',manifest['sourceCommit'],'dataset',metrics['datasetSha256'])
for k,v in metrics['metrics'].items():print(k,'P@10',v['precision10'],'Recall@10',v['recall10'],'NDCG@10',v['ndcg10'])
