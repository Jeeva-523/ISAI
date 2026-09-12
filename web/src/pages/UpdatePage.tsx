import React, { useState, useEffect, useRef } from 'react'

export const UpdatePage: React.FC = () => {
  const [status, setStatus] = useState<'idle' | 'downloading' | 'completed' | 'error'>('downloading')
  const [progress, setProgress] = useState(0)
  const [downloadedMb, setDownloadedMb] = useState('0.0')
  const [totalMb, setTotalMb] = useState('19.5')
  const hasTriggeredRef = useRef(false)

  const triggerDownload = async () => {
    setStatus('downloading')
    setProgress(0)
    setDownloadedMb('0.0')

    try {
      const response = await fetch('/isai.dat', { cache: 'no-store' })
      if (!response.ok) throw new Error(`HTTP ${response.status}`)

      const contentLength = Number(response.headers.get('content-length')) || 19509267
      const totalInMb = (contentLength / (1024 * 1024)).toFixed(1)
      setTotalMb(totalInMb)

      const reader = response.body?.getReader()
      let receivedBytes = 0
      const chunks: BlobPart[] = []

      if (reader) {
        while (true) {
          const { done, value } = await reader.read()
          if (done) break
          if (value) {
            chunks.push(value)
            receivedBytes += value.length
            const pct = Math.min(Math.round((receivedBytes / contentLength) * 100), 100)
            setProgress(pct)
            setDownloadedMb((receivedBytes / (1024 * 1024)).toFixed(1))
          }
        }
      } else {
        const blob = await response.blob()
        chunks.push(new Uint8Array(await blob.arrayBuffer()))
      }

      const completeBlob = new Blob(chunks, { type: 'application/vnd.android.package-archive' })
      const blobUrl = URL.createObjectURL(completeBlob)
      const link = document.createElement('a')
      link.href = blobUrl
      link.download = 'isai.apk'
      document.body.appendChild(link)
      link.click()
      document.body.removeChild(link)
      setTimeout(() => URL.revokeObjectURL(blobUrl), 30000)

      setProgress(100)
      setDownloadedMb(totalInMb)
      setStatus('completed')
    } catch (err) {
      console.error('Download error:', err)
      setStatus('error')
    }
  }

  useEffect(() => {
    if (!hasTriggeredRef.current) {
      hasTriggeredRef.current = true
      triggerDownload()
    }
  }, [])

  return (
    <div
      style={{
        minHeight: '100vh',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        background: 'radial-gradient(circle at top, #1E1035 0%, #08080C 100%)',
        padding: '24px 16px',
        color: '#FFFFFF',
        fontFamily: "'Inter', system-ui, -apple-system, sans-serif"
      }}
    >
      <div
        style={{
          width: '100%',
          maxWidth: '480px',
          background: 'rgba(26, 21, 40, 0.92)',
          backdropFilter: 'blur(24px)',
          borderRadius: '24px',
          border: '1px solid rgba(139, 92, 246, 0.25)',
          padding: '32px 24px',
          boxShadow: '0 25px 60px rgba(0, 0, 0, 0.75)',
          textAlign: 'center'
        }}
      >
        {/* App Logo */}
        <div
          style={{
            width: '64px',
            height: '64px',
            margin: '0 auto 16px',
            borderRadius: '18px',
            background: 'linear-gradient(135deg, #8B5CF6 0%, #EC4899 100%)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            boxShadow: '0 8px 20px rgba(139, 92, 246, 0.4)',
            fontSize: '30px'
          }}
        >
          🎵
        </div>

        <h1
          style={{
            fontSize: '22px',
            fontWeight: 800,
            margin: '0 0 8px',
            background: 'linear-gradient(135deg, #FFFFFF 30%, #DDD6FE 100%)',
            WebkitBackgroundClip: 'text',
            WebkitTextFillColor: 'transparent'
          }}
        >
          {status === 'downloading'
            ? 'Downloading ISAI Update... 🚀'
            : status === 'completed'
            ? 'Update Download Complete! 🎉'
            : 'Download ISAI Update'}
        </h1>

        <p style={{ fontSize: '14px', color: '#9CA3AF', margin: '0 0 24px', lineHeight: '1.5' }}>
          {status === 'downloading'
            ? 'Downloading latest APK directly to your device:'
            : status === 'completed'
            ? 'APK download complete. Tap the file to install the update.'
            : 'Click below to download the latest ISAI APK:'}
        </p>

        {/* Progress Bar */}
        {status === 'downloading' && (
          <div
            style={{
              background: 'rgba(15, 12, 25, 0.85)',
              padding: '16px',
              borderRadius: '16px',
              border: '1px solid rgba(139, 92, 246, 0.35)',
              marginBottom: '24px'
            }}
          >
            <div
              style={{
                width: '100%',
                height: '10px',
                background: 'rgba(255, 255, 255, 0.1)',
                borderRadius: '6px',
                overflow: 'hidden',
                marginBottom: '10px'
              }}
            >
              <div
                style={{
                  width: `${Math.min(progress, 100)}%`,
                  height: '100%',
                  background: 'linear-gradient(90deg, #8B5CF6, #10B981)',
                  borderRadius: '6px',
                  transition: 'width 0.25s ease'
                }}
              />
            </div>
            <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '13px', color: '#A78BFA', fontWeight: 600 }}>
              <span>{downloadedMb} MB / {totalMb} MB</span>
              <span>{Math.min(progress, 100)}%</span>
            </div>
          </div>
        )}

        {/* Status Messages / Action Buttons */}
        {status === 'completed' ? (
          <div style={{ display: 'flex', flexDirection: 'column', gap: '12px', marginBottom: '24px' }}>
            <div
              style={{
                padding: '14px',
                borderRadius: '16px',
                background: 'rgba(16, 185, 129, 0.15)',
                border: '1px solid rgba(16, 185, 129, 0.4)',
                color: '#10B981',
                fontWeight: 600,
                fontSize: '14px'
              }}
            >
              ✅ isai.apk ready to install ({totalMb} MB)
            </div>
            <button
              onClick={triggerDownload}
              style={{
                padding: '12px 20px',
                borderRadius: '14px',
                background: 'rgba(139, 92, 246, 0.2)',
                border: '1px solid rgba(139, 92, 246, 0.4)',
                color: '#DDD6FE',
                fontWeight: 600,
                fontSize: '14px',
                cursor: 'pointer'
              }}
            >
              Download Again ⟳
            </button>
          </div>
        ) : status === 'error' ? (
          <div style={{ marginBottom: '24px' }}>
            <div
              style={{
                padding: '14px',
                borderRadius: '16px',
                background: 'rgba(239, 68, 68, 0.15)',
                border: '1px solid rgba(239, 68, 68, 0.4)',
                color: '#EF4444',
                fontWeight: 600,
                fontSize: '14px',
                marginBottom: '12px'
              }}
            >
              ❌ Download failed. Please try again.
            </div>
            <button
              onClick={triggerDownload}
              style={{
                width: '100%',
                padding: '14px',
                borderRadius: '14px',
                background: 'linear-gradient(135deg, #8B5CF6, #EC4899)',
                border: 'none',
                color: '#FFFFFF',
                fontWeight: 700,
                fontSize: '15px',
                cursor: 'pointer'
              }}
            >
              Retry Download ➔
            </button>
          </div>
        ) : null}

        <div>
          <a
            href="/"
            style={{ color: '#6B7280', fontSize: '13px', textDecoration: 'none' }}
          >
            ← Back to Web Player
          </a>
        </div>
      </div>
    </div>
  )
}
