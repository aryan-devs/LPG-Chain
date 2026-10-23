import React from 'react';
import { ArrowUpRight, ArrowDownRight, Minus } from 'lucide-react';

interface AdminKpiCardProps {
  title: string;
  value: string | number;
  subtitle?: string;
  icon: React.ReactNode;
  iconBgColor?: string;
  iconColor?: string;
  trend?: {
    value: string;
    isPositive?: boolean;
    isNeutral?: boolean;
  };
  isPlaceholder?: boolean;
  onClick?: () => void;
}

export const AdminKpiCard: React.FC<AdminKpiCardProps> = ({
  title,
  value,
  subtitle,
  icon,
  iconBgColor = 'bg-rose-50',
  iconColor = 'text-rose-600',
  trend,
  isPlaceholder = false,
  onClick,
}) => {
  return (
    <div
      onClick={onClick}
      className={`relative p-5 rounded-2xl bg-white border border-slate-200/80 shadow-xs hover:shadow-md transition-all duration-200 flex flex-col justify-between ${
        onClick ? 'cursor-pointer hover:border-slate-300' : ''
      }`}
    >
      {/* Top Header */}
      <div className="flex items-start justify-between gap-3">
        <div className="flex flex-col">
          <span className="text-xs font-semibold text-slate-500 uppercase tracking-wider">
            {title}
          </span>
          <div className="flex items-baseline gap-2 mt-1.5">
            <span className="text-2xl sm:text-3xl font-black text-slate-900 tracking-tight">
              {value}
            </span>
            {isPlaceholder && (
              <span className="text-[10px] font-medium text-slate-400 bg-slate-100 px-1.5 py-0.5 rounded">
                Preview
              </span>
            )}
          </div>
        </div>

        <div className={`w-11 h-11 rounded-xl ${iconBgColor} ${iconColor} flex items-center justify-center shrink-0 shadow-inner`}>
          {icon}
        </div>
      </div>

      {/* Bottom Subtitle / Trend */}
      {(subtitle || trend) && (
        <div className="mt-4 pt-3 border-t border-slate-100 flex items-center justify-between text-xs text-slate-500">
          {subtitle && <span>{subtitle}</span>}
          {trend && (
            <span
              className={`inline-flex items-center gap-0.5 font-semibold ${
                trend.isNeutral
                  ? 'text-slate-500'
                  : trend.isPositive
                  ? 'text-emerald-600'
                  : 'text-rose-600'
              }`}
            >
              {trend.isNeutral ? (
                <Minus size={13} />
              ) : trend.isPositive ? (
                <ArrowUpRight size={14} />
              ) : (
                <ArrowDownRight size={14} />
              )}
              {trend.value}
            </span>
          )}
        </div>
      )}
    </div>
  );
};
