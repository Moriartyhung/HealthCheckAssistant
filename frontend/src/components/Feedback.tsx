import { Button } from './ui/button'
export function Feedback({error, loading, retry}: {error?: Error | null; loading?: boolean; retry?: () => void}) {
  if (error) return <div className="feedback error" role="alert"><p>{error.message}</p>{retry && <Button variant="outline" onClick={retry}>重新載入</Button>}</div>
  if (loading) return <p role="status" className="feedback">資料載入中…</p>
  return null
}
