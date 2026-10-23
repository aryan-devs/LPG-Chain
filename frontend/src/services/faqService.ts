import { PortalTab, CustomerProfileData } from '../components/portal/portalTypes';

export type FaqIntentId =
  | 'BOOKING_HOWTO'
  | 'BOOKING_STATUS'
  | 'TRACKING_DELIVERY'
  | 'PAYMENT_HELP'
  | 'KYC_HELP'
  | 'COMPLAINT_HOWTO'
  | 'SAFETY_GUIDELINES'
  | 'CUSTOMER_SUPPORT';

export interface FaqSuggestionItem {
  id: FaqIntentId;
  label: string;
  query: string;
}

export interface FaqResult {
  reply: string;
  intentId: FaqIntentId | 'AMBIGUOUS' | 'OUT_OF_SCOPE';
  actionTab?: PortalTab;
  actionLabel?: string;
  supportPhone?: string;
  suggestions: string[];
}

export const SUPPORTED_FAQ_TITLES: Record<FaqIntentId, string> = {
  BOOKING_HOWTO: 'How to book an LPG cylinder',
  BOOKING_STATUS: 'How to check booking status',
  TRACKING_DELIVERY: 'How to track a dispatched delivery',
  PAYMENT_HELP: 'Payment methods and status help',
  KYC_HELP: 'Required KYC documents',
  COMPLAINT_HOWTO: 'How to raise a complaint',
  SAFETY_GUIDELINES: 'LPG safety guidelines and emergency guidance',
  CUSTOMER_SUPPORT: 'How to contact customer support',
};

export const CORE_FAQ_SUGGESTIONS: FaqSuggestionItem[] = [
  { id: 'BOOKING_HOWTO', label: 'How to book an LPG cylinder', query: 'How to book an LPG cylinder' },
  { id: 'BOOKING_STATUS', label: 'How to check booking status', query: 'How to check booking status' },
  { id: 'TRACKING_DELIVERY', label: 'How to track a dispatched delivery', query: 'How to track a dispatched delivery' },
  { id: 'PAYMENT_HELP', label: 'Payment methods and status help', query: 'Payment methods and status help' },
  { id: 'KYC_HELP', label: 'Required KYC documents', query: 'Required KYC documents' },
  { id: 'COMPLAINT_HOWTO', label: 'How to raise a complaint', query: 'How to raise a complaint' },
  { id: 'SAFETY_GUIDELINES', label: 'LPG safety guidelines and emergency guidance', query: 'LPG safety guidelines and emergency guidance' },
  { id: 'CUSTOMER_SUPPORT', label: 'How to contact customer support', query: 'How to contact customer support' },
];

/**
 * Reads verified customer support phone from configuration.
 * Fallback to default verified helpline '1800-233-3555'.
 */
export const getConfiguredSupportPhone = (): string | null => {
  const envPhone = (import.meta as any).env?.VITE_LPGCHAIN_SUPPORT_PHONE;
  if (typeof envPhone === 'string') {
    const trimmed = envPhone.trim();
    if (!trimmed || trimmed.toLowerCase() === 'none' || trimmed.toLowerCase() === 'null') {
      return null;
    }
    return trimmed;
  }
  return '1800-233-3555';
};

/**
 * Normalizes input: lowercase, strip punctuation, collapse whitespace.
 */
export const normalizeQuery = (text: string): string => {
  if (!text) return '';
  return text
    .toLowerCase()
    .replace(/[^a-z0-9\s]/g, ' ')
    .replace(/\s+/g, ' ')
    .trim();
};

/**
 * Match a typed user query to an intent ID using maintainable canonical phrases,
 * token intersections, and synonym sets.
 */
