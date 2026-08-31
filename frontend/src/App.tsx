import { useEffect, useState } from 'react'
import { Activity, CheckCircle, XCircle, Loader } from 'lucide-react'

// ----------------------------------------------------------------
// Types
// ----------------------------------------------------------------

type BackendStatus = 'loading' | 'up' | 'down'

interface HealthResponse {
  status: string
  service: string
}

// ----------------------------------------------------------------
// App — Phase 1: Application Shell
// Displays the app title and polls the backend health endpoint.
// ----------------------------------------------------------------

function App() {
  const [backendStatus, setBackendStatus] = useState<BackendStatus>('loading')
  const [healthData, setHealthData] = useState<HealthResponse | null>(null)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    const checkHealth = async () => {
      try {
        setBackendStatus('loading')
        const response = await fetch('/api/health')
        if (!response.ok) throw new Error(`HTTP ${response.status}`)
        const data: HealthResponse = await response.json()
        setHealthData(data)
        setBackendStatus('up')
        setError(null)
      } catch (err) {
        setBackendStatus('down')
        setError('Could not reach the backend. Make sure it is running on port 8080.')
      }
    }

    checkHealth()
  }, [])

  return (
    <div className="min-h-screen bg-gray-950 text-white flex flex-col items-center justify-center gap-8 px-4">

      {/* ── Title ── */}
      <div className="text-center">
        <h1 className="text-4xl font-bold tracking-tight text-white">
          LPG-Nexus <span className="text-blue-400">AI</span>
        </h1>
        <p className="mt-2 text-gray-400 text-sm">
          Phase 1 — Project Foundation
        </p>
      </div>

      {/* ── Backend Status Card ── */}
      <div className="bg-gray-900 border border-gray-800 rounded-2xl p-6 w-full max-w-sm shadow-lg">
        <div className="flex items-center gap-2 mb-4 text-gray-400 text-xs uppercase tracking-widest font-semibold">
          <Activity size={14} />
          Backend Status
        </div>

        {backendStatus === 'loading' && (
          <div className="flex items-center gap-3 text-yellow-400">
            <Loader size={20} className="animate-spin" />
            <span className="text-sm">Checking connection…</span>
          </div>
        )}

        {backendStatus === 'up' && healthData && (
          <div className="flex flex-col gap-2">
            <div className="flex items-center gap-3 text-emerald-400">
              <CheckCircle size={20} />
              <span className="text-sm font-medium">{healthData.status}</span>
            </div>
            <p className="text-gray-400 text-xs ml-8">{healthData.service}</p>
          </div>
        )}

        {backendStatus === 'down' && (
          <div className="flex flex-col gap-2">
            <div className="flex items-center gap-3 text-red-400">
              <XCircle size={20} />
              <span className="text-sm font-medium">Unreachable</span>
            </div>
            {error && (
              <p className="text-gray-500 text-xs ml-8">{error}</p>
            )}
          </div>
        )}
      </div>

      {/* ── Footer ── */}
      <p className="text-gray-700 text-xs">
        LPG-Nexus AI &mdash; Phase 1 Foundation &copy; 2024
      </p>
    </div>
  )
}

export default App
