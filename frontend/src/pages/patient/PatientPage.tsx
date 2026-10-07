import { useState } from 'react'
import { ArrowRight, CheckCircle2, Circle } from 'lucide-react'
import { usePatient, useNext } from '../../hooks/queries'
import { Card, CardHeader, CardTitle, CardContent } from '../../components/ui/card'
import { Input } from '../../components/ui/input'
import { Button } from '../../components/ui/button'
import { Badge } from '../../components/ui/badge'
import { Feedback } from '../../components/Feedback'
import { ProgressSummary } from '../../components/ProgressSummary'
export function PatientPage() {
  const [input, setInput] = useState('P001')
  const [id, setId] = useState('P001')
  const patient = usePatient(id)
  const next = useNext(id)
  return <><div className="page-intro"><span className="eyebrow">MY HEALTH CHECK</span><h1>我的健檢進度</h1><p>掌握已完成的檢查，找到下一個站點。</p></div><form className="patient-search" onSubmit={e => {e.preventDefault(); setId(input.trim().toUpperCase())}}><label htmlFor="lookup">病歷號</label><Input id="lookup" value={input} onChange={e => setInput(e.target.value)} required placeholder="P001 / P002"/><Button disabled={!input.trim()}>查看進度</Button></form><Feedback loading={patient.isPending} error={patient.error} retry={() => void patient.refetch()}/>
    {patient.data && <><div className="patient-overview"><Card><CardHeader><div className="flex items-center justify-between gap-4"><CardTitle>{patient.data.patientName}</CardTitle><Badge variant="secondary">{patient.data.patientId}</Badge></div></CardHeader><CardContent><ProgressSummary completed={patient.data.completedCount} total={patient.data.totalCount}/></CardContent></Card><Card className="next-station"><CardHeader><CardTitle>下一站建議</CardTitle></CardHeader><CardContent><Feedback loading={next.isPending} error={next.error} retry={() => void next.refetch()}/>{next.data && <><p>{next.data.message}</p>{next.data.nextExam && <div className="next-detail"><div><h2>{next.data.nextExam.stationName}</h2><p>{next.data.nextExam.examName} · {next.data.nextExam.stationId}</p><p className="hint">開放時間 {next.data.nextExam.startTime}–{next.data.nextExam.endTime}</p></div><ArrowRight aria-hidden="true"/></div>}</>}</CardContent></Card></div><div className="patient-grid">{(['PENDING', 'COMPLETED'] as const).map(status => <Card key={status}><CardHeader><CardTitle>{status === 'PENDING' ? '待完成項目' : '已完成項目'}</CardTitle></CardHeader><CardContent>{patient.data.exams.filter(e => e.status === status).length === 0 && <p className="hint">{status === 'PENDING' ? '已完成所有檢查' : '尚無已完成項目'}</p>}<ul className="exam-list">{patient.data.exams.filter(e => e.status === status).map(exam => <li key={exam.examId}>{status === 'COMPLETED' ? <CheckCircle2 className="text-primary"/> : <Circle className="text-muted-foreground"/>}<div><strong>{exam.examName}</strong><p>{exam.stationName} · {exam.startTime}–{exam.endTime}</p>{exam.completedAt && <p>完成於 {new Date(exam.completedAt).toLocaleTimeString('zh-TW')}</p>}</div></li>)}</ul></CardContent></Card>)}</div></>}
  </>
}

