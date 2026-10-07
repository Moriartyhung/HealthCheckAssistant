import { beforeAll, afterAll, beforeEach, describe, expect, it } from 'vitest'
import { setupServer } from 'msw/node'
import { handlers } from './handlers'
import { resetStore, setMockTime } from './store'

const server = setupServer(...handlers)
beforeAll(() => server.listen({ onUnhandledFrame: 'error' }))
afterAll(() => server.close())
beforeEach(() => resetStore())
const get = (path: string) => fetch(`http://localhost/api${path}`)
const post = (path: string, body: unknown) => fetch(`http://localhost/api${path}`, { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(body) })

describe('健檢 API contract', () => {
  it('WS2 updates patient and admin progress, and rejects reused code1', async () => {
    const check = await (await post('/ws1/check', {patientId: 'P001', stationId: 'S002'})).json()
    expect(check.allowed).toBe(true)
    expect(check.examId).toBe('E002')
    const complete = await post('/ws2/complete', {code1: check.code1})
    expect((await complete.json()).success).toBe(true)
    const patient = await (await get('/patients/P001/exams')).json()
    expect(patient.completedCount).toBe(1)
    expect(patient.exams.find((e: {examId: string}) => e.examId === 'E002').status).toBe('COMPLETED')
    const admin = await (await get('/admin/progress')).json()
    expect(admin.find((p: {patientId: string}) => p.patientId === 'P001').completedCount).toBe(1)
    expect((await post('/ws2/complete', {code1: check.code1})).status).toBe(409)
    expect(await (await post('/ws1/check', {patientId: 'P001', stationId: 'S002'})).json()).toMatchObject({allowed: false, code1: null})
  })
  it('blocks prerequisite and closed-window exams without issuing a token', async () => {
    expect(await (await post('/ws1/check', {patientId: 'P001', stationId: 'S005'})).json()).toMatchObject({allowed: false, code1: null})
    setMockTime('17:30')
    expect(await (await post('/ws1/check', {patientId: 'P001', stationId: 'S002'})).json()).toMatchObject({allowed: false, code1: null})
    expect(await (await get('/patients/P001/next')).json()).toMatchObject({nextExam: null})
  })
  it('recommends only eligible pending exams and eventually reports all completed', async () => {
    expect(await (await get('/patients/P001/next')).json()).toMatchObject({nextExam: {examId: 'E001', stationId: 'S001'}})
    for (const stationId of ['S001', 'S002', 'S003', 'S004', 'S005']) {
      const check = await (await post('/ws1/check', {patientId: 'P001', stationId})).json()
      expect(check.allowed).toBe(true)
      expect((await post('/ws2/complete', {code1: check.code1})).status).toBe(200)
    }
    expect(await (await get('/patients/P001/next')).json()).toMatchObject({nextExam: null})
    expect(await (await get('/patients/P001/exams')).json()).toMatchObject({completedCount: 5, totalCount: 5})
  })
  it('handles unknown identifiers, invalid tokens and malformed bodies', async () => {
    expect((await get('/patients/UNKNOWN/exams')).status).toBe(404)
    expect((await post('/ws1/check', {patientId: 'P001', stationId: 'S999'})).status).toBe(404)
    expect((await post('/ws2/complete', {code1: 'invalid'})).status).toBe(404)
    expect((await post('/ws1/check', {})).status).toBe(400)
  })
})
