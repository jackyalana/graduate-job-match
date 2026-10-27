#!/usr/bin/env python3
"""Loopback-only browser regression proxy: delays Shanghai batch creation, never logs credentials."""
import json,time,urllib.request,urllib.error
from http.server import BaseHTTPRequestHandler,ThreadingHTTPServer
from pathlib import Path
LOG=Path(__file__).resolve().parents[2]/'docs/evidence/M2-review/browser-proxy.log'
class Proxy(BaseHTTPRequestHandler):
 def log_message(self,*args): pass
 def handle_proxy(self):
  body=self.rfile.read(int(self.headers.get('Content-Length','0')))
  city=None
  if self.command=='GET' and self.path.startswith('/api/student/recommendation-batches/') and 'page=2' in self.path:
   with LOG.open('a') as f:f.write('page 2 requested; delaying 15 seconds\n')
   time.sleep(15)
  if self.command=='POST' and self.path=='/api/student/recommendation-batches':
   try:city=json.loads(body).get('city')
   except (ValueError,AttributeError):pass
   with LOG.open('a') as f:f.write('batch requested city='+str(city)+'\n')
   if city=='Shanghai':time.sleep(15)
  headers={k:v for k,v in self.headers.items() if k.lower() not in ('host','connection','content-length','accept-encoding')}
  req=urllib.request.Request('http://127.0.0.1:18081'+self.path,data=body if self.command in ('POST','PUT','PATCH') else None,headers=headers,method=self.command)
  try:res=urllib.request.urlopen(req,timeout=40)
  except urllib.error.HTTPError as e:res=e
  data=res.read();self.send_response(res.status)
  for k,v in res.headers.items():
   if k.lower() not in ('connection','transfer-encoding','content-length','server','date'):self.send_header(k,v)
  self.send_header('Content-Length',str(len(data)));self.end_headers()
  try:self.wfile.write(data)
  except (BrokenPipeError,ConnectionResetError):pass
 do_GET=handle_proxy;do_POST=handle_proxy;do_PUT=handle_proxy;do_OPTIONS=handle_proxy
if __name__=='__main__':
 LOG.write_text('Controlled loopback test: delay Shanghai batch 15 seconds\n')
 ThreadingHTTPServer(('127.0.0.1',18083),Proxy).serve_forever()