export const matchTypedFaqIntent = (query: string): FaqIntentId | 'AMBIGUOUS' | 'OUT_OF_SCOPE' => {
  const normalized = normalizeQuery(query);
  if (!normalized) return 'AMBIGUOUS';

  const tokens = normalized.split(' ');

  // Greetings / short single-word prompts
  if (tokens.length <= 2 && ['hi', 'hello', 'hey', 'help', 'greetings', 'morning', 'afternoon'].some(g => tokens.includes(g))) {
    return 'AMBIGUOUS';
  }

  // 1. Direct canonical phrase matching
  if (
    normalized.includes('how to book') ||
    normalized.includes('how can i book') ||
    normalized.includes('book cylinder') ||
    normalized.includes('book a cylinder') ||
    normalized.includes('order cylinder') ||
    normalized.includes('order a cylinder') ||
    normalized.includes('steps to book') ||
    normalized.includes('buy cylinder') ||
    normalized.includes('order refill') ||
    normalized.includes('book refill') ||
    normalized.includes('want to book') ||
    normalized.includes('need a cylinder')
  ) {
    return 'BOOKING_HOWTO';
  }

  if (
    normalized.includes('check booking status') ||
    normalized.includes('booking status') ||
    normalized.includes('where is my booking') ||
    normalized.includes('where can i see my order') ||
    normalized.includes('where is my order') ||
    normalized.includes('order status') ||
    normalized.includes('check my order') ||
    normalized.includes('check my booking') ||
    normalized.includes('booking history') ||
    normalized.includes('refill status') ||
    normalized.includes('did my booking go through') ||
    normalized.includes('is my cylinder booked')
  ) {
    return 'BOOKING_STATUS';
  }

  if (
    normalized.includes('track delivery') ||
    normalized.includes('track dispatched delivery') ||
    normalized.includes('track my delivery') ||
    normalized.includes('live tracking') ||
    normalized.includes('delivery tracking') ||
    normalized.includes('delivery status') ||
    normalized.includes('driver location') ||
    normalized.includes('gps location') ||
    normalized.includes('where is my cylinder') ||
    normalized.includes('delivery vehicle') ||
    normalized.includes('out for delivery') ||
    normalized.includes('track driver') ||
    normalized.includes('delivery otp')
  ) {
    return 'TRACKING_DELIVERY';
  }

  if (
    normalized.includes('payment methods') ||
    normalized.includes('payment status') ||
    normalized.includes('how to pay') ||
    normalized.includes('pay bill') ||
    normalized.includes('payment options') ||
    normalized.includes('can i pay') ||
    normalized.includes('cash on delivery') ||
    normalized.includes('cod') ||
    normalized.includes('upi payment') ||
    normalized.includes('razorpay') ||
    normalized.includes('qr code') ||
    normalized.includes('dbtl subsidy') ||
    normalized.includes('pahal subsidy') ||
    normalized.includes('subsidy help') ||
    normalized.includes('subsidy status') ||
    normalized.includes('cylinder price') ||
    normalized.includes('billing help')
  ) {
    return 'PAYMENT_HELP';
  }

  if (
    normalized.includes('required kyc documents') ||
    normalized.includes('kyc documents') ||
    normalized.includes('document upload') ||
    normalized.includes('what documents') ||
    normalized.includes('documents do i need') ||
    normalized.includes('documents required') ||
    normalized.includes('kyc requirements') ||
    normalized.includes('upload aadhaar') ||
    normalized.includes('upload ration card') ||
    normalized.includes('kyc verification') ||
    normalized.includes('documents needed')
  ) {
    return 'KYC_HELP';
  }

  if (
    normalized.includes('how to raise a complaint') ||
    normalized.includes('raise a complaint') ||
    normalized.includes('raise complaint') ||
    normalized.includes('file complaint') ||
    normalized.includes('register complaint') ||
    normalized.includes('report issue') ||
    normalized.includes('file grievance') ||
    normalized.includes('report delayed cylinder') ||
    normalized.includes('damaged cylinder') ||
    normalized.includes('broken seal') ||
    normalized.includes('log complaint')
  ) {
    return 'COMPLAINT_HOWTO';
  }

  if (
    normalized.includes('lpg safety guidelines') ||
    normalized.includes('safety guidelines') ||
    normalized.includes('emergency guidance') ||
    normalized.includes('gas leak') ||
    normalized.includes('smell gas') ||
    normalized.includes('what should i do if i smell gas') ||
    normalized.includes('safety tips') ||
    normalized.includes('1906 helpline') ||
    normalized.includes('tare weight') ||
    normalized.includes('cylinder weight check') ||
    normalized.includes('fire emergency')
  ) {
    return 'SAFETY_GUIDELINES';
  }

  if (
    normalized.includes('how to contact customer support') ||
    normalized.includes('contact customer support') ||
    normalized.includes('customer support') ||
    normalized.includes('customer care') ||
    normalized.includes('contact support') ||
    normalized.includes('helpline number') ||
    normalized.includes('support phone') ||
    normalized.includes('call support') ||
    normalized.includes('support desk') ||
    normalized.includes('toll free')
  ) {
    return 'CUSTOMER_SUPPORT';
  }

  // 2. Token intersections
  const hasToken = (t: string) => tokens.includes(t);

  if ((hasToken('book') || hasToken('order') || hasToken('refill')) && (hasToken('cylinder') || hasToken('gas') || hasToken('lpg'))) {
    if (hasToken('status') || hasToken('where') || hasToken('check') || hasToken('track')) {
      return 'BOOKING_STATUS';
    }
    return 'BOOKING_HOWTO';
  }

  if (hasToken('track') || hasToken('tracking') || hasToken('driver') || hasToken('dispatch') || hasToken('dispatched')) {
    return 'TRACKING_DELIVERY';
  }

  if (hasToken('payment') || hasToken('pay') || hasToken('cod') || hasToken('upi') || hasToken('subsidy') || hasToken('dbtl') || hasToken('pahal')) {
    return 'PAYMENT_HELP';
  }

  if (hasToken('kyc') || (hasToken('document') || hasToken('documents')) && (hasToken('upload') || hasToken('proof') || hasToken('aadhaar') || hasToken('ration'))) {
    return 'KYC_HELP';
  }

  if (hasToken('complaint') || hasToken('grievance') || (hasToken('issue') && (hasToken('cylinder') || hasToken('delay') || hasToken('leakage')))) {
    return 'COMPLAINT_HOWTO';
  }

  if (hasToken('safety') || hasToken('emergency') || hasToken('1906') || hasToken('leak') || hasToken('leakage') || hasToken('smell')) {
    return 'SAFETY_GUIDELINES';
  }

  if (hasToken('support') || hasToken('helpline') || (hasToken('call') && hasToken('care'))) {
    return 'CUSTOMER_SUPPORT';
  }

  return 'OUT_OF_SCOPE';
};

