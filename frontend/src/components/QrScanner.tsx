import { useEffect, useRef, useState } from 'react'
import QrScanner from 'qr-scanner'
import { Camera, Square } from 'lucide-react'
import { Button } from './ui/button'

export function QrScannerInput({label, onScan, disabled}: {label: string; onScan: (value: string) => void; disabled?: boolean}) {
  const [active, setActive] = useState(false)
  const [error, setError] = useState('')
  const video = useRef<HTMLVideoElement>(null)
  const callback = useRef(onScan)
  useEffect(() => { callback.current = onScan }, [onScan])
  useEffect(() => { if (disabled) setActive(false) }, [disabled])
  useEffect(() => {
    if (!active || !video.current) return
    let disposed = false
    const scanner = new QrScanner(video.current, result => {
      if (disposed) return
      const value = result.data.trim().toUpperCase()
      const pattern = label === '受檢者' ? /^P\d{3}$/ : /^S\d{3}$/
      if (!pattern.test(value)) { setError(`QR 內容不是有效的${label}代碼，請使用手動輸入。`); setActive(false); return }
      callback.current(value); setActive(false)
    }, {preferredCamera: 'environment', highlightScanRegion: true, returnDetailedScanResult: true})
    void (async () => {
      try {
        if (!navigator.mediaDevices?.getUserMedia) throw new Error('相機需要 localhost 或 HTTPS 環境')
        if (!await QrScanner.hasCamera()) throw new Error('未偵測到相機')
        if (disposed) return
        await scanner.start()
        if (disposed) scanner.destroy()
      } catch (cause) {
        if (disposed) return
        const name = cause instanceof DOMException ? cause.name : ''
        setError(name === 'NotAllowedError' ? '相機權限遭拒，請允許相機或手動輸入。' : name === 'NotFoundError' ? '未偵測到相機，請手動輸入。' : `${cause instanceof Error ? cause.message : '無法開啟相機'}；請手動輸入。`)
        setActive(false)
      }
    })()
    return () => { disposed = true; scanner.destroy() }
  }, [active, label])
  return <div className="scanner"><Button type="button" variant="outline" disabled={disabled} onClick={() => {setError(''); setActive(value => !value)}}>{active ? <Square/> : <Camera/>}{active ? '停止相機' : `掃描${label} QR`}</Button>{active && <video ref={video} muted playsInline aria-label={`${label}掃描畫面`}/>} {error && <p role="alert" className="scanner-error">{error}</p>}</div>
}
