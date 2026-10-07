import { Progress } from './ui/progress'
export function ProgressSummary({completed, total}: {completed: number; total: number}) {
  const value = total ? Math.round(completed / total * 100) : 0
  return <div className="progress-summary"><div><strong>{completed} / {total}</strong><span>{value}%</span></div><Progress value={value} aria-label="健檢完成進度"/><p>{completed === total ? '所有檢查已完成' : `還有 ${total - completed} 項檢查待完成`}</p></div>
}
