import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { BrowserRouter } from 'react-router-dom'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { App } from './App'
import { prepareApiMode } from './config/runtime'
import './index.css'
const client = new QueryClient({defaultOptions: {queries: {retry: 1, staleTime: 0}, mutations: {retry: false}}})
async function start() {
  await prepareApiMode()
  createRoot(document.getElementById('root')!).render(<StrictMode><QueryClientProvider client={client}><BrowserRouter><App/></BrowserRouter></QueryClientProvider></StrictMode>)
}
void start().catch(() => {
  const root = document.getElementById('root')!
  root.textContent = '無法啟動應用程式，請重新整理或聯絡工作人員。'
  root.setAttribute('role', 'alert')
})
