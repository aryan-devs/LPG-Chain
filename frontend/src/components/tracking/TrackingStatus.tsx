import React from 'react';
import { ConnectionStatus } from '../../types/tracking';
import { Radio, RefreshCw, AlertCircle, WifiOff } from 'lucide-react';

interface TrackingStatusProps {
  status: ConnectionStatus;
  lastUpdated?: string;
  onReconnect?: () => void;
}

export const TrackingStatus: React.FC<TrackingStatusProps> = ({
  status,
  lastUpdated,
  onReconnect,
}) => {
  const getStatusBadge = () => {
    switch (status) {
      case 'CONNECTED':
        return {
          label: 'Live Connected',
          badgeClass: 'bg-emerald-50 text-emerald-700 border-emerald-200/90 shadow-xs',
          dotClass: 'bg-emerald-500 animate-pulse',
          icon: <Radio size={13} className="text-emerald-600 animate-pulse" />,
        };
      case 'CONNECTING':
        return {
          label: 'Connecting...',
          badgeClass: 'bg-amber-50 text-amber-700 border-amber-200/90 shadow-xs',
          dotClass: 'bg-amber-500 animate-ping',
          icon: <RefreshCw size={13} className="text-amber-600 animate-spin" />,
        };
      case 'RECONNECTING':
        return {
          label: 'Reconnecting...',
          badgeClass: 'bg-amber-50 text-amber-700 border-amber-200/90 shadow-xs',
          dotClass: 'bg-amber-500 animate-ping',
          icon: <RefreshCw size={13} className="text-amber-600 animate-spin" />,
        };
      case 'ERROR':
        return {
          label: 'Connection Offline',
          badgeClass: 'bg-rose-50 text-rose-700 border-rose-200/90 shadow-xs',
          dotClass: 'bg-rose-500',
          icon: <AlertCircle size={13} className="text-rose-600" />,
        };
      case 'DISCONNECTED':
      default:
        return {
          label: 'Standby',
          badgeClass: 'bg-slate-100 text-slate-700 border-slate-200/90 shadow-xs',
          dotClass: 'bg-slate-400',
          icon: <WifiOff size={13} className="text-slate-500" />,
        };
    }
  };

  const current = getStatusBadge();

  // Formats ISO timestamp or time string nicely
  const formatTime = (ts?: string) => {
    if (!ts) return 'Standby';
    try {
      const date = new Date(ts);
      if (isNaN(date.getTime())) {
        return ts;
      }
      return date.toLocaleTimeString([], { hour12: false, hour: '2-digit', minute: '2-digit', second: '2-digit' });
    } catch {
      return ts;
    }
  };

  return (
    <div className="flex flex-wrap items-center justify-between gap-3 text-sm">
      <div className="flex items-center gap-2">
        <span className="text-[11px] uppercase tracking-wider font-bold text-slate-400">
          Feed:
        </span>
        <div
          className={`inline-flex items-center gap-1.5 px-3 py-1 rounded-full border text-xs font-bold tracking-tight ${current.badgeClass}`}
        >
          <span className={`w-1.5 h-1.5 rounded-full ${current.dotClass}`} />
          {current.icon}
          <span>{current.label}</span>
        </div>

        {(status === 'DISCONNECTED' || status === 'ERROR') && onReconnect && (
          <button
            type="button"
            onClick={onReconnect}
            className="inline-flex items-center gap-1.5 px-2.5 py-1 text-xs font-semibold text-slate-700 hover:text-rose-600 bg-white hover:bg-rose-50/50 border border-slate-200 hover:border-rose-200 rounded-lg shadow-xs transition-colors"
          >
            <RefreshCw size={11} />
            <span>Reconnect</span>
          </button>
        )}
      </div>

      {lastUpdated && (
        <div className="text-xs text-slate-500 flex items-center gap-1">
          <span>Last ping:</span>
          <span className="font-mono text-slate-800 font-bold">{formatTime(lastUpdated)}</span>
        </div>
      )}
    </div>
  );
};
