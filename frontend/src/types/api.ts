import type { Exam } from './exam'
export interface CheckRequest { patientId: string; stationId: string }
export interface CheckResponse {
  allowed: boolean; message: string; examId?: string; examName?: string
  remainingExams: Pick<Exam, 'examId' | 'examName'>[]
  code1: string | null
}
export interface CompleteResponse { success: boolean; message: string }
export interface NextResponse { nextExam: Exam | null; message: string }
