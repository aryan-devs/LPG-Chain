import React from 'react';
import { Inbox, RefreshCw } from 'lucide-react';

interface AdminEmptyStateProps {
  icon?: React.ReactNode;
  title: string;
  description: string;
  actionLabel?: string;
  onAction?: () => void;
  isLoading?: boolean;
  secondaryAction?: React.ReactNode;
}

export const AdminEmptyState: React.FC<AdminEmptyStateProps> = ({
  icon,
  title,
  description,
  actionLabel,
  onAction,
  isLoading = false,
  secondaryAction,
}) => {
  return (
    <div className="flex flex-col items-center justify-center p-8 sm:p-12 bg-white rounded-2xl border border-slate-200/80 text-center gap-4 shadow-xs">
      <div className="w-12 h-12 rounded-2xl bg-slate-100 text-slate-500 flex items-center justify-center shadow-inner">
        {icon || <Inbox size={24} />}
      </div>
      <div className="max-w-md">
        <h3 className="text-base font-bold text-slate-900">{title}</h3>
        <p className="text-xs text-slate-500 mt-1 leading-relaxed">{description}</p>
      </div>

      <div className="flex items-center gap-3 mt-1">
        {actionLabel && onAction && (
          <button
            type="button"
            onClick={onAction}
            disabled={isLoading}
            className="flex items-center gap-2 px-4 py-2 rounded-xl text-xs font-bold bg-slate-900 hover:bg-slate-800 text-white transition-colors disabled:opacity-50"
          >
            <RefreshCw size={13} className={isLoading ? 'animate-spin' : ''} />
            <span>{actionLabel}</span>
          </button>
        )}
        {secondaryAction}
      </div>
    </div>
  );
};
