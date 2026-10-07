import { Link, NavLink, Navigate, Route, Routes, useLocation } from 'react-router-dom'
import { HeartPulse, ScanLine, UserRound, ClipboardList, ArrowUpRight, ChevronRight, Sparkles, CalendarDays } from 'lucide-react'
import { StaffPage } from './pages/staff/StaffPage'
import { PatientPage } from './pages/patient/PatientPage'
import { AdminPage } from './pages/admin/AdminPage'
import { USE_MSW } from './config/runtime'
const roles = [
  {path: '/staff', label: '工作人員', detail: '現場檢查作業', icon: ScanLine},
  {path: '/patient', label: '受檢者', detail: '個人健檢進度', icon: UserRound},
  {path: '/admin', label: '管理後台', detail: '當日進度總覽', icon: ClipboardList},
]
export function App() {
  const location = useLocation()
  const role = roles.find(item => item.path === location.pathname)
  return <div className="app-shell">
    <aside className="sidebar">
      <Link className="brand" to="/staff"><span className="brand-mark"><HeartPulse/></span><div>健檢助手<small>HealthCheck Assistant</small></div></Link>
      <div className="nav-caption">工作空間 <span>WORKSPACE</span></div>
      <nav aria-label="角色頁面">{roles.map(({path, label, detail, icon: Icon}) => <NavLink key={path} to={path}><Icon/><span>{label}<small>{detail}</small></span><ChevronRight className="nav-arrow"/></NavLink>)}</nav>
      <div className="sidebar-note"><span className="note-icon"><HeartPulse/></span><h2>讓每一步，都更安心。</h2><p>從辨識、檢查到完成，<br/>清楚掌握健檢流程。</p><span className="note-line">HEALTH CHECK 2026 <ArrowUpRight/></span></div>
      <div className="sidebar-bottom"><span className="system-dot"/>Frontend Prototype <span>v1</span></div>
    </aside>
    <div className="workspace">
      <header className="topbar"><div className="breadcrumb"><span>健檢現場流程助手</span><ChevronRight/><strong>{role?.label ?? '首頁'}</strong></div><div className="topbar-meta"><span className="today"><CalendarDays/>{new Date().toLocaleDateString('zh-TW', {timeZone: 'Asia/Taipei', month: '2-digit', day: '2-digit'})}</span>{USE_MSW && <span className="demo-pill"><span/>DEMO 環境</span>}<span className="profile-mark" aria-hidden="true">HC</span></div></header>
      <main>
        <Routes><Route path="/" element={<Navigate to="/staff" replace/>}/><Route path="/staff" element={<StaffPage/>}/><Route path="/patient" element={<PatientPage/>}/><Route path="/admin" element={<AdminPage/>}/><Route path="*" element={<div className="page-intro"><h1>找不到此頁面</h1><NavLink to="/staff">返回工作人員頁面</NavLink></div>}/></Routes>
        {USE_MSW && <div className="demo-note"><Sparkles/><p>目前使用模擬資料，檢查時間固定 10:30。重新整理頁面會重設進度。</p></div>}
      </main>
      <footer><span>HealthCheck Assistant</span><span>清楚掌握每一步 · 2026</span></footer>
    </div>
  </div>
}

