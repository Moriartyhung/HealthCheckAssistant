import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { USE_MSW } from '../../config/runtime'
import { queryKeys } from '../../hooks/queries'
import { createPatient, deletePatient, getManagedPatient, listPatients, updatePatientName, getExamCatalog } from '../../api/patientManagementApi'
import type { ManagedPatient, ManagedPatientDetail } from '../../api/patientManagementApi'
import { Button } from '../../components/ui/button'
import { Input } from '../../components/ui/input'
import { Card, CardContent, CardHeader, CardTitle } from '../../components/ui/card'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '../../components/ui/table'
import { Feedback } from '../../components/Feedback'

export function PatientManagement({mockMode = USE_MSW}: {mockMode?: boolean}) {
  if (mockMode) return <section aria-label="受檢者資料管理"><h2>受檢者資料管理</h2><p className="feedback">模擬模式下停用受檢者資料管理，請切換正式 API 模式。</p></section>
  return <ManagementForm/>
}

function ManagementForm() {
  const client = useQueryClient()
  const [search, setSearch] = useState('')
  const [filter, setFilter] = useState('')
  const [id, setId] = useState('')
  const [name, setName] = useState('')
  const [editing, setEditing] = useState(false)
  const [deleting, setDeleting] = useState<ManagedPatient | null>(null)
  const [message, setMessage] = useState('')
  const [selected, setSelected] = useState<string[]>([])
  const [detail, setDetail] = useState<ManagedPatientDetail | null>(null)
  const [editingPatient, setEditingPatient] = useState<ManagedPatientDetail | null>(null)
  const catalog = useQuery({queryKey: queryKeys.examCatalog, queryFn: getExamCatalog})
  const patients = useQuery({queryKey: [...queryKeys.management, filter], queryFn: async () => filter ? [await getManagedPatient(filter)] : listPatients()})
  function clearForm() { setId(''); setName(''); setEditing(false); setSelected([]); setEditingPatient(null); setDetail(null) }
  const load = useMutation({
    mutationFn: async ({patientId, edit}: {patientId: string; edit: boolean}) => ({patient: await getManagedPatient(patientId), edit}),
    onSuccess: ({patient, edit}) => {
      setDetail(patient)
      if (edit) {setId(patient.patientId); setName(patient.patientName); setSelected(patient.exams.map(e => e.examId)); setEditing(true); setEditingPatient(patient)}
    },
  })
  const mutation = useMutation({
    mutationFn: async (action: 'create' | 'update' | 'delete') => {
      if (action === 'delete') { if (!deleting) throw new Error('請選擇受檢者'); return (await deletePatient(deleting.patientId)).message }
      if (action === 'update') { await updatePatientName(id, name.trim(), selected); return '姓名與健檢安排已更新' }
      await createPatient({patientId: id.trim(), patientName: name.trim(), examIds: selected}); return '受檢者已新增'
    },
    onSuccess: async text => {
      setMessage(text); clearForm(); setDeleting(null); setFilter(''); setSearch('')
      await Promise.all([
        client.invalidateQueries({queryKey: queryKeys.management}),
        client.invalidateQueries({queryKey: queryKeys.admin}),
        client.invalidateQueries({queryKey: queryKeys.patients}),
      ])
    },
  })
  const busy = mutation.isPending || load.isPending
  function resetFeedback() { mutation.reset(); load.reset(); setMessage('') }
  function selectFilter(value: string) {
    resetFeedback()
    setDetail(null)
    if (value === filter) void patients.refetch()
    else setFilter(value)
  }
  const missingPrerequisite = catalog.data?.some(e => selected.includes(e.examId) && e.requiredExamIds.some(required => !selected.includes(required))) ?? false
  const queryDetail = filter && patients.data?.[0] && 'exams' in patients.data[0] ? patients.data[0] as ManagedPatientDetail : null
  const shownDetail = detail ?? queryDetail ?? editingPatient
  return <section className="patient-management" aria-label="受檢者資料管理">
    <Card><CardHeader><CardTitle>受檢者資料管理</CardTitle><p className="hint">病歷號建立後不可修改；已有檢查紀錄或確認碼的受檢者不可刪除。</p></CardHeader><CardContent>
      <form className="management-form" onSubmit={e => {e.preventDefault(); resetFeedback(); mutation.mutate(editing ? 'update' : 'create')}}>
        <div><label htmlFor="managed-id">病歷號</label><Input id="managed-id" value={id} onChange={e => setId(e.target.value)} required maxLength={20} disabled={editing || busy || !!deleting}/></div>
        <div><label htmlFor="managed-name">姓名</label><Input id="managed-name" value={name} onChange={e => setName(e.target.value)} required maxLength={100} disabled={busy || !!deleting}/><p className="hint">上限 100 bytes，中文可輸入長度依資料庫字元集而定。</p></div>
        <fieldset className="exam-picker" disabled={busy || !!deleting}><legend>安排健檢項目</legend>
          <p className="hint">每個站點最多安排一項檢查；請一併勾選所需前置項目。已完成或已有確認碼紀錄的項目不能取消。</p>
          <Feedback loading={catalog.isPending} error={catalog.error} retry={() => void catalog.refetch()}/>
          {catalog.data?.map(option => {
            const existing = editingPatient?.exams.find(e => e.examId === option.examId)
            const blocked = !!existing && !existing.removable
            const sameStation = !selected.includes(option.examId) && catalog.data.some(other => other.stationId === option.stationId && selected.includes(other.examId))
            const requiredNames = option.requiredExamIds.map(required => catalog.data.find(e => e.examId === required)?.examName ?? required)
            return <label className="exam-choice" key={option.examId}><input type="checkbox" aria-label={`${option.examName} · ${option.stationName}`} checked={selected.includes(option.examId)} disabled={blocked || sameStation}
              onChange={e => setSelected(current => e.target.checked ? [...current, option.examId].sort() : current.filter(id => id !== option.examId))}/>
              <span><strong>{option.examName} · {option.stationName}</strong><small>{option.examId} · {option.startTime}–{option.endTime}{existing && ` · ${existing.status === 'COMPLETED' ? '已完成' : '未完成'}`}</small>
                {requiredNames.length > 0 && <small>前置項目：{requiredNames.join('、')}</small>}{blocked && <small>{existing.removalBlockReason}</small>}{sameStation && <small>此站點已選其他項目</small>}
              </span></label>
          })}
          {catalog.data?.length === 0 && <p>目前沒有可安排的檢查項目</p>}
          {missingPrerequisite && <p role="alert">部分項目缺少前置檢查，請一併勾選。</p>}
        </fieldset>
        <div className="management-actions"><Button disabled={busy || !!deleting || !id.trim() || !name.trim() || !catalog.isSuccess || missingPrerequisite}>{editing ? '儲存姓名與安排' : '新增受檢者'}</Button>{editing && <Button type="button" variant="outline" disabled={busy} onClick={() => {clearForm(); resetFeedback()}}>取消編輯</Button>}</div>
      </form>
      <Feedback error={mutation.error || load.error}/>{message && <p className="feedback success" role="status">{message}</p>}
      {deleting && <div className="delete-confirmation" role="alertdialog" aria-labelledby="delete-title" aria-describedby="delete-detail">
        <h3 id="delete-title">刪除受檢者</h3><p id="delete-detail">確定刪除 {deleting.patientId}（{deleting.patientName}）？</p>
        <div className="management-actions"><Button type="button" disabled={busy} onClick={() => {resetFeedback(); mutation.mutate('delete')}}>確認刪除</Button><Button type="button" variant="outline" disabled={busy} onClick={() => {setDeleting(null); resetFeedback()}}>取消刪除</Button></div>
      </div>}
      <form className="patient-search" onSubmit={e => {e.preventDefault(); selectFilter(search.trim())}}>
        <label htmlFor="managed-search">查詢病歷號</label><Input id="managed-search" value={search} onChange={e => setSearch(e.target.value)} maxLength={20} disabled={busy}/>
        <Button disabled={busy}>查詢</Button><Button type="button" variant="outline" disabled={busy} onClick={() => {setSearch(''); selectFilter('')}}>顯示全部</Button>
      </form>
      <Feedback loading={patients.isPending} error={patients.error} retry={() => void patients.refetch()}/>
      {patients.data && <Table><TableHeader><TableRow><TableHead>病歷號</TableHead><TableHead>姓名</TableHead><TableHead>操作</TableHead></TableRow></TableHeader><TableBody>{patients.data.map(patient => <TableRow key={patient.patientId}>
        <TableCell>{patient.patientId}</TableCell><TableCell>{patient.patientName}</TableCell><TableCell><div className="management-actions">
          <Button type="button" variant="outline" aria-label={`查看安排 ${patient.patientId}`} disabled={busy || !!deleting} onClick={() => {resetFeedback(); load.mutate({patientId: patient.patientId, edit: false})}}>查看安排</Button>
          <Button type="button" variant="outline" aria-label={`編輯 ${patient.patientId}`} disabled={busy || !!deleting} onClick={() => {resetFeedback(); load.mutate({patientId: patient.patientId, edit: true})}}>編輯</Button>
          <Button type="button" variant="outline" aria-label={`刪除 ${patient.patientId}`} disabled={busy || !!deleting} onClick={() => {setDeleting(patient); resetFeedback()}}>刪除</Button>
        </div></TableCell>
      </TableRow>)}</TableBody></Table>}
      {patients.data?.length === 0 && <p className="feedback">目前沒有受檢者資料</p>}
      {shownDetail && <section className="arrangement-detail" aria-label="受檢者健檢安排"><h3>{shownDetail.patientId} 的健檢安排</h3>
        <Table><TableHeader><TableRow><TableHead>健檢項目</TableHead><TableHead>站點</TableHead><TableHead>狀態</TableHead><TableHead>完成時間</TableHead><TableHead>移除限制</TableHead></TableRow></TableHeader><TableBody>
          {shownDetail.exams.map(exam => <TableRow key={exam.examId}><TableCell>{exam.examName}</TableCell><TableCell>{exam.stationName}</TableCell><TableCell>{exam.status === 'COMPLETED' ? '已完成' : '未完成'}</TableCell><TableCell>{exam.completedAt ?? '—'}</TableCell><TableCell>{exam.removalBlockReason ?? '可移除'}</TableCell></TableRow>)}
        </TableBody></Table>{shownDetail.exams.length === 0 && <p>尚未安排健檢項目</p>}
      </section>}
    </CardContent></Card>
  </section>
}
