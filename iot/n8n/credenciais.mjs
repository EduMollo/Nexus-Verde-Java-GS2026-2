import { readFileSync, writeFileSync } from 'node:fs';
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

const workflow = JSON.parse(readFileSync(new URL('./abastecimento.json', import.meta.url), 'utf8'));
const modelo = workflow.nodes.find(node => node.name === 'Modelo Groq').parameters.model;
if (groq && !process.env.MODEL_BASE_URL) {
  // O catálogo depende das permissões da organização na Groq.
  try {
    const resposta = await fetch('https://api.groq.com/openai/v1/models', {
      headers: {Authorization: 'Bearer ' + groq},
      signal: AbortSignal.timeout(8000)
    });
    if (!resposta.ok) throw new Error('HTTP ' + resposta.status);
    const {data} = await resposta.json();
    const disponiveis = new Set(data.map(item => item.id));
    const escolhido = [modelo.value, 'openai/gpt-oss-20b'].find(id => disponiveis.has(id));
    if (escolhido) modelo.value = escolhido;
    else console.warn('Nenhum dos modelos de abastecimento consta no catálogo da conta.');
  } catch (erro) {
    console.warn('Catálogo Groq indisponível durante a inicialização:', erro.message);
  }
}
console.info('Modelo do agente de abastecimento:', modelo.value);
writeFileSync(process.argv[3], JSON.stringify(workflow), {mode: 0o600});
