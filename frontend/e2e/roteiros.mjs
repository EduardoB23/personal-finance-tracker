// Roteiros de tela da entrega 4, rodados contra um servidor no ar em E2E_BASE (padrão http://127.0.0.1:8080).
//
// ATENÇÃO: os roteiros criam perfis, contas e lançamentos de teste. Rode só contra um servidor descartável,
// com o banco de teste (ex.: subir o .jar a partir de uma pasta temporária). Não rode contra o banco da família.
//
// Variáveis:
//   E2E_PIN       PIN familiar do servidor descartável (obrigatório)
//   CHROME_PATH   executável do Chromium/headless shell (obrigatório, ex.: /opt/pw-browsers/.../headless_shell)
//   E2E_BASE      URL do servidor (opcional)
import { chromium } from 'playwright-core'

const BASE = process.env.E2E_BASE ?? 'http://127.0.0.1:8080'
const PIN = process.env.E2E_PIN
const EXECUTABLE = process.env.CHROME_PATH

if (!PIN || !EXECUTABLE) {
  console.error('Defina E2E_PIN e CHROME_PATH. Veja o cabeçalho de e2e/roteiros.mjs.')
  process.exit(2)
}

const results = []
async function check(name, fn) {
  try {
    await fn()
    results.push({ name, ok: true })
  } catch (error) {
    results.push({ name, ok: false, reason: String(error.message).split('\n')[0] })
  }
}

function assert(condition, message) {
  if (!condition) throw new Error(message)
}

