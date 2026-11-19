import { createServer } from 'node:http';

const baseUrl = process.env.MODEL_BASE_URL || 'https://api.groq.com/openai/v1';
const chave = process.env.GROQ_API_KEY;
const prazo = Number(process.env.IOT_AGENT_TIMEOUT_MS || 45000);
const ferramentas = [
  {nome: 'Consultar_ponto', campo: 'situacao', descricao: 'Obter a situação operacional e a leitura do reservatório selecionado.'},
  {nome: 'Consultar_procedimentos', campo: 'fontes', descricao: 'Obter os procedimentos recuperados para a situação, por ordem de relevância.'}
];

async function escolherModelo() {
  const preferido = process.env.MODEL_ID || 'openai/gpt-oss-120b';
  if (!chave || process.env.MODEL_BASE_URL) return preferido;
  try {
    const resposta = await fetch(baseUrl + '/models', {
      headers: {Authorization: 'Bearer ' + chave}, signal: AbortSignal.timeout(8000)
    });
    if (!resposta.ok) throw new Error('HTTP ' + resposta.status);
    const {data} = await resposta.json();
    const ids = new Set(data.map(item => item.id));
    return [preferido, 'openai/gpt-oss-20b'].find(id => ids.has(id)) || preferido;
  } catch (erro) {
    console.warn('Falha ao consultar o catálogo de modelos:', erro.message);
    return preferido;
  }
}
const modelo = await escolherModelo();
console.info('Modelo do agente de abastecimento:', modelo);

async function conversar(mensagens, opcoes) {
  const resposta = await fetch(baseUrl + '/chat/completions', {
    method: 'POST',
    headers: {Authorization: 'Bearer ' + chave, 'Content-Type': 'application/json'},
    body: JSON.stringify({model: modelo, messages: mensagens, temperature: 0, max_tokens: 1600, ...opcoes}),
    signal: AbortSignal.timeout(prazo)
  });
  if (!resposta.ok) throw new Error('Provedor de IA retornou HTTP ' + resposta.status);
  return (await resposta.json()).choices[0].message;
}

async function consultar(estado, ferramenta) {
  const historico = [...estado.mensagens, {role: 'user', content: 'Chame agora a ferramenta ' + ferramenta.nome + '.'}];
  const definicao = {type: 'function', function: {
    name: ferramenta.nome, description: ferramenta.descricao,
    parameters: {type: 'object', properties: {}, additionalProperties: false}
  }};
  const mensagem = await conversar(historico, {tools: [definicao], tool_choice: 'required'});
  const chamadas = mensagem.tool_calls || [];
  if (chamadas.length !== 1 || chamadas[0].function.name !== ferramenta.nome || !chamadas[0].id)
    throw new Error('O modelo não solicitou a consulta ' + ferramenta.nome);
  const chamada = chamadas[0];
  const retorno = {role: 'tool', tool_call_id: chamada.id, content: JSON.stringify(estado[ferramenta.campo])};
  return {...estado, mensagens: [...historico, {role: 'assistant', content: null, tool_calls: [chamada]}, retorno]};
}

async function orientar(estado) {
  const codigo = estado.fontes[0].codigo;
  const instrucao = 'As duas consultas foram realizadas. Redija somente a próxima ação para a equipe em português, conforme o primeiro procedimento, '
    + codigo + '. Não mencione números, volumes, percentuais nem códigos no texto da orientação. Não declare potabilidade ou ajustes realizados. '
    + 'Responda em JSON com exatamente dois campos: texto (orientação) e fonte (código ' + codigo + ').';
  const formato = {type: 'json_schema', json_schema: {name: 'orientacao', strict: true, schema: {
    type: 'object', additionalProperties: false, required: ['texto', 'fonte'],
    properties: {texto: {type: 'string'}, fonte: {type: 'string', enum: [codigo]}}
  }}};
  const mensagem = await conversar([...estado.mensagens, {role: 'user', content: instrucao}], {response_format: formato});
  const resposta = JSON.parse(mensagem.content);
  if (resposta.fonte !== codigo || typeof resposta.texto !== 'string' ||
      !resposta.texto.trim() || resposta.texto.length > 900 || /\d/.test(resposta.texto))
    throw new Error('Orientação incompatível com a fonte');
  return {texto: resposta.texto.trim(), fonte: codigo};
}

// Apenas o workflow interno pode encaminhar as consultas; o proxy público expõe o webhook autenticado.
createServer(async (req, res) => {
  if (req.method !== 'POST' || !['/ponto', '/procedimentos', '/orientacao'].includes(req.url)) {
    res.writeHead(404).end();
    return;
  }
  try {
    req.setEncoding('utf8');
    let dados = '';
    for await (const chunk of req) {
      dados += chunk;
      if (dados.length > 100000) throw new Error('Entrada acima do limite');
    }
    const entrada = JSON.parse(dados);
    let resultado;
    if (req.url === '/ponto') {
      if (!entrada.situacao || !entrada.fontes?.[0]?.codigo) throw new Error('Situação ou fontes ausentes');
      const estado = {situacao: entrada.situacao, fontes: entrada.fontes,
        mensagens: [{role: 'system', content: 'Você auxilia a equipe de abastecimento de um abrigo fictício. Consulte os dados pelas ferramentas indicadas.'}]};
      resultado = await consultar(estado, ferramentas[0]);
    } else if (req.url === '/procedimentos') {
      resultado = await consultar(entrada, ferramentas[1]);
    } else {
      resultado = await orientar(entrada);
    }
    res.writeHead(200, {'Content-Type': 'application/json'}).end(JSON.stringify(resultado));
  } catch (erro) {
    console.error('Etapa do agente', req.url, erro instanceof SyntaxError ? 'JSON inválido' : erro.message);
    res.writeHead(502).end();
  }
}).listen(5680, '127.0.0.1');
