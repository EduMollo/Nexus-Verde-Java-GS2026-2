// Dublê do protocolo de tool calling usado apenas no ensaio de integração.
import { createServer } from 'node:http';
const resposta = {texto:'Confira o inventário com a equipe antes de ajustar o cadastro.',fonte:'EST-04'};
createServer(async(req,res)=>{
  if(req.url!=='/v1/chat/completions') {res.writeHead(404).end();return;}
  let body='';
  for await (const chunk of req) body+=chunk;
  const {messages,tools,model}=JSON.parse(body);
  console.log(JSON.stringify({ferramentas:tools?.map(t=>({nome:t.function.name,schema:t.function.parameters})),resultados:messages.filter(m=>m.role==='tool').map(m=>m.content)}));
  const retornos=messages.filter(m=>m.role==='tool');
  let message={role:'assistant',content:JSON.stringify(resposta)};
  if(retornos.length<2) {
    const nome=retornos.length===0?'Consultar_ponto':'Consultar_procedimentos';
    const ferramenta=tools?.find(t=>t.function.name===nome);
    if(!ferramenta) {res.writeHead(422).end();return;}
    message={role:'assistant',content:null,tool_calls:[{type:'function',id:'call_'+nome,
      function:{name:nome,arguments:JSON.stringify(Object.fromEntries(Object.keys(ferramenta.function.parameters.properties ?? {}).map(k=>[k,'Consultar o registro'])))}}]};
  } else if (!retornos.some(m=>JSON.stringify(m.content).includes('EST-04'))
          || !retornos.some(m=>JSON.stringify(m.content).includes('8000'))) {
    res.writeHead(422).end();return;
  }
  res.writeHead(200,{'content-type':'application/json'}).end(JSON.stringify({id:'chatcmpl-teste',
    object:'chat.completion',created:Math.floor(Date.now()/1000),model,
    choices:[{index:0,message,finish_reason:message.tool_calls?'tool_calls':'stop'}],usage:{prompt_tokens:1,completion_tokens:1,total_tokens:2}}));
}).listen(9000,'0.0.0.0');
