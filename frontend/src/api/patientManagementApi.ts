import { request } from './client'
import { USE_MSW } from '../config/runtime'

export interface ManagedPatient { patientId: string; patientName: string }
export interface ExamOption { examId: string; examName: string; stationId: string; stationName: string; startTime: string; endTime: string; requiredExamIds: string[] }
export interface ManagedExam extends Omit<ExamOption, 'requiredExamIds'> { status: 'PENDING' | 'COMPLETED'; completedAt: string | null; removable: boolean; removalBlockReason: string | null }
export interface ManagedPatientDetail extends ManagedPatient { exams: ManagedExam[] }
function realRequest<T>(path: string, options?: RequestInit) {
  if (USE_MSW) return Promise.reject<T>(new Error('模擬模式下停用受檢者資料管理，請切換正式 API 模式'))
  return request<T>(path, options)
}
const base = '/admin/patients'
export const listPatients = () => realRequest<ManagedPatient[]>(base)
export const getManagedPatient = (id: string) => realRequest<ManagedPatientDetail>(`${base}/${encodeURIComponent(id)}`)
export const createPatient = (patient: ManagedPatient & {examIds?: string[]}) => realRequest<ManagedPatientDetail>(base, {method: 'POST', body: JSON.stringify(patient)})
export const updatePatientName = (id: string, patientName: string, examIds?: string[]) => realRequest<ManagedPatientDetail>(`${base}/${encodeURIComponent(id)}`, {method: 'PUT', body: JSON.stringify({patientName, ...(examIds !== undefined ? {examIds} : {})})})
export const getExamCatalog = () => realRequest<ExamOption[]>('/admin/exams')
export const deletePatient = (id: string) => realRequest<{message: string}>(`${base}/${encodeURIComponent(id)}`, {method: 'DELETE'})
