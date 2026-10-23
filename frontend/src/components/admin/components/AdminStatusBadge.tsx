import React from 'react';
import {
  CheckCircle2,
  Clock,
  AlertTriangle,
  XCircle,
  Truck,
  ShieldCheck,
  ShieldAlert,
  Info,
} from 'lucide-react';

interface AdminStatusBadgeProps {
  status: string;
  size?: 'sm' | 'md';
  customLabel?: string;
}

export const AdminStatusBadge: React.FC<AdminStatusBadgeProps> = ({
  status,
  size = 'md',
  customLabel,
}) => {
  const norm = (status || '').toUpperCase().trim();

  let colorClasses = 'bg-slate-100 text-slate-700 border-slate-200';
  let icon = <Info size={12} />;
  let label = customLabel || status;

  switch (norm) {
    case 'ACTIVE':
    case 'VERIFIED':
    case 'DELIVERED':
    case 'SUCCESS':
    case 'COMPLETED':
    case 'ONLINE':
    case 'CONNECTED':
      colorClasses = 'bg-emerald-50 text-emerald-700 border-emerald-200/80';
      icon = <CheckCircle2 size={12} className="text-emerald-600" />;
      break;

    case 'PENDING':
    case 'PENDING_VERIFICATION':
    case 'ASSIGNED':
    case 'QUEUED':
    case 'STANDBY':
    case 'IN_PROGRESS':
      colorClasses = 'bg-amber-50 text-amber-700 border-amber-200/80';
      icon = <Clock size={12} className="text-amber-600" />;
      break;

    case 'OUT_FOR_DELIVERY':
    case 'IN_TRANSIT':
    case 'PICKED_UP':
    case 'ON_DELIVERY':
      colorClasses = 'bg-blue-50 text-blue-700 border-blue-200/80';
      icon = <Truck size={12} className="text-blue-600" />;
      break;

    case 'CONFIRMED':
      colorClasses = 'bg-indigo-50 text-indigo-700 border-indigo-200/80';
      icon = <ShieldCheck size={12} className="text-indigo-600" />;
      break;

    case 'SUSPENDED':
    case 'REJECTED':
    case 'CANCELLED':
    case 'FAILED':
    case 'CRITICAL':
    case 'DENIED':
    case 'DAMAGED':
    case 'DISCONNECTED':
      colorClasses = 'bg-rose-50 text-rose-700 border-rose-200/80';
      icon = <XCircle size={12} className="text-rose-600" />;
      break;

    case 'WARNING':
    case 'MAINTENANCE':
    case 'DISCREPANCY':
      colorClasses = 'bg-orange-50 text-orange-700 border-orange-200/80';
      icon = <AlertTriangle size={12} className="text-orange-600" />;
      break;

    case 'INACTIVE':
    case 'OFF_DUTY':
    case 'UNASSIGNED':
    case 'EXHAUSTED':
    case 'CLOSED':
      colorClasses = 'bg-slate-100 text-slate-600 border-slate-200';
      icon = <ShieldAlert size={12} className="text-slate-500" />;
      break;

    default:
      colorClasses = 'bg-slate-100 text-slate-700 border-slate-200';
      icon = <Info size={12} className="text-slate-500" />;
  }

  const sizeClasses =
    size === 'sm'
      ? 'px-2 py-0.5 text-[10px] gap-1'
      : 'px-2.5 py-1 text-xs gap-1.5 font-semibold';

  return (
    <span
      className={`inline-flex items-center rounded-full border font-medium uppercase tracking-wider ${sizeClasses} ${colorClasses}`}
    >
      {icon}
      <span>{label.replace(/_/g, ' ')}</span>
    </span>
  );
};
