import { useAdmin } from '../../hooks/queries'
import { Card, CardContent } from '../../components/ui/card'
import { Button } from '../../components/ui/button'
import { Badge } from '../../components/ui/badge'
import { Table, TableHeader, TableRow, TableHead, TableBody, TableCell } from '../../components/ui/table'
import { Progress } from '../../components/ui/progress'
import { Feedback } from '../../components/Feedback'
export function AdminPage() {
  const query = useAdmin()
  const completed = query.data?.filter(p => p.completedCount === p.totalCount).length ?? 0
  return <><div className="page-intro"><span className="eyebrow">DAILY OVERVIEW</span><h1>當日健檢進度</h1><p>查看受檢者完成狀態，每 5 秒自動更新。</p></div><div className="admin-toolbar"><span className="hint" role="status">{query.isFetching ? '正在同步進度…' : query.dataUpdatedAt ? `上次更新 ${new Date(query.dataUpdatedAt).toLocaleTimeString('zh-TW')}` : '等待資料'}</span><Button variant="outline" disabled={query.isFetching} onClick={() => void query.refetch()}>重新整理</Button></div><Feedback loading={query.isPending} error={query.error} retry={() => void query.refetch()}/>{query.data && <><div className="metrics">{[['受檢者總數', query.data.length], ['已完成', completed], ['進行中', query.data.length - completed]].map(([label, count]) => <Card key={label}><CardContent><p>{label}</p><strong>{count}</strong></CardContent></Card>)}</div><Card className="admin-table"><div className="table-heading"><h2>受檢者一覽</h2><span>即時掌握每一位受檢者的進度</span></div><CardContent><Table><TableHeader><TableRow><TableHead>病歷號</TableHead><TableHead>姓名</TableHead><TableHead>已完成 / 總項目</TableHead><TableHead>進度</TableHead><TableHead>狀態</TableHead></TableRow></TableHeader><TableBody>{query.data.map(p => {const percent = p.totalCount ? Math.round(p.completedCount / p.totalCount * 100) : 0; return <TableRow key={p.patientId}><TableCell className="font-medium">{p.patientId}</TableCell><TableCell><div className="table-name"><span className="table-avatar">{p.patientName.slice(-1)}</span>{p.patientName}</div></TableCell><TableCell>{p.completedCount} / {p.totalCount}</TableCell><TableCell><div className="table-progress"><Progress value={percent} aria-label={`${p.patientName}完成進度`}/><span>{percent}%</span></div></TableCell><TableCell><Badge variant={p.completedCount === p.totalCount ? 'default' : 'secondary'}>{p.completedCount === p.totalCount ? '已完成' : '進行中'}</Badge></TableCell></TableRow>})}</TableBody></Table>{query.data.length === 0 && <p className="feedback">當日沒有受檢者</p>}</CardContent></Card></>}</>
}

