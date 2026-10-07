import { request } from './client'
import type { AdminProgress } from '../types/patient'
export const getAdminProgress = () => request<AdminProgress[]>('/admin/progress')
