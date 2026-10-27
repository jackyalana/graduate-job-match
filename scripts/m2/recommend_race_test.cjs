const fs = require('node:fs')
const vm = require('node:vm')
const assert = require('node:assert/strict')
const source = fs.readFileSync('frontend/src/pages/Home.vue', 'utf8')
function extract(name) {
  const start = source.indexOf(`async function ${name}(`)
  assert(start >= 0, name)
  const end = source.indexOf('\n}', start)
  assert(end >= 0, name)
  return source.slice(start, end + 2)
}
function harness() {
  const calls=[]
  const ref=value=>({value})
  const state={ role:ref('USER'),recommendLoading:ref(false),recommendLoadingMore:ref(false),
    recommendJobs:ref([]),recommendOffset:ref(0),batchId:ref(null),batchStale:ref(false),
    recommendHasMore:ref(true),prefetchCount:ref(0),filters:{city:'Shanghai'},allTagIds:ref([]),pageSize:10,
    AbortController,request:{post:(url,params,config)=>new Promise((resolve,reject)=>calls.push({url,params,config,resolve,reject})),
      get:(url,config)=>new Promise((resolve,reject)=>calls.push({url,config,resolve,reject}))}}
  vm.createContext(state)
  vm.runInContext(`let recommendGeneration=0; let activeRecommendAbort=null;\n${extract('loadMoreRecommendJobs')}\n${extract('resetAndFetchRecommendJobs')}`,state)
  return {state,calls}
}
const tick=()=>new Promise(resolve=>setImmediate(resolve))
async function run() {
  {
    const {state:s,calls:c}=harness()
    const shanghai=s.resetAndFetchRecommendJobs(); assert.equal(c[0].params.city,'Shanghai')
    s.filters.city='Beijing'; const beijing=s.resetAndFetchRecommendJobs(); assert.equal(c.length,2);assert.equal(c[1].params.city,'Beijing')
    c[0].resolve({batchId:'old-shanghai',items:[{id:1}],totalRanked:20});await shanghai
    assert.equal(s.batchId.value,null);assert.equal(s.recommendJobs.value.length,0);assert.equal(s.recommendLoadingMore.value,true);assert.equal(s.recommendLoading.value,true)
    c[1].resolve({batchId:'new-beijing',items:[{id:2}],totalRanked:11});await beijing
    assert.equal(s.batchId.value,'new-beijing');assert.equal(s.recommendJobs.value[0].id,2);assert.equal(s.recommendOffset.value,1)
    console.log('PASS filter change starts Beijing while Shanghai is pending; old success/finally ignored')
  }
  {
    const {state:s,calls:c}=harness()
    const initial=s.resetAndFetchRecommendJobs();c[0].resolve({batchId:'A',items:Array.from({length:10},(_,i)=>({id:i+1})),totalRanked:20});await initial
    const oldPage=s.loadMoreRecommendJobs();assert.equal(c[1].config.params.page,2)
    s.filters.city='Beijing';const refresh=s.resetAndFetchRecommendJobs();c[2].resolve({batchId:'B',items:[{id:99}],totalRanked:1});await refresh
    c[1].resolve({batchId:'A',items:[{id:11}],totalRanked:20});await oldPage
    assert.equal(s.batchId.value,'B');assert.equal(s.recommendOffset.value,1);assert.equal(s.recommendJobs.value.map(x=>x.id).join(','),'99');assert.equal(s.recommendHasMore.value,false)
    console.log('PASS old page returns after refresh; list, batch ID and cursor remain from new batch')
  }
  {
    const {state:s,calls:c}=harness()
    const old=s.resetAndFetchRecommendJobs();s.filters.city='Beijing';const fresh=s.resetAndFetchRecommendJobs()
    c[0].reject({response:{data:{code:'BATCH_STALE'}}});await old
    assert.equal(s.batchStale.value,false);assert.equal(s.recommendLoadingMore.value,true);assert.equal(s.recommendLoading.value,true)
    c[1].resolve({batchId:'B',items:[{id:5}],totalRanked:1});await fresh
    assert.equal(s.batchId.value,'B');assert.equal(s.recommendOffset.value,1);assert.equal(s.recommendJobs.value[0].id,5)
    console.log('PASS obsolete error/finally cannot overwrite new request state')
  }
  await tick()
}
run().catch(e=>{console.error(e);process.exitCode=1})
