import type { Exam } from '../types/exam'
import type { PatientProgress } from '../types/patient'

const catalog: Omit<Exam, 'status' | 'completedAt'>[] = [
  {examId: 'E001', examName: '血液檢查', stationId: 'S001', stationName: '抽血站', startTime: '08:00', endTime: '11:00'},
  {examId: 'E002', examName: '胸部X光', stationId: 'S002', stationName: 'X光站', startTime: '08:00', endTime: '16:00'},
  {examId: 'E003', examName: '心電圖', stationId: 'S003', stationName: '心電圖站', startTime: '08:00', endTime: '15:00'},
  {examId: 'E004', examName: '腹部超音波', stationId: 'S004', stationName: '超音波站', startTime: '09:00', endTime: '15:00'},
  {examId: 'E005', examName: '醫師總評', stationId: 'S005', stationName: '醫師總評站', startTime: '10:00', endTime: '17:00'},
]
export const patients = new Map<string, PatientProgress>()
export const confirmations = new Map<string, {patientId: string; examId: string; used: boolean}>()
let mockTime = '10:30'
export function setMockTime(time: string) { mockTime = time }
export function resetStore() {
  patients.clear(); confirmations.clear(); mockTime = '10:30'
  for (const [id, name, count] of [['P001', '測試受檢者A', 5], ['P002', '測試受檢者B', 3]] as const) {
    patients.set(id, {patientId: id, patientName: name, completedCount: 0, totalCount: count,
      exams: catalog.slice(0, count).map(exam => ({...exam, status: 'PENDING', completedAt: null}))})
  }
}
export function denial(patient: PatientProgress, exam: Exam) {
  if (exam.status === 'COMPLETED') return '此檢查已完成，無需重複檢查'
  if (mockTime < exam.startTime || mockTime > exam.endTime) return `不在允許檢查時間（${exam.startTime}–${exam.endTime}）`
  if (exam.examId === 'E005' && ['E001', 'E002', 'E003', 'E004'].some(id => !patient.exams.some(e => e.examId === id && e.status === 'COMPLETED'))) return '前置檢查尚未完成：請先完成血液、X光、心電圖與超音波'
  return null
}
resetStore()
