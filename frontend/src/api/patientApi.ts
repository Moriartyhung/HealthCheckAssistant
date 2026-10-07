import { request } from './client'
import type { PatientProgress } from '../types/patient'
import type { NextResponse } from '../types/api'
export const getPatientExams = (id: string) => request<PatientProgress>(`/patients/${encodeURIComponent(id)}/exams`)
export const getNextExam = (id: string) => request<NextResponse>(`/patients/${encodeURIComponent(id)}/next`)
