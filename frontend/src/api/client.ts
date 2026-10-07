import { API_BASE_URL } from '../config/runtime'

export async function request<T>(path: string, options?: RequestInit): Promise<T> {
  const response = await fetch(`${API_BASE_URL}/api${path}`, { ...options, headers: { 'Content-Type': 'application/json', ...options?.headers } })
  let body: unknown
  try { body = await response.json() } catch { throw new Error('API 回應格式錯誤，請確認服務已啟動。') }
  if (!response.ok) throw new Error(typeof body === 'object' && body !== null && 'message' in body ? String(body.message) : `請求失敗 (${response.status})`)
  return body as T
}

