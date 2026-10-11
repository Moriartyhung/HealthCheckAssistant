// @vitest-environment jsdom
import { afterEach, beforeEach, expect, it, vi } from 'vitest'
import { cleanup, render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { PatientManagement } from './PatientManagement'

let patients: {patientId: string; patientName: string}[]
let calls: {url: string; method: string; body?: unknown}[]
beforeEach(() => {
  patients = [{patientId: 'P900', patientName: '原姓名'}]; calls = []
  vi.stubGlobal('fetch', vi.fn(async (url: string, options?: RequestInit) => {
    const method = options?.method ?? 'GET'
    const body = options?.body ? JSON.parse(String(options.body)) : undefined
    calls.push({url, method, body})
    if (url.endsWith('/admin/exams')) return new Response('[]')
    if (method === 'POST') { patients.push(body); return new Response(JSON.stringify(body), {status: 201}) }
    if (method === 'PUT') { patients[0] = {...patients[0], patientName: body.patientName}; return new Response(JSON.stringify(patients[0])) }
    if (method === 'DELETE') { patients = []; return new Response('{"message":"受檢者已刪除"}') }
    return new Response(JSON.stringify(url.endsWith('/patients') ? patients : {...patients[0], exams: []}))
  }))
})
afterEach(() => { cleanup(); vi.unstubAllGlobals() })
function mount(mockMode = false) {
  const client = new QueryClient({defaultOptions: {queries: {retry: false}, mutations: {retry: false}}})
  render(<QueryClientProvider client={client}><PatientManagement mockMode={mockMode}/></QueryClientProvider>)
}
it('lists, queries one, edits an immutable ID and deletes only after confirmation', async () => {
  const user = userEvent.setup(); mount()
  await screen.findByText('原姓名')
  await user.type(screen.getByLabelText('查詢病歷號'), 'P900')
  await user.click(screen.getByRole('button', {name: '查詢'}))
  await waitFor(() => expect(calls.some(c => c.url === '/api/admin/patients/P900' && c.method === 'GET')).toBe(true))
  await user.click(screen.getByRole('button', {name: '編輯 P900'}))
  await waitFor(() => expect((screen.getByLabelText('病歷號') as HTMLInputElement).disabled).toBe(true))
  await user.clear(screen.getByLabelText('姓名')); await user.type(screen.getByLabelText('姓名'), '新姓名')
  await user.click(screen.getByRole('button', {name: '儲存姓名與安排'}))
  await screen.findByText('新姓名')
  expect(calls.find(c => c.method === 'PUT')?.body).toEqual({patientName: '新姓名', examIds: []})
  await user.click(screen.getByRole('button', {name: '刪除 P900'}))
  expect(calls.some(c => c.method === 'DELETE')).toBe(false)
  await user.click(screen.getByRole('button', {name: '確認刪除'}))
  await screen.findByText('受檢者已刪除')
})
it('creates using the real API adapter and refreshes the list', async () => {
  const user = userEvent.setup(); mount(); await screen.findByText('原姓名')
  await user.type(screen.getByLabelText('病歷號'), 'P901'); await user.type(screen.getByLabelText('姓名'), '新增姓名')
  await user.click(screen.getByRole('button', {name: '新增受檢者'}))
  await screen.findByText('新增姓名')
  expect(calls.find(c => c.method === 'POST')).toEqual({url: '/api/admin/patients', method: 'POST', body: {patientId: 'P901', patientName: '新增姓名', examIds: []}})
})
it('shows backend deletion conflict without removing the patient', async () => {
  const user = userEvent.setup(); mount(); await screen.findByText('原姓名')
  const original = globalThis.fetch
  vi.stubGlobal('fetch', vi.fn((url: string, options?: RequestInit) => options?.method === 'DELETE'
    ? Promise.resolve(new Response('{"message":"受檢者已有關聯檢查紀錄或確認碼，無法刪除"}', {status: 409})) : original(url, options)))
  await user.click(screen.getByRole('button', {name: '刪除 P900'})); await user.click(screen.getByRole('button', {name: '確認刪除'}))
  await screen.findByText('受檢者已有關聯檢查紀錄或確認碼，無法刪除')
  expect(screen.getByText('原姓名')).toBeTruthy()
})
it('does not query or mutate in demo mode', () => {
  mount(true)
  expect(screen.getByText(/模擬模式下停用/)).toBeTruthy()
  expect(calls).toHaveLength(0)
  expect(screen.queryByRole('button', {name: '新增受檢者'})).toBeNull()
})
it('surfaces query failure rather than displaying invented patient data', async () => {
  vi.stubGlobal('fetch', vi.fn(async () => new Response('{"message":"服務暫時無法使用"}', {status: 500})))
  mount()
  await screen.findAllByText('服務暫時無法使用')
  expect(screen.queryByText('原姓名')).toBeNull()
})
it('keeps creation form and shows duplicate error', async () => {
  const user = userEvent.setup(); mount(); await screen.findByText('原姓名')
  const original = globalThis.fetch
  vi.stubGlobal('fetch', vi.fn((url: string, options?: RequestInit) => options?.method === 'POST'
    ? Promise.resolve(new Response('{"message":"病歷號已存在"}', {status: 409})) : original(url, options)))
  await user.type(screen.getByLabelText('病歷號'), 'P900'); await user.type(screen.getByLabelText('姓名'), '重複姓名')
  await user.click(screen.getByRole('button', {name: '新增受檢者'}))
  await screen.findByText('病歷號已存在')
  expect((screen.getByLabelText('病歷號') as HTMLInputElement).value).toBe('P900')
  expect(screen.queryByText('重複姓名')).toBeNull()
})
