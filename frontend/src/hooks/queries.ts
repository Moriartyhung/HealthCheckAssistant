import { useQuery } from '@tanstack/react-query'
import { getPatientExams, getNextExam } from '../api/patientApi'
import { getAdminProgress } from '../api/adminApi'
export const queryKeys = {
  management: ['patient-management'] as const,
  examCatalog: ['exam-catalog'] as const,
  patients: ['patients'] as const,
  patient: (id: string) => ['patients', id, 'exams'] as const,
  next: (id: string) => ['patients', id, 'next'] as const,
  admin: ['admin', 'progress'] as const,
}
export const usePatient = (id: string) => useQuery({queryKey: queryKeys.patient(id), queryFn: () => getPatientExams(id), enabled: !!id})
export const useNext = (id: string) => useQuery({queryKey: queryKeys.next(id), queryFn: () => getNextExam(id), enabled: !!id})
export const useAdmin = () => useQuery({queryKey: queryKeys.admin, queryFn: getAdminProgress, refetchInterval: 5000})
