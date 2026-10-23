import React, { useState } from 'react';
import {
  ShieldCheck,
  FileText,
  Eye,
  X,
  Printer,
} from 'lucide-react';
import { BillingInvoice, CustomerProfileData } from '../portalTypes';

interface PortalBillingPaymentsViewProps {
  profile: CustomerProfileData;
  invoices: BillingInvoice[];
}

export const PortalBillingPaymentsView: React.FC<PortalBillingPaymentsViewProps> = ({
  profile,
  invoices,
}) => {
  const [selectedInvoice, setSelectedInvoice] = useState<BillingInvoice | null>(null);

  return (
    <div className="max-w-5xl mx-auto flex flex-col gap-6 pb-12">
      {/* Header */}
      <div>
        <h1 className="text-2xl sm:text-3xl font-black text-slate-900 tracking-tight">
          Billing &amp; Payments
        </h1>
        <p className="text-sm text-slate-500 mt-1">
          Review tax receipts, DBTL direct subsidy credits, and download GST compliant invoices.
        </p>
      </div>

      {/* DBTL Direct Subsidy Summary Card */}
      <div className="bg-white rounded-3xl border border-slate-200/90 shadow-sm p-6 sm:p-7 flex flex-col md:flex-row items-start md:items-center justify-between gap-6">
        <div className="flex items-center gap-4">
          <div className="w-14 h-14 rounded-2xl bg-emerald-50 text-emerald-600 flex items-center justify-center flex-shrink-0">
            <ShieldCheck size={32} />
          </div>
          <div>
            <div className="flex items-center gap-2">
              <span className="text-xs font-bold text-slate-900">
                PAHAL (DBTL) Direct Subsidy Transfer
              </span>
              <span className="px-2 py-0.5 rounded-full bg-emerald-100 text-emerald-800 text-[10px] font-bold">
                Active &bull; Aadhaar Linked
              </span>
            </div>
            <p className="text-sm text-slate-600 mt-1">
              Subsidy of ₹200.00 is credited automatically to your primary bank account within 24-48 hours.
            </p>
            <span className="text-xs font-mono text-slate-400 mt-1 block">
              Mandate: HDFC Bank A/C ending in 7891 &bull; NPCI Aadhaar Mapper Verified
            </span>
          </div>
        </div>

        <div className="bg-emerald-50/80 border border-emerald-200/80 rounded-2xl px-5 py-3 text-right self-stretch md:self-auto">
          <span className="text-[10px] font-bold text-emerald-700 uppercase tracking-wider block">
            Annual Subsidy Received
          </span>
          <span className="text-xl font-black text-emerald-900">₹600.00</span>
          <span className="text-[10px] text-emerald-600 block">3 refills credited</span>
        </div>
      </div>

      {/* Invoices List */}
      <div className="bg-white rounded-3xl border border-slate-200/90 shadow-sm overflow-hidden flex flex-col">
        <div className="p-5 sm:px-6 border-b border-slate-100 flex items-center justify-between">
          <h2 className="text-base font-bold text-slate-900">Digital Tax Invoices</h2>
          <span className="text-xs text-slate-400">{invoices.length} Invoices Available</span>
        </div>

        <div className="overflow-x-auto">
          <table className="w-full text-left border-collapse">
            <thead>
              <tr className="bg-slate-50/70 border-b border-slate-100 text-[11px] font-bold text-slate-400 uppercase tracking-wider">
                <th className="py-3.5 px-6">Invoice #</th>
                <th className="py-3.5 px-4">Date</th>
                <th className="py-3.5 px-4">Refill Type</th>
                <th className="py-3.5 px-4">Gross Amount</th>
                <th className="py-3.5 px-4">Subsidy Benefit</th>
                <th className="py-3.5 px-4">Payment Method</th>
                <th className="py-3.5 px-6 text-right">Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100 text-xs text-slate-700">
              {invoices.map((inv) => (
                <tr key={inv.invoiceNumber} className="hover:bg-slate-50/60 transition-colors">
                  <td className="py-4 px-6 font-mono font-bold text-slate-900">
                    {inv.invoiceNumber}
                  </td>
                  <td className="py-4 px-4 text-slate-500">{inv.date}</td>
                  <td className="py-4 px-4 font-medium text-slate-800">{inv.cylinderType}</td>
                  <td className="py-4 px-4 font-bold text-slate-900">₹{inv.amount}.00</td>
                  <td className="py-4 px-4 font-bold text-emerald-600">
                    {inv.subsidyAmount > 0 ? `-₹${inv.subsidyAmount}.00` : 'N/A'}
                  </td>
                  <td className="py-4 px-4 text-slate-500">{inv.paymentMethod}</td>
                  <td className="py-4 px-6 text-right">
                    <button
                      type="button"
                      onClick={() => setSelectedInvoice(inv)}
                      className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-xl border border-slate-200 hover:border-rose-400 hover:bg-rose-50 text-slate-700 hover:text-rose-600 font-bold text-xs transition-colors"
                    >
                      <Eye size={13} />
                      <span>View Receipt</span>
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>

      {/* Invoice Modal Preview */}
      {selectedInvoice && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-900/40 backdrop-blur-sm animate-in fade-in">
          <div className="relative w-full max-w-xl bg-white rounded-3xl shadow-2xl border border-slate-200 p-6 sm:p-8">
            <div className="flex items-center justify-between pb-4 border-b border-slate-100">
              <div className="flex items-center gap-2">
                <FileText size={18} className="text-rose-600" />
                <h3 className="text-base font-bold text-slate-900">
                  LPG Refill Cash Memo #{selectedInvoice.invoiceNumber}
                </h3>
              </div>
              <button
                type="button"
                onClick={() => setSelectedInvoice(null)}
                className="p-1 rounded-full text-slate-400 hover:text-slate-600"
              >
                <X size={18} />
              </button>
            </div>

            <div className="py-6 flex flex-col gap-4 text-xs">
              <div className="flex justify-between text-slate-500">
                <span>Distributor: {profile.distributorName}</span>
                <span>Date: {selectedInvoice.date}</span>
              </div>
              <div className="flex justify-between text-slate-500">
                <span>Consumer: {profile.name}</span>
                <span className="font-mono">ID: {profile.consumerNumber}</span>
              </div>

              <div className="border-t border-b border-slate-100 py-3 flex flex-col gap-2">
                <div className="flex justify-between font-bold text-slate-800">
                  <span>{selectedInvoice.cylinderType}</span>
                  <span>₹{selectedInvoice.amount}.00</span>
                </div>
                <div className="flex justify-between text-slate-500 text-[11px]">
                  <span>CGST 2.5% + SGST 2.5%</span>
                  <span>Included</span>
                </div>
                {selectedInvoice.subsidyAmount > 0 && (
                  <div className="flex justify-between text-emerald-700 font-semibold bg-emerald-50 p-2 rounded-xl">
                    <span>PAHAL DBTL Subsidy Transfer</span>
                    <span>-₹{selectedInvoice.subsidyAmount}.00</span>
                  </div>
                )}
              </div>

              <div className="flex justify-between items-baseline pt-2">
                <span className="text-sm font-bold text-slate-800">Net Household Outflow:</span>
                <span className="text-xl font-black text-rose-600">
                  ₹{selectedInvoice.amount - selectedInvoice.subsidyAmount}.00
                </span>
              </div>
            </div>

            <div className="pt-4 border-t border-slate-100 flex items-center justify-end gap-3">
              <button
                type="button"
                onClick={() => window.print()}
                className="px-4 py-2 rounded-xl border border-slate-200 text-xs font-bold text-slate-700 flex items-center gap-1.5 hover:bg-slate-50"
              >
                <Printer size={14} />
                <span>Print</span>
              </button>
              <button
                type="button"
                onClick={() => setSelectedInvoice(null)}
                className="px-5 py-2 rounded-xl bg-rose-600 text-white text-xs font-bold hover:bg-rose-500"
              >
                Done
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
