export interface Exam {
  examId: string
  examName: string
  stationId: string
  stationName: string
  startTime: string
  endTime: string
  status: 'PENDING' | 'COMPLETED'
  completedAt: string | null
}
