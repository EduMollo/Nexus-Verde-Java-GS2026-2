import { writeFileSync } from 'node:fs';

const token = process.env.IOT_N8N_TOKEN;
if (!token || token.length < 32) throw new Error('Configure IOT_N8N_TOKEN com pelo menos 32 caracteres.');
const credencial = {
  id: 'nvHeader001', name: 'Painel Nexus Verde', type: 'httpHeaderAuth',
  data: {name: 'X-Copiloto-Key', value: token}
};
writeFileSync(process.argv[2], JSON.stringify([credencial]), {mode: 0o600});