/**
 * Returns verified, comprehensive answers for each of the 8 supported FAQ categories.
 */
export const getFaqAnswerForIntent = (
  intent: FaqIntentId | 'AMBIGUOUS' | 'OUT_OF_SCOPE',
  profile?: CustomerProfileData
): FaqResult => {
  const phone = getConfiguredSupportPhone();

  switch (intent) {
    case 'BOOKING_HOWTO':
      return {
        intentId: 'BOOKING_HOWTO',
        reply:
          `To book an LPG cylinder with LPG-Chain, follow these verified steps:\n\n` +
          `1. **Navigate to Booking**: Open the **Book a Cylinder** view in your Customer Portal.\n` +
          `2. **Select Cylinder Category**:\n` +
          `   • **Domestic 14.2 kg** — ₹853.00 (Eligible for ₹200 direct DBTL government subsidy credited to your bank account).\n` +
          `   • **Domestic 5 kg Mini** — ₹340.00 (Ideal for backup or small households).\n` +
          `   • **Commercial 19 kg** — ₹1,850.00 (For commercial and hospitality use).\n` +
          `3. **Confirm Delivery Address**: Choose your verified delivery address from saved addresses.\n` +
          `4. **Choose Payment Mode**: Pay securely via Demo Online Checkout (UPI / Cards / NetBanking) or choose Cash on Delivery (COD).\n` +
          `5. **Instant Confirmation**: Submit to generate your unique Booking Number and live order status.`,
        actionTab: 'book',
        actionLabel: 'Go to Book Cylinder',
        supportPhone: phone || undefined,
        suggestions: [
          'How to check booking status',
          'Payment methods and status help',
          'How to track a dispatched delivery',
        ],
      };

    case 'BOOKING_STATUS':
      return {
        intentId: 'BOOKING_STATUS',
        reply:
          `Here is how to check your booking status on LPG-Chain:\n\n` +
          `• **In the Portal**: Navigate to the **Order History** section to view current and previous orders.\n` +
          `• **Order Stages**: Every booking transitions through verified milestones: \`PENDING\` → \`CONFIRMED\` → \`OUT_FOR_DELIVERY\` → \`DELIVERED\`.\n` +
          `• **Live Status**: Once marked \`OUT_FOR_DELIVERY\`, live GPS coordinates and driver contact activate in the Track Delivery tab.\n` +
          (profile ? `• **Account Context**: Hello ${profile.name.split(' ')[0]}, you have ${profile.subsidizedQuotaLeft} subsidized refills left this financial year.` : `• **Account Data**: Please log in to view your live personal booking records.`),
        actionTab: 'history',
        actionLabel: 'View Order History',
        supportPhone: phone || undefined,
        suggestions: [
          'How to book an LPG cylinder',
          'How to track a dispatched delivery',
          'Payment methods and status help',
        ],
      };

    case 'TRACKING_DELIVERY':
      return {
        intentId: 'TRACKING_DELIVERY',
        reply:
          `Live delivery tracking is available for all dispatched orders on LPG-Chain:\n\n` +
          `1. **Dispatch Notification**: When your distributor assigns a vehicle and delivery driver, your order updates to \`OUT_FOR_DELIVERY\`.\n` +
          `2. **Real-Time GPS Map**: Open the **Track Delivery** view to see live vehicle GPS location, driver details, and arrival progress.\n` +
          `3. **Delivery Handover OTP**: For security and subsidy authorization, share your 4-digit Delivery Handover OTP with the delivery executive only when the physical cylinder is handed over at your doorstep.\n\n` +
          `You can view active delivery progress anytime under the Track Delivery tab.`,
        actionTab: 'track',
        actionLabel: 'Open Track Delivery',
        supportPhone: phone || undefined,
        suggestions: [
          'How to check booking status',
          'LPG safety guidelines and emergency guidance',
          'How to contact customer support',
        ],
      };

    case 'PAYMENT_HELP':
      return {
        intentId: 'PAYMENT_HELP',
        reply:
          `LPG-Chain supports the following verified payment methods and subsidy policies:\n\n` +
          `• **Demo Online Checkout (Razorpay Simulation)**: Pay securely via simulated UPI, Credit/Debit cards, or Net Banking with immediate receipt generation.\n` +
          `• **Cash on Delivery (COD)**: Pay cash or scan the delivery executive's dynamic QR code upon physical cylinder handover.\n` +
          `• **DBTL / PAHAL Subsidy**: A direct ₹200 government subsidy is credited to your registered bank account for domestic 14.2 kg refills following OTP delivery verification.\n` +
          `• **Payment Status & Invoices**: Review payment records, download tax memos, and verify transaction references in the **Billing & Payments** section.\n\n` +
          `Note: We support both demo online checkout and Cash on Delivery. Unsupported payment methods are not accepted.`,
        actionTab: 'billing',
        actionLabel: 'View Billing & Payments',
        supportPhone: phone || undefined,
        suggestions: [
          'How to book an LPG cylinder',
          'How to check booking status',
          'Required KYC documents',
        ],
      };

    case 'KYC_HELP':
      return {
        intentId: 'KYC_HELP',
        reply:
          `To comply with safety regulations and qualify for direct DBTL subsidies, customers must submit 4 required KYC documents:\n\n` +
          `1. **Proof of Identity (PoI)**: Aadhaar Card, Voter ID, or Passport.\n` +
          `2. **Proof of Address (PoA)**: Ration Card or Electricity Bill.\n` +
          `3. **Bank Account Proof**: Bank Passbook or Cancelled Cheque (required for direct subsidy deposit).\n` +
          `4. **Passport Photograph**: Recent photograph.\n\n` +
          `**Document Upload Guidelines**:\n` +
          `• Supported file formats: PDF, JPG, PNG (up to 10 MB per file).\n` +
          `• Upload and track verification under the **KYC Documents** view. Your assigned LPG distributor conducts verification within 24 to 48 hours.`,
        actionTab: 'kyc',
        actionLabel: 'Go to KYC Documents',
        supportPhone: phone || undefined,
        suggestions: [
          'Payment methods and status help',
          'How to book an LPG cylinder',
          'How to contact customer support',
        ],
      };

    case 'COMPLAINT_HOWTO':
      return {
        intentId: 'COMPLAINT_HOWTO',
        reply:
          `If you experience any service irregularity, you can raise an official complaint through LPG-Chain:\n\n` +
          `1. Navigate to the **Help & Support** tab in your portal.\n` +
          `2. Select the issue category:\n` +
          `   • **Gas Leakage / Emergency**: *Dial 1906 immediately!*\n` +
          `   • **Delayed Delivery**\n` +
          `   • **Cylinder Shortage / Underweight**\n` +
          `   • **Overcharging / Pricing Dispute**\n` +
          `   • **Defective Valve or Seal**\n` +
          `3. Provide a brief description and submit. A unique Complaint Ticket ID is generated for real-time resolution tracking.\n\n` +
          `Distributors are required to resolve consumer grievances within 24 to 48 hours.`,
        actionTab: 'support',
        actionLabel: 'Raise a Complaint',
        supportPhone: phone || undefined,
        suggestions: [
          'LPG safety guidelines and emergency guidance',
          'How to contact customer support',
          'How to check booking status',
        ],
      };

    case 'SAFETY_GUIDELINES':
      return {
        intentId: 'SAFETY_GUIDELINES',
        reply:
          `🚨 **LPG Safety Guidelines & Emergency Guidance**:\n\n` +
          `**Immediate Protocol in Case of Gas Smell or Leakage**:\n` +
          `1. **NO Sparks or Flames**: Do NOT turn any electrical switches, fans, or appliances ON or OFF. Sparks can ignite gas. Extinguish all matches, incense, and open flames immediately.\n` +
          `2. **Close Cylinder Regulator**: Turn the regulator knob clockwise to the OFF position if safe to do so.\n` +
          `3. **Ventilate the Premises**: Open all doors and windows to disperse accumulated gas (LPG is heavier than air and sinks to the floor) without operating electrical exhaust fans.\n` +
          `4. **Evacuate to Safety**: Move all family members outside into fresh air immediately.\n` +
          `5. **Emergency Helpline**: Evacuate and call the **24/7 LPG Emergency Helpline at 1906** (Toll-Free) or Fire Services at 101. Do not use your phone inside the kitchen.\n\n` +
          `**Cylinder Inspection Tips**:\n` +
          `• Verify tare weight stamped on cylinder stay plate. Net LPG weight for domestic cylinders is 14.2 kg.\n` +
          `• Check that the plastic safety cap seal is intact before accepting delivery from the driver.`,
        supportPhone: phone || undefined,
        suggestions: [
          'How to raise a complaint',
          'How to contact customer support',
          'How to book an LPG cylinder',
        ],
      };

    case 'CUSTOMER_SUPPORT':
      return {
        intentId: 'CUSTOMER_SUPPORT',
        reply: phone
          ? `You can reach LPG-Chain customer support through the following verified channels:\n\n` +
            `• **Customer Support Helpline**: Call us at **${phone}** for booking, billing, KYC, and connection queries.\n` +
            `• **24/7 LPG Emergency Helpline**: Dial **1906** (Toll-Free) immediately for gas leaks or fire emergencies.\n` +
            `• **Help & Support Portal**: Raise an online ticket under the Help & Support tab for priority distributor assistance.\n\n` +
            `Support hours: Monday to Saturday, 8:00 AM – 8:00 PM.`
          : `You can reach LPG-Chain customer support through our portal and emergency channels:\n\n` +
            `• **24/7 LPG Emergency Helpline**: Dial **1906** (Toll-Free) immediately for gas leaks or fire emergencies.\n` +
            `• **Help & Support Portal**: Raise an online ticket under the Help & Support tab for priority distributor assistance.`,
        actionTab: 'support',
        actionLabel: 'Visit Help & Support',
        supportPhone: phone || undefined,
        suggestions: [
          'How to raise a complaint',
          'How to book an LPG cylinder',
          'LPG safety guidelines and emergency guidance',
        ],
      };

    case 'AMBIGUOUS':
      return {
        intentId: 'AMBIGUOUS',
        reply:
          `Hello! I am your LPG-Chain Operations Assistant. How can I assist you today?\n\n` +
          `You can select any of the common topics below or type your question:\n` +
          `• How to book an LPG cylinder\n` +
          `• Checking booking and delivery status\n` +
          `• Payment methods and DBTL subsidy\n` +
          `• Required KYC documents & verification\n` +
          `• How to raise a complaint\n` +
          `• LPG safety guidelines & emergency guidance\n` +
          `• Customer support contact details`,
        supportPhone: phone || undefined,
        suggestions: [
          'How to book an LPG cylinder',
          'How to check booking status',
          'LPG safety guidelines and emergency guidance',
          'How to contact customer support',
        ],
      };

    case 'OUT_OF_SCOPE':
    default:
      if (phone) {
        return {
          intentId: 'OUT_OF_SCOPE',
          reply:
            `I apologize, but I cannot answer that particular question as it is outside the scope of LPG-Chain services. ` +
            `Please contact the LPG-Chain customer support team at ${phone} for assistance.`,
          actionTab: 'support',
          actionLabel: 'Contact Support',
          supportPhone: phone,
          suggestions: [
            'How to book an LPG cylinder',
            'How to check booking status',
            'LPG safety guidelines and emergency guidance',
            'How to contact customer support',
          ],
        };
      } else {
        return {
          intentId: 'OUT_OF_SCOPE',
          reply: `I couldn't find an answer to that question. Please contact the LPG-Chain support team for assistance.`,
          actionTab: 'support',
          actionLabel: 'Contact Support',
          supportPhone: undefined,
          suggestions: [
            'How to book an LPG cylinder',
            'How to check booking status',
            'LPG safety guidelines and emergency guidance',
            'How to contact customer support',
          ],
        };
      }
  }
};
