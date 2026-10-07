import { useState } from 'react'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { CheckCircle2, ScanLine, UserRound, MapPin, ShieldCheck } from 'lucide-react'
import { checkStation, completeExam } from '../../api/staffApi'
import { usePatient, queryKeys } from '../../hooks/queries'
import type { CheckResponse } from '../../types/api'
import { Button } from '../../components/ui/button'
import { Input } from '../../components/ui/input'
import { Card, CardHeader, CardTitle, CardContent } from '../../components/ui/card'
import { Badge } from '../../components/ui/badge'
import { QrScannerInput } from '../../components/QrScanner'
import { Feedback } from '../../components/Feedback'

export function StaffPage() {
  const [patientInput, setPatientInput] = useState('P001')
  const [patientId, setPatientId] = useState('P001')
  const [stationId, setStationId] = useState('S002')
  const [result, setResult] = useState<CheckResponse | null>(null)
  const [success, setSuccess] = useState('')
  const patient = usePatient(patientId)
  const client = useQueryClient()
  const check = useMutation({mutationFn: checkStation, onSuccess: data => setResult(data)})
  const complete = useMutation({mutationFn: completeExam, onSuccess: async data => {
    setSuccess(data.message); setResult(null)
    await Promise.all([client.invalidateQueries({queryKey: queryKeys.patients}), client.invalidateQueries({queryKey: queryKeys.admin})])
  }})
  const busy = check.isPending || complete.isPending
  function reset() {setResult(null); setSuccess(''); check.reset(); complete.reset()}
  function selectPatient(value: string) {reset(); setPatientInput(value); setPatientId(value)}
  return <><div className="page-intro"><span className="eyebrow">STAFF WORKSPACE</span><h1>工作人員作業</h1><p>辨識受檢者與站點，確認資格後完成檢查。</p></div>
    <div className="steps" aria-label="操作流程">{[{title: '辨識受檢者', detail: '掃描 QR 或輸入病歷號', icon: UserRound}, {title: '判斷站點', detail: '確認檢查資格與前置條件', icon: MapPin}, {title: '確認完成', detail: '完成檢查並同步進度', icon: ShieldCheck}].map(({title, detail, icon: Icon}, index) => <div className="step" key={title}><span className="step-number"><Icon/></span><div><strong><span className="step-index">0{index + 1}</span>{title}</strong><small>{detail}</small></div></div>)}</div>
    <div className="staff-grid"><Card><CardHeader><div className="card-heading"><span className="card-icon"><UserRound/></span><div><CardTitle>受檢者辨識</CardTitle><p>確認身分，開始這次檢查</p></div></div></CardHeader><CardContent className="space-y-4"><form onSubmit={e => {e.preventDefault(); selectPatient(patientInput.trim().toUpperCase())}}><label htmlFor="patient-id">病歷號</label><div className="input-row"><Input id="patient-id" value={patientInput} disabled={busy} onChange={e => {reset(); setPatientId(''); setPatientInput(e.target.value)}} placeholder="例如 P001" required/><Button disabled={busy || !patientInput.trim()}>查詢</Button></div></form><QrScannerInput label="受檢者" disabled={busy} onScan={selectPatient}/><Feedback loading={!!patientId && patient.isPending} error={patient.error} retry={() => void patient.refetch()}/>{patient.data && <div className="patient-identity"><div className="avatar">{patient.data.patientName.slice(-1)}</div><div><strong>{patient.data.patientName}</strong><p>{patient.data.patientId} · 已完成 {patient.data.completedCount} / {patient.data.totalCount} 項</p></div></div>}<p className="hint">Demo 病歷號：P001、P002</p></CardContent></Card>
    <Card><CardHeader><div className="card-heading"><span className="card-icon blue"><MapPin/></span><div><CardTitle>檢查站點</CardTitle><p>選定站點，確認是否可以進行</p></div></div></CardHeader><CardContent className="space-y-4"><label htmlFor="station-id">站點代碼</label><Input id="station-id" value={stationId} disabled={busy} onChange={e => {reset(); setStationId(e.target.value.toUpperCase())}} placeholder="例如 S002"/><QrScannerInput label="站點" disabled={busy} onScan={value => {reset(); setStationId(value)}}/><p className="hint">S001 抽血 · S002 X光 · S003 心電圖<br/>S004 超音波 · S005 醫師總評</p><Button className="w-full" disabled={busy || !patient.data || !patientId || !stationId.trim()} onClick={() => {reset(); check.mutate({patientId, stationId: stationId.trim()})}}><ScanLine/>{check.isPending ? '資格判斷中…' : '判斷是否可進行檢查'}</Button></CardContent></Card></div>
    <Feedback error={check.error || complete.error}/>
    {result && <Card className="result-card"><CardHeader><CardTitle className="flex items-center gap-3"><Badge variant={result.allowed ? 'default' : 'destructive'}>{result.allowed ? '允許檢查' : '暫不可檢查'}</Badge>{result.examName ?? '資格判斷結果'}</CardTitle></CardHeader><CardContent className="space-y-4"><p role="status">{result.message}</p>{result.allowed && result.code1 && <><p className="hint">確認碼 code1</p><code className="token">{result.code1}</code><Button disabled={busy} onClick={() => complete.mutate(result.code1!)}><CheckCircle2/>{complete.isPending ? '更新中…' : '確定完成此站點'}</Button></>}{result.remainingExams.length > 0 && <p className="hint">其他待完成：{result.remainingExams.map(e => e.examName).join('、')}</p>}</CardContent></Card>}
    {success && <div className="feedback success" role="status"><CheckCircle2/><span>{success}，可繼續判斷下一站。</span></div>}
  </>
}


