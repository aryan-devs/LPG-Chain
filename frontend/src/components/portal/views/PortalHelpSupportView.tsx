import React, { useState } from 'react';
import {
  HelpCircle,
  AlertTriangle,
  PhoneCall,
  ChevronDown,
  MessageSquare,
  Send,
  Check,
} from 'lucide-react';
import { CustomerProfileData, SupportTicket } from '../portalTypes';
import { CustomerService } from '../../../services/customerApi';

interface PortalHelpSupportViewProps {
  profile: CustomerProfileData;
  tickets: SupportTicket[];
  onTicketCreated: (ticket: SupportTicket) => void;
}

export const PortalHelpSupportView: React.FC<PortalHelpSupportViewProps> = ({
  profile,
  tickets,
  onTicketCreated,
}) => {
  const [openFaqIndex, setOpenFaqIndex] = useState<number | null>(0);
  const [ticketSubject, setTicketSubject] = useState('');
  const [ticketCategory, setTicketCategory] = useState<SupportTicket['category']>('Delivery Issue');
  const [ticketDesc, setTicketDesc] = useState('');
  const [ticketSuccess, setTicketSuccess] = useState(false);

  const faqs = [
    {
      q: 'How do I verify if my delivered cylinder has the correct weight?',
      a: 'Every domestic cylinder contains exactly 14.2 kg of net LPG. Check the Tare Weight (empty cylinder weight) stamped on the cylinder collar or stay plate (e.g. 15.6 kg). Demand that the delivery person weighs the cylinder in your presence on their calibrated spring balance. The total weight must equal Tare Weight + 14.2 kg (allowable tolerance ±150g).',
    },
    {
      q: 'When will my PAHAL (DBTL) subsidy be credited?',
      a: 'The DBTL subsidy of ₹200.00 is automatically credited via the National Payments Corporation of India (NPCI) into your Aadhaar-linked primary bank account within 24 to 48 hours following successful physical delivery.',
    },
    {
      q: 'What should I do immediately if I suspect an LPG gas leak?',
      a: '1. Immediately extinguish all open flames and incense sticks. 2. Do NOT operate any electrical switches (neither turn on nor turn off). 3. Turn off the regulator switch on top of the cylinder. 4. Open all doors and windows. 5. Evacuate the kitchen and immediately call the 24x7 Emergency Helpline 1906.',
    },
    {
      q: 'How many subsidized cylinders am I entitled to in a year?',
      a: 'Under current petroleum ministry regulations, each household connection is entitled to 12 subsidized 14.2 kg domestic cylinders per financial year (April to March). Subsequent refills are provided at the prevailing domestic non-subsidized rate.',
    },
    {
      q: 'How can I change or update my delivery address?',
      a: 'Go to the "Saved Addresses" tab in your portal to add a new household location. If permanently relocating to a new distributor ward, submit an updated electricity bill or rent agreement in the "KYC & Documents" section for seamless transfer.',
    },
  ];

  const handleCreateTicket = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!ticketSubject.trim() || !ticketDesc.trim()) return;

    const newT = await CustomerService.createTicket({
      subject: ticketSubject.trim(),
      category: ticketCategory,
      description: ticketDesc.trim(),
    });

    onTicketCreated(newT);
    setTicketSubject('');
    setTicketDesc('');
    setTicketSuccess(true);
    setTimeout(() => setTicketSuccess(false), 3000);
  };

  return (
    <div className="max-w-4xl mx-auto flex flex-col gap-6 pb-12">
      {/* Page Title */}
      <div>
        <h1 className="text-2xl sm:text-3xl font-black text-slate-900 tracking-tight">
          Help &amp; Customer Support
        </h1>
        <p className="text-sm text-slate-500 mt-1">
          24/7 dedicated support desk, emergency safety response, and ticketing assistance.
        </p>
      </div>

      {/* 🚨 Emergency Helpline 1906 Banner */}
      <div className="bg-gradient-to-r from-rose-600 via-red-600 to-rose-700 rounded-3xl text-white shadow-lg shadow-rose-600/30 p-6 sm:p-7 flex flex-col md:flex-row items-start md:items-center justify-between gap-6">
        <div className="flex items-start gap-4">
          <div className="w-12 h-12 rounded-2xl bg-white/20 backdrop-blur-md flex items-center justify-center flex-shrink-0 text-white mt-1">
            <AlertTriangle size={26} />
          </div>
          <div>
            <div className="flex items-center gap-2">
              <span className="px-2.5 py-0.5 rounded-full bg-white text-rose-700 text-[10px] font-black uppercase tracking-wider">
                Emergency 24x7 Hotline
              </span>
            </div>
            <h2 className="text-xl sm:text-2xl font-black mt-1">
              Smell Gas? Call Helpline 1906
            </h2>
            <p className="text-xs text-rose-100 mt-0.5 leading-relaxed max-w-lg">
              Toll-free emergency response for gas leaks, regulator issues, and fire hazard prevention.
            </p>
          </div>
        </div>

        <a
          href="tel:1906"
          className="px-6 py-3 rounded-2xl bg-white text-rose-700 hover:bg-rose-50 font-black text-base shadow-md transition-all self-stretch md:self-auto text-center flex items-center justify-center gap-2 flex-shrink-0"
        >
          <PhoneCall size={20} />
          <span>Call 1906 Now</span>
        </a>
      </div>

      {/* Contact Channels Grid */}
      <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
        <div className="bg-white rounded-3xl border border-slate-200/90 shadow-sm p-5 flex flex-col gap-2">
          <span className="text-[10px] font-bold text-slate-400 uppercase tracking-wider">
            Household Care Desk
          </span>
          <span className="text-base font-black text-slate-900">1800 233 3555</span>
          <span className="text-xs text-slate-500">Toll-free 8:00 AM - 8:00 PM</span>
        </div>

        <div className="bg-white rounded-3xl border border-slate-200/90 shadow-sm p-5 flex flex-col gap-2">
          <span className="text-[10px] font-bold text-slate-400 uppercase tracking-wider">
            Your Distributor
          </span>
          <span className="text-base font-black text-slate-900">{profile.distributorPhone}</span>
          <span className="text-xs text-slate-500">{profile.distributorName}</span>
        </div>

        <div className="bg-white rounded-3xl border border-slate-200/90 shadow-sm p-5 flex flex-col gap-2">
          <span className="text-[10px] font-bold text-slate-400 uppercase tracking-wider">
            Official Email Support
          </span>
          <span className="text-base font-black text-slate-900 truncate">care@lpgchain.gov.in</span>
          <span className="text-xs text-slate-500">Response within 4 business hours</span>
        </div>
      </div>

      {/* Frequently Asked Questions */}
      <div className="bg-white rounded-3xl border border-slate-200/90 shadow-sm p-6 sm:p-7 flex flex-col gap-4">
        <h2 className="text-base font-bold text-slate-900 pb-2 border-b border-slate-100 flex items-center gap-2">
          <HelpCircle size={18} className="text-rose-600" />
          <span>Frequently Asked Questions</span>
        </h2>

        <div className="divide-y divide-slate-100">
          {faqs.map((faq, idx) => {
            const isOpen = openFaqIndex === idx;
            return (
              <div key={faq.q} className="py-3.5">
                <button
                  type="button"
                  onClick={() => setOpenFaqIndex(isOpen ? null : idx)}
                  className="w-full flex items-center justify-between text-left text-xs sm:text-sm font-bold text-slate-800 hover:text-rose-600 transition-colors"
                >
                  <span>{faq.q}</span>
                  <ChevronDown
                    size={16}
                    className={`text-slate-400 transition-transform ${
                      isOpen ? 'rotate-180 text-rose-600' : ''
                    }`}
                  />
                </button>
                {isOpen && (
                  <p className="mt-2.5 text-xs text-slate-600 leading-relaxed pl-2 border-l-2 border-rose-600">
                    {faq.a}
                  </p>
                )}
              </div>
            );
          })}
        </div>
      </div>

      {/* Raise a Support Ticket */}
      <div className="bg-white rounded-3xl border border-slate-200/90 shadow-sm p-6 sm:p-7 flex flex-col gap-5">
        <div className="flex items-center gap-3 pb-3 border-b border-slate-100">
          <div className="w-9 h-9 rounded-xl bg-rose-50 text-rose-600 flex items-center justify-center">
            <MessageSquare size={18} />
          </div>
          <div>
            <h2 className="text-base font-bold text-slate-900">Raise a Support Ticket</h2>
            <p className="text-xs text-slate-500">
              Our regional operations team will review your query and reply within 24 hours.
            </p>
          </div>
        </div>

        {ticketSuccess && (
          <div className="p-4 rounded-2xl bg-emerald-50 border border-emerald-200 text-emerald-800 text-xs font-bold flex items-center gap-2">
            <Check size={16} />
            <span>Support ticket submitted! Our executive will contact you shortly.</span>
          </div>
        )}

        <form onSubmit={handleCreateTicket} className="space-y-4">
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div>
              <label className="text-xs font-bold text-slate-700 block mb-1">Category</label>
              <select
                value={ticketCategory}
                onChange={(e) => setTicketCategory(e.target.value as any)}
                className="w-full p-2.5 rounded-xl border border-slate-300 text-xs text-slate-900 focus:outline-none focus:ring-2 focus:ring-rose-500"
              >
                <option value="Delivery Issue">Delivery Delay or Driver Issue</option>
                <option value="Billing & Subsidy">PAHAL DBTL Subsidy Discrepancy</option>
                <option value="Cylinder Quality">Defective Seal or Weight Issue</option>
                <option value="Gas Leak / Emergency">Gas Equipment Safety Query</option>
                <option value="General Query">General Customer Query</option>
              </select>
            </div>

            <div>
              <label className="text-xs font-bold text-slate-700 block mb-1">Subject</label>
              <input
                type="text"
                value={ticketSubject}
                onChange={(e) => setTicketSubject(e.target.value)}
                placeholder="Brief summary of your issue..."
                className="w-full p-2.5 rounded-xl border border-slate-300 text-xs text-slate-900 focus:outline-none focus:ring-2 focus:ring-rose-500"
                required
              />
            </div>
          </div>

          <div>
            <label className="text-xs font-bold text-slate-700 block mb-1">
              Detailed Description
            </label>
            <textarea
              rows={3}
              value={ticketDesc}
              onChange={(e) => setTicketDesc(e.target.value)}
              placeholder="Provide details such as Booking ID, timestamp, or specific concern..."
              className="w-full p-2.5 rounded-xl border border-slate-300 text-xs text-slate-900 focus:outline-none focus:ring-2 focus:ring-rose-500"
              required
            />
          </div>

          <div className="flex justify-end">
            <button
              type="submit"
              className="px-6 py-2.5 rounded-xl bg-rose-600 hover:bg-rose-500 text-white font-bold text-xs shadow-md shadow-rose-600/25 transition-all flex items-center gap-2"
            >
              <Send size={14} />
              <span>Submit Ticket</span>
            </button>
          </div>
        </form>

        {/* Existing User Tickets */}
        {tickets.length > 0 && (
          <div className="pt-4 border-t border-slate-100 flex flex-col gap-2">
            <span className="text-xs font-bold text-slate-700">Recent Support Tickets</span>
            {tickets.map((t) => (
              <div
                key={t.ticketId}
                className="p-3 rounded-2xl bg-slate-50 border border-slate-100 flex items-center justify-between text-xs"
              >
                <div>
                  <div className="flex items-center gap-2">
                    <span className="font-mono font-bold text-slate-900">{t.ticketId}</span>
                    <span className="px-2 py-0.5 rounded-full bg-slate-200 text-slate-700 text-[10px] font-bold">
                      {t.category}
                    </span>
                  </div>
                  <p className="text-slate-700 mt-1 font-medium">{t.subject}</p>
                </div>
                <div className="text-right">
                  <span
                    className={`px-2 py-0.5 rounded-full text-[10px] font-bold ${
                      t.status === 'RESOLVED'
                        ? 'bg-emerald-100 text-emerald-800'
                        : 'bg-amber-100 text-amber-800'
                    }`}
                  >
                    {t.status}
                  </span>
                  <span className="text-[10px] text-slate-400 block mt-1">{t.createdDate}</span>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
};
