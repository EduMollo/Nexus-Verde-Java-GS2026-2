import './agente.mjs';
import { createServer, request } from 'node:http';
import { timingSafeEqual, createHash } from 'node:crypto';
const hash = texto => createHash('sha256').update(texto).digest();
const esperado=hash(process.env.IOT_N8N_TOKEN);
createServer((req,res)=>{
  const saude=req.url==='/healthz' && req.method==='GET';
  if (!saude && (req.method!=='POST' || req.url!=='/webhook/abastecimento')) {
    res.writeHead(404).end(); return;
  }
  const token=req.headers['x-copiloto-key'];
  if (!saude && (typeof token!=='string' || !timingSafeEqual(hash(token),esperado))) {
    res.writeHead(401).end(); return;
  }
  if (!saude && !process.env.GROQ_API_KEY) { res.writeHead(503).end(); return; }
  const upstream=request({hostname:'127.0.0.1',port:5678,path:saude?'/healthz/readiness':req.url,
    method:req.method,headers:saude?{}:{'content-type':'application/json','x-copiloto-key':token}},r=>{
      res.writeHead(r.statusCode,{'content-type':r.headers['content-type']||'text/plain'});r.pipe(res);
    });
  upstream.setTimeout(Number(process.env.IOT_AGENT_TIMEOUT_MS || 55000),()=>upstream.destroy());
  upstream.on('error',()=>{if(!res.headersSent)res.writeHead(503);res.end();});
  req.pipe(upstream);
}).listen(Number(process.env.PORT || 10000),'0.0.0.0');
