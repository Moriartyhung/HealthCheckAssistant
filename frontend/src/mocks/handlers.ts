import { http, HttpResponse } from 'msw'
import { patients, confirmations, denial } from './store'

const error = (message: string, status: number) => HttpResponse.json({message}, {status})
async function readBody(request: Request): Promise<Record<string, unknown>> {
  try {
    const value: unknown = await request.json()
    return typeof value === 'object' && value !== null && !Array.isArray(value) ? value as Record<string, unknown> : {}
  } catch { return {} }
}
export const handlers = [
  http.post('*/api/ws1/check', async ({request}) => {
    const {patientId, stationId} = await readBody(request)
    if (typeof patientId !== 'string' || typeof stationId !== 'string' || !patientId || !stationId) return error('請提供受檢者與站點代碼', 400)
    const patient = patients.get(patientId)
    if (!patient) return error('找不到受檢者', 404)
    if (!['S001', 'S002', 'S003', 'S004', 'S005'].includes(stationId)) return error('找不到站點', 404)
    const exam = patient.exams.find(e => e.stationId === stationId)
    const remainingExams = patient.exams.filter(e => e.status === 'PENDING').map(({examId, examName}) => ({examId, examName}))
    const reason = exam ? denial(patient, exam) : '受檢者未安排此站檢查'
    if (!exam || reason) return HttpResponse.json({allowed: false, message: reason, remainingExams, code1: null})
    const code1 = `MOCK-${crypto.randomUUID()}`
    confirmations.set(code1, {patientId, examId: exam.examId, used: false})
    return HttpResponse.json({allowed: true, message: '可以進行檢查', examId: exam.examId, examName: exam.examName, remainingExams: remainingExams.filter(e => e.examId !== exam.examId), code1})
  }),
  http.post('*/api/ws2/complete', async ({request}) => {
    const {code1} = await readBody(request)
    if (typeof code1 !== 'string' || !code1) return error('請提供 code1', 400)
    const confirmation = confirmations.get(code1)
    if (!confirmation) return error('無效的確認碼，請重新判斷', 404)
    if (confirmation.used) return error('確認碼已使用，請勿重複提交', 409)
    const patient = patients.get(confirmation.patientId)!
    const exam = patient.exams.find(e => e.examId === confirmation.examId)!
    if (exam.status === 'COMPLETED') return error('此檢查已完成', 409)
    confirmation.used = true
    exam.status = 'COMPLETED'; exam.completedAt = new Date().toISOString()
    patient.completedCount = patient.exams.filter(e => e.status === 'COMPLETED').length
    return HttpResponse.json({success: true, message: '檢查完成狀態已更新'})
  }),
  http.get('*/api/patients/:patientId/exams', ({params}) => {
    const patient = patients.get(String(params.patientId))
    return patient ? HttpResponse.json(patient) : error('找不到受檢者', 404)
  }),
  http.get('*/api/patients/:patientId/next', ({params}) => {
    const patient = patients.get(String(params.patientId))
    if (!patient) return error('找不到受檢者', 404)
    const nextExam = patient.exams.find(e => !denial(patient, e)) ?? null
    return HttpResponse.json({nextExam, message: nextExam ? '建議前往以下站點' : patient.completedCount === patient.totalCount ? '所有檢查已完成，謝謝您的配合！' : '目前沒有可進行的檢查，請洽工作人員'})
  }),
  http.get('*/api/admin/progress', () => HttpResponse.json([...patients.values()].map(({exams: _exams, ...progress}) => progress))),
]

