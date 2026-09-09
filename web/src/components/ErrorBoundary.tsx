import { Component, ErrorInfo, ReactNode } from 'react'

interface Props {
  children: ReactNode
  fallback?: ReactNode
}

interface State {
  hasError: boolean
  error: Error | null
  showDetails: boolean
}

export class ErrorBoundary extends Component<Props, State> {
  public state: State = {
    hasError: false,
    error: null,
    showDetails: false
  }

  public static getDerivedStateFromError(error: Error): Partial<State> {
    return { hasError: true, error }
  }

  public componentDidCatch(error: Error, errorInfo: ErrorInfo) {
    console.error('[ISAI ErrorBoundary] Caught error:', error, errorInfo)
    try {
      (window as any).__LAST_REACT_ERROR__ = {
        message: error?.message,
        stack: error?.stack,
        info: errorInfo?.componentStack
      }
    } catch {
      // ignore
    }
  }

  private handleReset = () => {
    this.setState({ hasError: false, error: null, showDetails: false })
    window.location.reload()
  }

  private handleDismiss = () => {
    this.setState({ hasError: false, error: null, showDetails: false })
  }

  public render() {
    if (this.state.hasError) {
      if (this.props.fallback !== undefined) {
        return this.props.fallback
      }

      return (
        <div style={{
          minHeight: '100vh',
          backgroundColor: '#0B0B0F',
          color: '#FFFFFF',
          display: 'flex',
          flexDirection: 'column',
          alignItems: 'center',
          justifyContent: 'center',
          padding: '24px',
          fontFamily: 'system-ui, -apple-system, sans-serif'
        }}>
          <div style={{
            background: '#16161F',
            border: '1px solid rgba(139, 92, 246, 0.3)',
            borderRadius: '20px',
            padding: '32px',
            maxWidth: '520px',
            width: '100%',
            textAlign: 'center',
            boxShadow: '0 20px 40px rgba(0,0,0,0.6)'
          }}>
            <div style={{ fontSize: '48px', marginBottom: '16px' }}>🎵</div>
            <h2 style={{ fontSize: '20px', fontWeight: 800, marginBottom: '8px', color: '#8B5CF6' }}>
              ISAI HUB
            </h2>
            <p style={{ fontSize: '14px', color: '#A5A5AE', marginBottom: '20px', lineHeight: 1.5 }}>
              A temporary display error occurred while updating playback.
            </p>

            {this.state.error && (
              <div style={{
                textAlign: 'left',
                background: 'rgba(0,0,0,0.4)',
                border: '1px solid rgba(239, 68, 68, 0.3)',
                padding: '12px 16px',
                borderRadius: '10px',
                color: '#f87171',
                fontSize: '12px',
                marginBottom: '20px',
                maxHeight: '140px',
                overflowY: 'auto',
                fontFamily: 'monospace',
                wordBreak: 'break-all'
              }}>
                <strong>Error:</strong> {this.state.error.message}
              </div>
            )}

            <div style={{ display: 'flex', gap: '12px', justifyContent: 'center' }}>
              <button
                onClick={this.handleDismiss}
                style={{
                  background: 'rgba(255, 255, 255, 0.1)',
                  color: '#FFFFFF',
                  border: '1px solid rgba(255, 255, 255, 0.2)',
                  padding: '10px 20px',
                  borderRadius: '24px',
                  fontSize: '13px',
                  fontWeight: 600,
                  cursor: 'pointer'
                }}
              >
                Continue
              </button>
              <button
                onClick={this.handleReset}
                style={{
                  background: 'linear-gradient(135deg, #8B5CF6 0%, #EC4899 100%)',
                  color: '#FFFFFF',
                  border: 'none',
                  padding: '10px 24px',
                  borderRadius: '24px',
                  fontSize: '13px',
                  fontWeight: 700,
                  cursor: 'pointer'
                }}
              >
                Reload ISAI HUB
              </button>
            </div>
          </div>
        </div>
      )
    }

    return this.props.children
  }
}
