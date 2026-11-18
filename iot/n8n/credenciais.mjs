import { writeFileSync } from 'node:fs';
import { randomBytes } from 'node:crypto';

const chave = process.env.IOT_N8N_TOKEN;
if (!chave || chave.length < 32) throw new Error('Configure IOT_N8N_TOKEN com pelo menos 32 caracteres.');
const groq = process.env.GROQ_API_KEY;
if (!groq) console.warn('GROQ_API_KEY pendente. A análise permanece indisponível.');
const credenciais = [
  {id:'nvHeader001',name:'Painel Nexus Verde',type:'httpHeaderAuth',data:{name:'X-Copiloto-Key',value:chave}},
  {id:'nvGroq001',name:'Groq compatível com OpenAI',type:'openAiApi',data:{
    apiKey:groq || randomBytes(32).toString('hex'),url:process.env.MODEL_BASE_URL || 'https://api.groq.com/openai/v1'}}
];
writeFileSync(process.argv[2],JSON.stringify(credenciais),{mode:0o600});