const monthName = (offset) => {
  const date = new Date()
  date.setDate(1)
  date.setMonth(date.getMonth() + offset)
  const label = new Intl.DateTimeFormat('pt-BR', { month: 'long', year: 'numeric' }).format(date)
  return label.charAt(0).toUpperCase() + label.slice(1)
}
const currentMonthIso = () => {
  const date = new Date()
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}`
}

const browser = await chromium.launch({ executablePath: EXECUTABLE, args: ['--no-sandbox'] })
const context = await browser.newContext({ viewport: { width: 375, height: 800 } })
const page = await context.newPage()

// Login pela tela de PIN, como um celular faria
await page.goto(BASE)
await page.waitForSelector('input[type=password]', { timeout: 15000 })
await page.fill('input[type=password]', PIN)
const loginResponse = page.waitForResponse((response) => response.url().endsWith('/api/auth/pin'))
await page.getByRole('button', { name: 'Entrar' }).click()
assert((await loginResponse).status() === 204, 'login pela tela de PIN falhou')

// C9: com o banco sem perfis, a tela "Quem é você?" oferece o cadastro. Só roda num servidor vazio.
const api = context.request
const existingProfiles = await (await api.get(`${BASE}/api/profiles`)).json()
if (existingProfiles.length === 0) {
  await check('quem-sou-eu: sem perfis mostra cadastro', async () => {
    await page.getByRole('button', { name: 'Cadastrar perfil' }).waitFor({ timeout: 10000 })
  })
}

// Seed mínimo pela API, usando o cookie da sessão recém-criada
async function ensure(path, listPath, field, body) {
  const list = await (await api.get(`${BASE}${listPath}`)).json()
  if (!list.some((item) => item[field] === body[field])) {
    const created = await api.post(`${BASE}${path}`, { data: body })
    assert(created.status() === 201, `seed ${path} falhou: ${created.status()}`)
  }
}
await ensure('/api/profiles', '/api/profiles', 'name', { name: 'Maria' })
await ensure('/api/accounts', '/api/accounts', 'name', { name: 'Conjunta', type: 'CHECKING', balance: 5000 })
await ensure('/api/credit-cards', '/api/credit-cards', 'name', { name: 'Nubank', limitAmount: 3000, availableLimit: 3000, closingDay: 20, dueDay: 10 })
const profiles = await (await api.get(`${BASE}/api/profiles`)).json()
const maria = profiles.find((profile) => profile.name === 'Maria')

// C5 e C6: aparelho sem escolha mostra a lista e grava a escolha
await page.evaluate(() => localStorage.removeItem('quem-sou-eu'))
await page.reload()
await check('quem-sou-eu: sem escolha mostra a lista', async () => {
  await page.getByRole('heading', { name: 'Quem é você?' }).waitFor({ timeout: 10000 })
  await page.getByRole('button', { name: 'Maria' }).waitFor({ timeout: 5000 })
})
await page.getByRole('button', { name: 'Maria' }).click()
await check('quem-sou-eu: escolha vai para o Resumo e fica gravada', async () => {
  await page.getByRole('heading', { name: 'Resumo familiar' }).waitFor({ timeout: 10000 })
  const stored = await page.evaluate(() => localStorage.getItem('quem-sou-eu'))
  assert(stored && JSON.parse(stored).state.profileId === maria.id, 'chave quem-sou-eu sem o id')
})
await check('faturas: vazio mostra mensagem', async () => {
  const section = page.getByRole('region', { name: 'Faturas' })
  await section.getByText('Nenhuma fatura em aberto').waitFor({ timeout: 10000 })
})

// C7: recarregar mantém a escolha
await page.reload()
await check('quem-sou-eu: com escolha salva, recarregar abre direto no Resumo', async () => {
  await page.getByRole('heading', { name: 'Resumo familiar' }).waitFor({ timeout: 10000 })
})

// C8: trocar mostra a lista de novo e a nova escolha substitui a anterior
await check('quem-sou-eu: trocar mostra a lista e a escolha nova substitui a anterior', async () => {
  await page.getByRole('button', { name: 'Trocar' }).click()
  await page.getByRole('heading', { name: 'Quem é você?' }).waitFor({ timeout: 5000 })
  await page.getByRole('button', { name: 'Maria' }).click()
  await page.getByRole('heading', { name: 'Resumo familiar' }).waitFor({ timeout: 5000 })
})

// C10: id salvo que não existe mais faz a tela de escolha reaparecer
await check('quem-sou-eu: id salvo inexistente mostra a tela de escolha', async () => {
  await page.evaluate(() => localStorage.setItem('quem-sou-eu', JSON.stringify({ state: { profileId: 999999 }, version: 0 })))
  await page.reload()
  await page.getByRole('heading', { name: 'Quem é você?' }).waitFor({ timeout: 10000 })
  await page.getByRole('button', { name: 'Maria' }).click()
  await page.getByRole('heading', { name: 'Resumo familiar' }).waitFor({ timeout: 10000 })
})

await check('celular: abas Resumo, Lançar e Extrato com a ativa marcada', async () => {
  const nav = page.locator('nav')
  for (const label of ['Resumo', 'Lançar', 'Extrato']) {
    await nav.getByRole('button', { name: label, exact: true }).waitFor({ timeout: 5000 })
  }
  const active = await nav.locator('[aria-current=page]').innerText()
  assert(active === 'Resumo', `aba ativa era "${active}"`)
})
await check('celular: sem rolagem horizontal em Resumo', async () => {
  const overflow = await page.evaluate(() => document.documentElement.scrollWidth - document.documentElement.clientWidth)
  assert(overflow <= 0, `rolagem horizontal de ${overflow}px`)
})

// C4 do cadastro de cartão e lançamento rápido
await page.getByRole('button', { name: 'Lançar', exact: true }).click()
await check('lancar: despesa vai para a API e limpa o valor', async () => {
  await page.getByRole('heading', { name: 'Lançar' }).waitFor({ timeout: 5000 })
  await page.getByLabel('Valor (R$)').fill('42,50')
  await page.getByLabel('Categoria').selectOption({ label: 'Alimentação' })
  await page.getByLabel('Pagar com').selectOption({ label: 'Conjunta' })
  const responsePromise = page.waitForResponse((response) => response.url().endsWith('/api/transactions') && response.request().method() === 'POST')
  await page.getByRole('button', { name: 'Salvar' }).click()
  const response = await responsePromise
  assert(response.status() === 201, `POST retornou ${response.status()}`)
  await page.getByText('Lançamento salvo', { exact: true }).waitFor({ timeout: 5000 })
  assert((await page.getByLabel('Valor (R$)').inputValue()) === '', 'valor não foi limpo')
})

await check('lancar: mais opções muda os campos', async () => {
  await page.getByRole('button', { name: 'Mais opções' }).click()
  await page.getByLabel('Descrição').waitFor({ timeout: 3000 })
  assert((await page.getByLabel('Parcelas').count()) === 0, 'parcelas visíveis sem cartão')
  await page.getByLabel('Pagar com').selectOption({ label: 'Nubank (cartão)' })
  await page.getByLabel('Parcelas').waitFor({ timeout: 3000 })
  await page.getByLabel('Pagar com').selectOption({ label: 'Conjunta' })
  assert((await page.getByLabel('Parcelas').count()) === 0, 'parcelas continuam com conta')
  await page.getByRole('radio', { name: 'Transferência' }).click()
  await page.getByLabel('Conta de destino').waitFor({ timeout: 3000 })
  await page.getByRole('radio', { name: 'Despesa' }).click()
})

await check('lancar: cartão em 3x mostra a mensagem de parcelas', async () => {
  await page.getByLabel('Valor (R$)').fill('300')
  await page.getByLabel('Categoria').selectOption({ label: 'Alimentação' })
  await page.getByLabel('Pagar com').selectOption({ label: 'Nubank (cartão)' })
  await page.getByLabel('Parcelas').selectOption('3')
  await page.getByRole('button', { name: 'Salvar' }).click()
  await page.getByText('Lançamento salvo em 3 parcelas').waitFor({ timeout: 5000 })
})

await check('lancar: erro da API fica na tela e mantém o valor', async () => {
  await page.getByLabel('Valor (R$)').fill('10')
  await page.getByLabel('Categoria').selectOption({ label: 'Alimentação' })
  await page.getByLabel('Pagar com').selectOption({ label: 'Nubank (cartão)' })
  await page.getByLabel('Parcelas').selectOption('2')
  await page.getByLabel('Repetir todo mês').check()
  await page.getByRole('button', { name: 'Salvar' }).click()
  await page.getByText('Lançamento recorrente não pode ser parcelado').waitFor({ timeout: 5000 })
  assert((await page.getByLabel('Valor (R$)').inputValue()) === '10', 'valor limpo após erro')
  await page.getByLabel('Repetir todo mês').uncheck()
  await page.getByLabel('Parcelas').selectOption('1')
})

await check('lancar: dois toques geram um lançamento', async () => {
  const before = await (await api.get(`${BASE}/api/transactions?month=${currentMonthIso()}`)).json()
  const countBefore = before.filter((item) => item.amount === 7.77).length
  await page.getByLabel('Valor (R$)').fill('7,77')
  await page.getByLabel('Categoria').selectOption({ label: 'Lazer' })
  await page.getByLabel('Pagar com').selectOption({ label: 'Conjunta' })
  const saveButton = page.getByRole('button', { name: 'Salvar' })
  await saveButton.dblclick()
  await page.getByText('Lançamento salvo', { exact: true }).waitFor({ timeout: 5000 })
  await page.waitForTimeout(800)
  const after = await (await api.get(`${BASE}/api/transactions?month=${currentMonthIso()}`)).json()
  const countAfter = after.filter((item) => item.amount === 7.77).length
  assert(countAfter - countBefore === 1, `criou ${countAfter - countBefore} lançamentos com um duplo toque`)
})

// Faturas: pagar, pagamento parcial, estorno (critérios 19 a 28)
const paymentCount = async () => {
  const cards = await (await api.get(`${BASE}/api/credit-cards`)).json()
  let total = 0
  for (const card of cards) {
    const invoices = await (await api.get(`${BASE}/api/credit-cards/${card.id}/invoices`)).json()
    total += invoices.reduce((sum, invoice) => sum + invoice.payments.length, 0)
  }
  return total
}
const accountId = (await (await api.get(`${BASE}/api/accounts`)).json()).find((account) => account.name === 'Conjunta').id

await page.getByRole('button', { name: 'Resumo', exact: true }).click()
const faturas = page.getByRole('region', { name: 'Faturas' })
await check('faturas: lista com restante e status', async () => {
  await faturas.getByText(/Restante R\$/).first().waitFor({ timeout: 10000 })
  assert(/Aberta|Parcial/.test(await faturas.innerText()), 'sem status de fatura')
})

await check('faturas: pagar abre com o restante e pagamento parcial atualiza', async () => {
  await faturas.getByRole('button', { name: 'Pagar', exact: true }).first().click()
  const restante = (await faturas.locator('p').filter({ hasText: 'Restante' }).first().innerText()).match(/R\$\s?([\d.]+,\d{2})/)[1]
  assert((await faturas.getByLabel('Valor (R$)').inputValue()) === restante, 'valor padrão diferente do restante')
  await faturas.getByLabel('Valor (R$)').fill('1,00')
  await faturas.getByLabel('Pagar com a conta').selectOption({ label: 'Conjunta' })
  await faturas.locator('form').getByRole('button', { name: 'Pagar', exact: true }).click()
  await faturas.getByText('Pagamento registrado').waitFor({ timeout: 10000 })
})

await check('faturas: valor acima do restante fica na tela', async () => {
  await faturas.getByRole('button', { name: 'Pagar', exact: true }).first().click()
  await faturas.getByLabel('Valor (R$)').fill('999999999')
  await faturas.getByLabel('Pagar com a conta').selectOption({ label: 'Conjunta' })
  await faturas.locator('form').getByRole('button', { name: 'Pagar', exact: true }).click()
  await faturas.getByText('Valor maior que o saldo da fatura').waitFor({ timeout: 5000 })
  assert((await faturas.getByLabel('Valor (R$)').inputValue()) === '999999999', 'valor digitado foi perdido')
  await faturas.getByRole('button', { name: 'Cancelar' }).click()
})

await check('faturas: dois toques geram um pagamento', async () => {
  const before = await paymentCount()
  await faturas.getByRole('button', { name: 'Pagar', exact: true }).first().click()
  await faturas.getByLabel('Valor (R$)').fill('2,00')
  await faturas.getByLabel('Pagar com a conta').selectOption({ label: 'Conjunta' })
  await faturas.locator('form').getByRole('button', { name: 'Pagar', exact: true }).dblclick()
  await faturas.getByText('Pagamento registrado').waitFor({ timeout: 10000 })
  await page.waitForTimeout(800)
  assert((await paymentCount()) - before === 1, 'dois toques criaram mais de um pagamento')
})

await check('faturas: pagamentos aparecem com estornar', async () => {
  await faturas.getByRole('button', { name: 'Estornar', exact: true }).first().waitFor({ timeout: 5000 })
})

await check('faturas: estornar pede confirmação', async () => {
  const before = await paymentCount()
  await faturas.getByRole('button', { name: 'Estornar', exact: true }).first().click()
  await faturas.getByText(/Estornar pagamento de R\$/).waitFor({ timeout: 5000 })
  assert((await paymentCount()) === before, 'estorno enviado antes da confirmação')
  await faturas.getByRole('button', { name: 'Cancelar' }).click()
})

await check('faturas: estorno confirmado atualiza', async () => {
  const before = await paymentCount()
  await faturas.getByRole('button', { name: 'Estornar', exact: true }).first().click()
  await faturas.getByRole('button', { name: 'Estornar', exact: true }).nth(0).click()
  await faturas.getByText('Pagamento estornado').waitFor({ timeout: 10000 })
  assert((await paymentCount()) - before === -1, 'estorno não reduziu os pagamentos')
})

await check('faturas: sem rolagem horizontal', async () => {
  const overflow = await page.evaluate(() => document.documentElement.scrollWidth - document.documentElement.clientWidth)
  assert(overflow <= 0, `rolagem horizontal de ${overflow}px`)
})

// Extrato
await page.getByRole('button', { name: 'Extrato', exact: true }).click()
await check('extrato: título mostra o mês atual e o anterior muda o título', async () => {
  await page.getByRole('heading', { name: monthName(0) }).waitFor({ timeout: 10000 })
  await page.getByRole('button', { name: 'Mês anterior' }).click()
  await page.getByRole('heading', { name: monthName(-1) }).waitFor({ timeout: 10000 })
  await page.getByRole('button', { name: 'Próximo mês' }).click()
  await page.getByRole('heading', { name: monthName(0) }).waitFor({ timeout: 10000 })
})

await check('extrato: filtro por pessoa restringe a lista', async () => {
  await page.getByRole('group', { name: 'Filtrar por pessoa' }).getByRole('button', { name: 'Maria' }).click()
  const items = await page.locator('main li, section li').allInnerTexts()
  const shown = items.filter((text) => text.includes('·'))
  assert(shown.length > 0, 'nenhum lançamento de Maria no mês')
  assert(shown.every((text) => text.includes('Maria')), 'há lançamento de outra pessoa na lista')
  await page.getByRole('group', { name: 'Filtrar por pessoa' }).getByRole('button', { name: 'Todos' }).click()
})

await check('extrato: mês vazio mostra mensagem e totais zero', async () => {
  for (let step = 0; step < 36; step++) await page.getByRole('button', { name: 'Mês anterior' }).click()
  await page.getByText('Nenhum lançamento neste mês').waitFor({ timeout: 10000 })
  const zeros = await page.locator('dd').allInnerTexts()
  assert(zeros.every((text) => text.replace(/\s/g, '') === 'R$0,00'), `totais: ${zeros.join(' | ')}`)
})

// Cadastro: dia de cartão fora de 1 a 31
await page.getByRole('button', { name: 'Resumo', exact: true }).click()
await page.getByRole('button', { name: 'Cadastros' }).click()
await check('cadastro: dia de fechamento fora de 1 a 31 mostra a mensagem', async () => {
  await page.getByLabel('Nome do cartão').fill('Teste')
  await page.getByLabel('Limite').fill('1000')
  await page.getByLabel('Dia de fechamento').fill('0')
  await page.getByLabel('Dia de vencimento').fill('10')
  await page.getByRole('button', { name: 'Cadastrar cartão' }).click()
  await page.getByText('Dia de fechamento deve ser entre 1 e 31').waitFor({ timeout: 5000 })
})

// Backup (critérios 10 a 15 da entrega 6). A importação substitui os dados, por isso fica por último.
await page.getByRole('button', { name: 'Voltar' }).click()
await page.getByRole('button', { name: 'Resumo', exact: true }).click()
await check('backup: nenhum aviso de falha no Resumo', async () => {
  await page.getByRole('heading', { name: 'Resumo familiar' }).waitFor({ timeout: 10000 })
  assert((await page.getByText('Backup automático falhou').count()) === 0, 'aviso de falha aparece sem falha')
})
await check('orcamentos: criar mostra o orçamento e repete o erro de duplicado', async () => {
  await page.getByRole('button', { name: 'Orçamentos' }).click()
  await page.getByRole('heading', { name: 'Orçamentos' }).waitFor({ timeout: 5000 })
  await page.getByLabel('Categoria do orçamento').selectOption({ label: 'Alimentação' })
  await page.getByLabel('Limite do orçamento').fill('500')
  await page.getByRole('button', { name: 'Criar orçamento' }).click()
  await page.getByText('Orçamento criado').waitFor({ timeout: 10000 })
  await page.getByLabel('Categoria do orçamento').selectOption({ label: 'Alimentação' })
  await page.getByLabel('Limite do orçamento').fill('600')
  await page.getByRole('button', { name: 'Criar orçamento' }).click()
  await page.getByText('Já existe orçamento para esta categoria neste mês').waitFor({ timeout: 5000 })
  const overflow = await page.evaluate(() => document.documentElement.scrollWidth - document.documentElement.clientWidth)
  assert(overflow <= 0, `rolagem horizontal de ${overflow}px em Orçamentos`)
})

await check('metas: criar meta e aportar mostra o progresso', async () => {
  await page.getByRole('button', { name: 'Voltar' }).click()
  await page.getByRole('button', { name: 'Metas' }).click()
  await page.getByRole('heading', { name: 'Metas' }).waitFor({ timeout: 5000 })
  await page.getByLabel('Título da meta').fill('Viagem')
  await page.getByLabel('Valor alvo').fill('1000')
  await page.getByLabel('Perfil da meta').selectOption({ label: 'Maria' })
  await page.getByRole('button', { name: 'Criar meta' }).click()
  await page.getByText('Meta criada').waitFor({ timeout: 10000 })
  await page.getByLabel('Aporte para Viagem').fill('250')
  await page.getByRole('button', { name: 'Aportar' }).click()
  await page.getByText('Aporte registrado').waitFor({ timeout: 10000 })
  await page.getByText('25% da meta').waitFor({ timeout: 5000 })
  const overflow = await page.evaluate(() => document.documentElement.scrollWidth - document.documentElement.clientWidth)
  assert(overflow <= 0, `rolagem horizontal de ${overflow}px em Metas`)
})

await page.getByRole('button', { name: 'Voltar' }).click()
await page.getByRole('button', { name: 'Resumo', exact: true }).click()
await page.getByRole('button', { name: 'Configurações' }).click()
await page.getByRole('heading', { name: 'Configurações' }).waitFor({ timeout: 5000 })

await check('backup: exportar JSON baixa finance-backup.json', async () => {
  const downloadPromise = page.waitForEvent('download')
  await page.getByRole('button', { name: 'Exportar JSON' }).click()
  const download = await downloadPromise
  assert(download.suggestedFilename() === 'finance-backup.json', `arquivo ${download.suggestedFilename()}`)
})

await check('backup: exportar Excel baixa finance-backup.xlsx', async () => {
  const downloadPromise = page.waitForEvent('download')
  await page.getByRole('button', { name: 'Exportar Excel (leitura)' }).click()
  const download = await downloadPromise
  assert(download.suggestedFilename() === 'finance-backup.xlsx', `arquivo ${download.suggestedFilename()}`)
})

await check('backup: exportar CSV baixa finance-backup.csv', async () => {
  const downloadPromise = page.waitForEvent('download')
  await page.getByRole('button', { name: 'Exportar CSV' }).click()
  const download = await downloadPromise
  assert(download.suggestedFilename() === 'finance-backup.csv', `arquivo ${download.suggestedFilename()}`)
})

await check('backup: sem rolagem horizontal em Configurações', async () => {
  const overflow = await page.evaluate(() => document.documentElement.scrollWidth - document.documentElement.clientWidth)
  assert(overflow <= 0, `rolagem horizontal de ${overflow}px`)
})

const backupFile = await (await api.get(`${BASE}/api/backup/export?format=json`)).text()
const backupPath = (await import('node:os')).tmpdir() + '/e2e-finance-backup.json'
;(await import('node:fs')).writeFileSync(backupPath, backupFile)

await check('backup: importar pede confirmação antes de enviar', async () => {
  await page.getByLabel('Arquivo de backup').setInputFiles(backupPath)
  await page.getByText('Importar substitui todos os dados atuais. Continuar?').waitFor({ timeout: 5000 })
  await page.getByRole('button', { name: 'Cancelar' }).click()
  // Limpa o campo, para que escolher o mesmo arquivo de novo dispare a troca
  await page.getByLabel('Arquivo de backup').setInputFiles([])
})

await check('backup: confirmar importação mostra sucesso', async () => {
  await page.getByLabel('Arquivo de backup').setInputFiles(backupPath)
  await page.getByRole('button', { name: 'Importar', exact: true }).click()
  await page.getByText('Backup importado com sucesso').waitFor({ timeout: 15000 })
})

await browser.close()

const failed = results.filter((result) => !result.ok)
for (const result of results) {
  console.log(`${result.ok ? 'PASS' : 'FAIL'}  ${result.name}${result.ok ? '' : `  -- ${result.reason}`}`)
}
console.log(`\n${results.length - failed.length}/${results.length} roteiros passaram`)
process.exit(failed.length === 0 ? 0 : 1)
