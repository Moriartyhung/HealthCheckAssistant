// @vitest-environment jsdom
import { afterEach, beforeEach, expect, it, vi } from 'vitest'
import { cleanup, render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { PatientManagement } from './PatientManagement'

const options = [
  {examId: 'A1', examName: '視力測試', stationId: 'V1', stationName: '視力站', startTime: '08:00', endTime: '17:00', requiredExamIds: []},
  {examId: 'A2', examName: '醫師檢查', stationId: 'V2', stationName: '醫師站', startTime: '08:00', endTime: '17:00', requiredExamIds: ['A1']},
  {examId: 'A3', examName: '其他檢查', stationId: 'V3', stationName: '其他站', startTime: '08:00', endTime: '17:00', requiredExamIds: []},
]
const assigned = (id: string, removable = true) => ({...options.find(o => o.examId === id)!, status: removable ? 'PENDING' : 'COMPLETED',
  completedAt: removable ? null : '2026-10-09T10:30:00', removable, removalBlockReason: removable ? null : '已完成項目不可移除'})
let calls: {url: string; method: string; body?: any}[]
let detail: any
beforeEach(() => {
  calls = []; detail = {patientId: 'P900', patientName: '測試姓名', exams: [assigned('A1', false), assigned('A3')]}
  vi.stubGlobal('fetch', vi.fn(async (url: string, init?: RequestInit) => {
    const method = init?.method ?? 'GET'; const body = init?.body ? JSON.parse(String(init.body)) : undefined
    calls.push({url, method, body})
    if (url.endsWith('/admin/exams')) return new Response(JSON.stringify(options))
    if (method === 'POST') return new Response(JSON.stringify({...body, exams: body.examIds.map((id: string) => assigned(id))}), {status: 201})
    if (method === 'PUT') {detail = {...detail, patientName: body.patientName, exams: body.examIds.map((id: string) => detail.exams.find((e: any) => e.examId === id) ?? assigned(id))}; return new Response(JSON.stringify(detail))}
    return new Response(JSON.stringify(url.endsWith('/patients') ? [{patientId: detail.patientId, patientName: detail.patientName}] : detail))
  }))
})
afterEach(() => {cleanup(); vi.unstubAllGlobals()})
function mount() {render(<QueryClientProvider client={new QueryClient({defaultOptions: {queries: {retry: false}, mutations: {retry: false}}})}><PatientManagement mockMode={false}/></QueryClientProvider>)}
it('loads real catalog with station names and posts multiple selected exams', async () => {
  const user = userEvent.setup(); mount()
  await screen.findByRole('checkbox', {name: /視力測試.*視力站/})
  await user.type(screen.getByLabelText('病歷號'), 'P901'); await user.type(screen.getByLabelText('姓名'), '新姓名')
  await user.click(screen.getByRole('checkbox', {name: /視力測試/})); await user.click(screen.getByRole('checkbox', {name: /醫師檢查/}))
  await user.click(screen.getByRole('button', {name: '新增受檢者'}))
  await screen.findByText('受檢者已新增')
  expect(calls.find(c => c.method === 'POST')?.body).toEqual({patientId: 'P901', patientName: '新姓名', examIds: ['A1', 'A2']})
})
it('loads full detail for edit, protects completed exams and changes pending assignments', async () => {
  const user = userEvent.setup(); mount(); await screen.findByRole('button', {name: '編輯 P900'})
  await user.click(screen.getByRole('button', {name: '編輯 P900'}))
  await waitFor(() => expect((screen.getByLabelText('病歷號') as HTMLInputElement).disabled).toBe(true))
  const completed = screen.getByRole('checkbox', {name: /視力測試/}) as HTMLInputElement
  expect(completed.checked).toBe(true); expect(completed.disabled).toBe(true)
  expect(screen.getByText('2026-10-09T10:30:00')).toBeTruthy()
  await user.click(screen.getByRole('checkbox', {name: /其他檢查/})); await user.click(screen.getByRole('checkbox', {name: /醫師檢查/}))
  await user.clear(screen.getByLabelText('姓名')); await user.type(screen.getByLabelText('姓名'), '修改姓名')
  await user.click(screen.getByRole('button', {name: '儲存姓名與安排'}))
  await screen.findByText('姓名與健檢安排已更新')
  expect(calls.find(c => c.method === 'PUT')?.body).toEqual({patientName: '修改姓名', examIds: ['A1', 'A2']})
})
it('view details displays station, status and completion timestamp', async () => {
  const user = userEvent.setup(); mount(); await screen.findByRole('button', {name: '查看安排 P900'})
  await user.click(screen.getByRole('button', {name: '查看安排 P900'}))
  await screen.findByText('2026-10-09T10:30:00')
  expect(screen.getByRole('region', {name: '受檢者健檢安排'}).textContent).toContain('已完成')
  expect(screen.getByRole('region', {name: '受檢者健檢安排'}).textContent).toContain('未完成')
})
it('blocks save if catalog failed instead of submitting a partial arrangement', async () => {
  const original = globalThis.fetch
  vi.stubGlobal('fetch', vi.fn((url: string, init?: RequestInit) => url.endsWith('/admin/exams')
    ? Promise.resolve(new Response('{"message":"無法載入檢查項目"}', {status: 500})) : original(url, init)))
  const user = userEvent.setup(); mount(); await screen.findByText('無法載入檢查項目')
  await user.type(screen.getByLabelText('病歷號'), 'P901'); await user.type(screen.getByLabelText('姓名'), '姓名')
  expect((screen.getByRole('button', {name: '新增受檢者'}) as HTMLButtonElement).disabled).toBe(true)
  expect(calls.some(c => c.method === 'POST')).toBe(false)
})
it('shows the explicitly viewed patient while another patient remains in the edit form', async () => {
  const original = globalThis.fetch
  vi.stubGlobal('fetch', vi.fn((url: string, init?: RequestInit) => {
    if (url.endsWith('/patients')) return Promise.resolve(new Response(JSON.stringify([{patientId: 'P900', patientName: '甲'}, {patientId: 'P901', patientName: '乙'}])))
    if (url.endsWith('/patients/P901')) return Promise.resolve(new Response(JSON.stringify({patientId: 'P901', patientName: '乙', exams: []})))
    return original(url, init)
  }))
  const user = userEvent.setup(); mount(); await screen.findByRole('button', {name: '編輯 P900'})
  await user.click(screen.getByRole('button', {name: '編輯 P900'})); await screen.findByText('P900 的健檢安排')
  await user.click(screen.getByRole('button', {name: '查看安排 P901'})); await screen.findByText('P901 的健檢安排')
  expect((screen.getByLabelText('病歷號') as HTMLInputElement).value).toBe('P900')
})
it('protects a pending assignment with an active WS1 token', async () => {
  detail.exams[0] = {...assigned('A1'), removable: false, removalBlockReason: '已有有效 WS1 確認碼，不可移除'}
  const user = userEvent.setup(); mount(); await screen.findByRole('button', {name: '編輯 P900'})
  await user.click(screen.getByRole('button', {name: '編輯 P900'})); await screen.findByText('P900 的健檢安排')
  const input = screen.getByRole('checkbox', {name: /視力測試/}) as HTMLInputElement
  expect(input.checked).toBe(true); expect(input.disabled).toBe(true)
})
it('prevents saving a selection missing its prerequisite', async () => {
  const user = userEvent.setup(); mount(); await screen.findByRole('checkbox', {name: /醫師檢查/})
  await user.type(screen.getByLabelText('病歷號'), 'P901'); await user.type(screen.getByLabelText('姓名'), '姓名')
  await user.click(screen.getByRole('checkbox', {name: /醫師檢查/}))
  expect(screen.getByText('部分項目缺少前置檢查，請一併勾選。')).toBeTruthy()
  expect((screen.getByRole('button', {name: '新增受檢者'}) as HTMLButtonElement).disabled).toBe(true)
})
