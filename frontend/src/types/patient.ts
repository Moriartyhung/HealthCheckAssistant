import type { Exam } from './exam'
export interface PatientProgress {
  patientId: string
  patientName: string
  completedCount: number
  totalCount: number
  exams: Exam[]
}
export type AdminProgress = Omit<PatientProgress, 'exams'>
