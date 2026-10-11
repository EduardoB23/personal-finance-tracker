// Cliente HTTP do app. Um 401 significa que a API pediu o PIN familiar (entrega 3).

export class PinRequiredError extends Error {}

export type SaveResult =
  | { ok: true }
  | { ok: false; message: string }

async function readBody(response: Response): Promise<{ message?: string; messages?: Record<string, string> }> {
  return (await response.json().catch(() => ({}))) as { message?: string; messages?: Record<string, string> }
}

export async function getJson<T>(url: string): Promise<T> {
  const response = await fetch(url)
  if (response.status === 401) throw new PinRequiredError()
  if (!response.ok) throw new Error('Não foi possível carregar os dados financeiros.')
  return response.json() as Promise<T>
}

// Envia um JSON e devolve a mensagem da API quando ela recusa (400, 429, ...)
export async function postJson(url: string, body: unknown): Promise<SaveResult> {
  const response = await fetch(url, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(body),
  })
  if (response.status === 401) throw new PinRequiredError()
  if (response.ok) return { ok: true }
  const data = await readBody(response)
  const fieldMessages = data.messages ? Object.values(data.messages) : []
  return { ok: false, message: data.message ?? (fieldMessages.join(' ') || 'Não foi possível salvar.') }
}
