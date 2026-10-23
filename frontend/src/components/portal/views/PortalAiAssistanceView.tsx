import React, { useState, useEffect, useRef } from 'react';
import {
  Sparkles,
  Send,
  AlertTriangle,
  User,
  PhoneCall,
  ArrowRight,
  ShieldAlert,
} from 'lucide-react';
import { CustomerProfileData, PortalTab } from '../portalTypes';
import { CustomerService } from '../../../services/customerApi';
import {
  FaqIntentId,
  CORE_FAQ_SUGGESTIONS,
  matchTypedFaqIntent,
  getFaqAnswerForIntent,
} from '../../../services/faqService';

interface PortalAiAssistanceViewProps {
  profile: CustomerProfileData;
  initialPrompt?: string;
  onNavigateTab: (tab: PortalTab) => void;
}

interface ChatMessage {
  id: string;
  sender: 'user' | 'assistant';
  text: string;
  timestamp: string;
  suggestions?: string[];
  actionLink?: {
    tab: PortalTab;
    label: string;
  };
  supportPhone?: string;
  isError?: boolean;
}

export const PortalAiAssistanceView: React.FC<PortalAiAssistanceViewProps> = ({
  profile,
  initialPrompt,
  onNavigateTab,
}) => {
  const [messages, setMessages] = useState<ChatMessage[]>([
    {
      id: 'welcome',
      sender: 'assistant',
      text: `Hello ${profile.name.split(' ')[0]}! I'm your LPG-Chain Household Assistant. How can I help you today? You can select any topic below or type your own question.`,
      timestamp: 'Just now',
      suggestions: CORE_FAQ_SUGGESTIONS.map((s) => s.label),
    },
  ]);
  const [inputValue, setInputValue] = useState('');
  const [isTyping, setIsTyping] = useState(false);
  const messagesEndRef = useRef<HTMLDivElement>(null);

  const scrollToBottom = () => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  };

  useEffect(() => {
    scrollToBottom();
  }, [messages, isTyping]);

  /**
   * Processes user query with intent resolution.
   * If explicitIntentId is provided (e.g. from clicking a suggested question),
   * it uses the exact intent directly without fragile string matching.
   */
  const handleProcessQuery = async (queryText: string, explicitIntentId?: FaqIntentId) => {
    if (!queryText.trim()) return;

    const userMsg: ChatMessage = {
      id: `usr-${Date.now()}`,
      sender: 'user',
      text: queryText.trim(),
      timestamp: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
    };

    setMessages((prev) => [...prev, userMsg]);
    setInputValue('');
    setIsTyping(true);

    // Resolve intent: either explicitly provided or matched via normalizer
    const resolvedIntent = explicitIntentId || matchTypedFaqIntent(queryText.trim());

    // Verified local answer (guarantees every suggested question and valid typed variation works reliably)
    const localFaqResult = getFaqAnswerForIntent(resolvedIntent, profile);

    let botReply = localFaqResult.reply;
    let botActionTab = localFaqResult.actionTab;
    let botActionLabel = localFaqResult.actionLabel;
    let botPhone = localFaqResult.supportPhone;
    let botSuggestions = localFaqResult.suggestions;

    // Optional backend sync for live database account data
    try {
      const backendResponse = await CustomerService.chatAiAssistant(
        queryText.trim(),
        resolvedIntent !== 'AMBIGUOUS' && resolvedIntent !== 'OUT_OF_SCOPE' ? resolvedIntent : undefined
      );

      if (backendResponse && backendResponse.reply) {
        // If backend returned account-specific verified data, enrich response
        if (
          backendResponse.toolUsed?.startsWith('getMy') ||
          backendResponse.toolUsed === 'unauthenticatedAccountQuery'
        ) {
          botReply = backendResponse.reply;
        } else if (resolvedIntent !== 'OUT_OF_SCOPE') {
          botReply = backendResponse.reply;
        }
        if (backendResponse.actionTab) botActionTab = backendResponse.actionTab as PortalTab;
        if (backendResponse.actionLabel) botActionLabel = backendResponse.actionLabel;
        if (backendResponse.supportPhone) botPhone = backendResponse.supportPhone;
        if (backendResponse.suggestions && backendResponse.suggestions.length > 0) {
          botSuggestions = backendResponse.suggestions;
        }
      }
    } catch {
      // Backend is unavailable, unauthenticated, or returned error.
      // localFaqResult already contains the verified answer for all supported topics.
    }

    const botMsg: ChatMessage = {
      id: `bot-${Date.now()}`,
      sender: 'assistant',
      text: botReply,
      timestamp: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
      suggestions: botSuggestions,
      actionLink: botActionTab
        ? {
            tab: botActionTab,
            label: botActionLabel || 'View Details',
          }
        : undefined,
      supportPhone: botPhone,
    };

    setIsTyping(false);
    setMessages((prev) => [...prev, botMsg]);
  };

  // Handle pre-filled initial prompt on mount
  useEffect(() => {
    if (initialPrompt && initialPrompt.trim().length > 0) {
      handleProcessQuery(initialPrompt);
    }
  }, [initialPrompt]);

  return (
    <div className="max-w-4xl mx-auto flex flex-col h-[750px] bg-white rounded-3xl border border-slate-200/90 shadow-sm overflow-hidden pb-4">
      {/* AI Header */}
      <div className="px-6 py-4 bg-gradient-to-r from-rose-50 to-orange-50 border-b border-orange-100 flex items-center justify-between">
        <div className="flex items-center gap-3">
          <div className="w-10 h-10 rounded-2xl bg-rose-600 text-white flex items-center justify-center shadow-md shadow-rose-600/25">
            <Sparkles size={20} />
          </div>
          <div>
            <div className="flex items-center gap-2">
              <h2 className="text-base font-bold text-slate-900">LPG-Chain AI Assistant</h2>
              <span className="px-2 py-0.5 rounded-full bg-emerald-100 text-emerald-800 text-[10px] font-bold">
                Online &bull; Support &amp; FAQ
              </span>
            </div>
            <p className="text-xs text-slate-500">
              Clear answers for bookings, live tracking, subsidies, KYC, complaints &amp; safety.
            </p>
          </div>
        </div>

        {/* Emergency quick button */}
        <button
          type="button"
          onClick={() => handleProcessQuery('LPG safety guidelines and emergency guidance', 'SAFETY_GUIDELINES')}
          className="hidden sm:flex items-center gap-1.5 px-3 py-1.5 rounded-full bg-rose-600/10 border border-rose-300 text-rose-700 text-xs font-bold hover:bg-rose-600 hover:text-white transition-colors"
          title="Instant 1906 Emergency Gas Leak Guidance"
        >
          <AlertTriangle size={14} />
          <span>Emergency 1906 Protocol</span>
        </button>
      </div>

      {/* Messages Scroll Area */}
      <div className="flex-1 overflow-y-auto p-4 sm:p-6 space-y-4">
        {messages.map((msg) => (
          <div
            key={msg.id}
            className={`flex items-start gap-3 ${
              msg.sender === 'user' ? 'flex-row-reverse' : 'flex-row'
            }`}
          >
            {/* Avatar */}
            <div
              className={`w-8 h-8 rounded-full flex items-center justify-center text-xs font-bold flex-shrink-0 ${
                msg.sender === 'user'
                  ? 'bg-slate-700 text-white'
                  : msg.isError
                  ? 'bg-amber-600 text-white'
                  : 'bg-rose-600 text-white shadow-sm'
              }`}
            >
              {msg.sender === 'user' ? (
                <User size={14} />
              ) : msg.isError ? (
                <ShieldAlert size={14} />
              ) : (
                <Sparkles size={14} />
              )}
            </div>

            {/* Bubble */}
            <div
              className={`max-w-[85%] sm:max-w-[80%] rounded-2xl p-4 shadow-sm text-xs sm:text-sm leading-relaxed whitespace-pre-line ${
                msg.sender === 'user'
                  ? 'bg-rose-600 text-white rounded-tr-none'
                  : 'bg-slate-50 border border-slate-200 text-slate-800 rounded-tl-none'
              }`}
            >
              <p>{msg.text}</p>

              {/* Clickable tel: Support Phone Button if provided */}
              {msg.supportPhone && (
                <div className="mt-3 pt-2.5 border-t border-slate-200/80 flex items-center gap-2">
                  <a
                    href={`tel:${msg.supportPhone.replace(/[^0-9+]/g, '')}`}
                    className="inline-flex items-center gap-1.5 px-3.5 py-1.5 rounded-xl bg-emerald-600 hover:bg-emerald-700 text-white font-bold text-xs shadow-sm transition-colors"
                    title={`Call LPG-Chain Customer Support at ${msg.supportPhone}`}
                  >
                    <PhoneCall size={13} />
                    <span>Call Support: {msg.supportPhone}</span>
                  </a>
                </div>
              )}

              {/* Optional Action Button Link if provided (Never automatic redirect) */}
              {msg.actionLink && (
                <div className="mt-3 pt-2.5 border-t border-slate-200">
                  <button
                    type="button"
                    onClick={() => onNavigateTab(msg.actionLink!.tab)}
                    className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-xl bg-white border border-rose-300 text-rose-600 hover:bg-rose-50 font-bold text-xs shadow-sm transition-colors"
                  >
                    <span>{msg.actionLink.label}</span>
                    <ArrowRight size={13} />
                  </button>
                </div>
              )}

              {/* Suggestions */}
              {msg.suggestions && msg.suggestions.length > 0 && (
                <div className="mt-3 pt-2.5 border-t border-slate-200/80">
                  <p className="text-[10px] font-semibold text-slate-500 uppercase tracking-wider mb-1.5">
                    Suggested Questions
                  </p>
                  <div className="flex flex-wrap gap-1.5">
                    {msg.suggestions.map((sug) => {
                      const matchedItem = CORE_FAQ_SUGGESTIONS.find(
                        (c) =>
                          c.label.toLowerCase() === sug.toLowerCase() ||
                          c.query.toLowerCase() === sug.toLowerCase()
                      );
                      return (
                        <button
                          key={sug}
                          type="button"
                          onClick={() => handleProcessQuery(sug, matchedItem?.id)}
                          className="px-2.5 py-1 rounded-lg bg-white border border-slate-300 hover:border-rose-400 hover:text-rose-600 text-[11px] font-medium text-slate-700 transition-colors shadow-2xs text-left"
                        >
                          {sug}
                        </button>
                      );
                    })}
                  </div>
                </div>
              )}

              <span
                className={`text-[9px] block mt-2 ${
                  msg.sender === 'user' ? 'text-rose-200 text-right' : 'text-slate-400'
                }`}
              >
                {msg.timestamp}
              </span>
            </div>
          </div>
        ))}

        {isTyping && (
          <div className="flex items-center gap-3">
            <div className="w-8 h-8 rounded-full bg-rose-600 text-white flex items-center justify-center text-xs">
              <Sparkles size={14} />
            </div>
            <div className="bg-slate-100 rounded-2xl px-4 py-3 text-xs text-slate-500 flex items-center gap-1.5">
              <span className="w-1.5 h-1.5 rounded-full bg-slate-400 animate-bounce" />
              <span className="w-1.5 h-1.5 rounded-full bg-slate-400 animate-bounce [animation-delay:0.2s]" />
              <span className="w-1.5 h-1.5 rounded-full bg-slate-400 animate-bounce [animation-delay:0.4s]" />
              <span className="ml-1 font-medium">Assistant looking up verified LPG-Chain info...</span>
            </div>
          </div>
        )}
        <div ref={messagesEndRef} />
      </div>

      {/* Input Form */}
      <div className="px-4 sm:px-6 pt-2">
        <form
          onSubmit={(e) => {
            e.preventDefault();
            handleProcessQuery(inputValue);
          }}
          className="flex items-center gap-2 p-1.5 rounded-2xl border border-slate-300 focus-within:border-rose-600 focus-within:ring-2 focus-within:ring-rose-500/20 bg-white"
        >
          <input
            type="text"
            value={inputValue}
            onChange={(e) => setInputValue(e.target.value)}
            placeholder="Ask a question (e.g. how to book, track delivery, gas leak safety, KYC documents)..."
            className="flex-1 px-3 py-2 text-xs sm:text-sm text-slate-800 placeholder-slate-400 focus:outline-none bg-transparent"
          />
          <button
            type="submit"
            disabled={!inputValue.trim()}
            className="w-10 h-10 rounded-xl bg-rose-600 hover:bg-rose-500 disabled:bg-slate-200 text-white flex items-center justify-center transition-colors flex-shrink-0 shadow-sm"
            title="Send query"
          >
            <Send size={16} />
          </button>
        </form>
      </div>
    </div>
  );
};
