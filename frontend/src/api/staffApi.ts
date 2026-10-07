import { request } from './client'
import type { CheckRequest, CheckResponse, CompleteResponse } from '../types/api'
export const checkStation = (body: CheckRequest) => request<CheckResponse>('/ws1/check', {method: 'POST', body: JSON.stringify(body)})
export const completeExam = (code1: string) => request<CompleteResponse>('/ws2/complete', {method: 'POST', body: JSON.stringify({code1})})
