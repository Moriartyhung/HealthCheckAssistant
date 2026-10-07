import { afterEach, beforeEach, expect, it, vi } from 'vitest'

beforeEach(() => { vi.resetModules(); vi.stubEnv('VITE_API_BASE_URL', undefined) })
afterEach(() => { vi.unstubAllEnvs(); vi.unstubAllGlobals() })
it('sends all five API calls to the same relative /api prefix', async () => {
  const fetchMock = vi.fn().mockResolvedValue(new Response('{}'))
  // Each response has its own body stream.
  fetchMock.mockImplementation(async () => new Response('{}'))
  vi.stubGlobal('fetch', fetchMock)
  const patient = await import('./patientApi'); const staff = await import('./staffApi'); const admin = await import('./adminApi')
  await patient.getPatientExams('P001'); await patient.getNextExam('P001')
  await staff.checkStation({patientId: 'P001', stationId: 'S002'}); await staff.completeExam('real-token'); await admin.getAdminProgress()
  expect(fetchMock.mock.calls.map(call => call[0])).toEqual(['/api/patients/P001/exams', '/api/patients/P001/next', '/api/ws1/check', '/api/ws2/complete', '/api/admin/progress'])
  expect(fetchMock.mock.calls[2][1]).toMatchObject({method: 'POST', body: JSON.stringify({patientId: 'P001', stationId: 'S002'})})
  expect(fetchMock.mock.calls[3][1]).toMatchObject({method: 'POST', body: JSON.stringify({code1: 'real-token'})})
})
it('uses one configured backend origin without duplicate slashes', async () => {
  vi.stubEnv('VITE_API_BASE_URL', 'http://localhost:8080/')
  const fetchMock = vi.fn().mockResolvedValue(new Response('[]'))
  vi.stubGlobal('fetch', fetchMock)
  await (await import('./client')).request('/admin/progress')
  expect(fetchMock.mock.calls[0][0]).toBe('http://localhost:8080/api/admin/progress')
})
it.each([400, 404, 409, 500])('preserves backend message for HTTP %s', async status => {
  vi.stubGlobal('fetch', vi.fn().mockResolvedValue(new Response('{"message":"後端錯誤訊息"}', {status})))
  await expect((await import('./client')).request('/ws2/complete')).rejects.toThrow('後端錯誤訊息')
})
it('treats a 200 business denial as a result', async () => {
  const denial = {allowed: false, code1: null, message: '不在允許檢查時間', remainingExams: []}
  vi.stubGlobal('fetch', vi.fn().mockResolvedValue(new Response(JSON.stringify(denial))))
  await expect((await import('./client')).request('/ws1/check')).resolves.toEqual(denial)
})
it('handles a non-JSON proxy failure with a readable fallback', async () => {
  vi.stubGlobal('fetch', vi.fn().mockResolvedValue(new Response('Bad Gateway', {status: 502})))
  await expect((await import('./client')).request('/admin/progress')).rejects.toThrow('API 回應格式錯誤，請確認服務已啟動。')
})
