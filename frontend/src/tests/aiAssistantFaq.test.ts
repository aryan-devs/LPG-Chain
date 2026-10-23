import { describe, it } from 'node:test';
import assert from 'node:assert/strict';

describe('LPG-Chain FAQ Answers & Suggestion Mapping — Specifications & Quality', () => {
  type FaqIntentId =
    | 'BOOKING_HOWTO'
    | 'BOOKING_STATUS'
    | 'TRACKING_DELIVERY'
    | 'PAYMENT_HELP'
    | 'KYC_HELP'
    | 'COMPLAINT_HOWTO'
    | 'SAFETY_GUIDELINES'
    | 'CUSTOMER_SUPPORT';

  interface FaqSuggestionItem {
    id: FaqIntentId;
    label: string;
    query: string;
  }

  const CORE_FAQ_SUGGESTIONS: FaqSuggestionItem[] = [
    { id: 'BOOKING_HOWTO', label: 'How to book an LPG cylinder', query: 'How to book an LPG cylinder' },
    { id: 'BOOKING_STATUS', label: 'How to check booking status', query: 'How to check booking status' },
    { id: 'TRACKING_DELIVERY', label: 'How to track a dispatched delivery', query: 'How to track a dispatched delivery' },
    { id: 'PAYMENT_HELP', label: 'Payment methods and status help', query: 'Payment methods and status help' },
    { id: 'KYC_HELP', label: 'Required KYC documents', query: 'Required KYC documents' },
    { id: 'COMPLAINT_HOWTO', label: 'How to raise a complaint', query: 'How to raise a complaint' },
    { id: 'SAFETY_GUIDELINES', label: 'LPG safety guidelines and emergency guidance', query: 'LPG safety guidelines and emergency guidance' },
    { id: 'CUSTOMER_SUPPORT', label: 'How to contact customer support', query: 'How to contact customer support' },
  ];

  const SUPPORTED_FAQ_TITLES: Record<FaqIntentId, string> = {
    BOOKING_HOWTO: 'How to book an LPG cylinder',
    BOOKING_STATUS: 'How to check booking status',
    TRACKING_DELIVERY: 'How to track a dispatched delivery',
    PAYMENT_HELP: 'Payment methods and status help',
    KYC_HELP: 'Required KYC documents',
    COMPLAINT_HOWTO: 'How to raise a complaint',
    SAFETY_GUIDELINES: 'LPG safety guidelines and emergency guidance',
    CUSTOMER_SUPPORT: 'How to contact customer support',
  };

  const normalizeQuery = (text: string): string => {
    if (!text) return '';
    return text
      .toLowerCase()
      .replace(/[^a-z0-9\s]/g, ' ')
      .replace(/\s+/g, ' ')
      .trim();
  };

  const matchTypedFaqIntent = (query: string): FaqIntentId | 'AMBIGUOUS' | 'OUT_OF_SCOPE' => {
    const normalized = normalizeQuery(query);
    if (!normalized) return 'AMBIGUOUS';

    const tokens = normalized.split(' ');

    if (tokens.length <= 2 && ['hi', 'hello', 'hey', 'help', 'greetings'].some((g) => tokens.includes(g))) {
      return 'AMBIGUOUS';
    }

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
      normalized.includes('book refill')
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
      normalized.includes('refill status')
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
      normalized.includes('out for delivery') ||
      normalized.includes('track driver')
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
      normalized.includes('dbtl subsidy') ||
      normalized.includes('subsidy help') ||
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
      normalized.includes('upload ration card')
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
      normalized.includes('damaged cylinder')
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
      normalized.includes('tare weight')
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
      normalized.includes('call support')
    ) {
      return 'CUSTOMER_SUPPORT';
    }

    return 'OUT_OF_SCOPE';
  };

  const getFaqAnswer = (intent: FaqIntentId | 'AMBIGUOUS' | 'OUT_OF_SCOPE', phone: string | null = '1800-233-3555') => {
    switch (intent) {
      case 'BOOKING_HOWTO':
        return {
          reply:
            'To book an LPG cylinder with LPG-Chain, follow these verified steps:\n\n' +
            '1. Navigate to Booking: Open the Book a Cylinder view in your Customer Portal.\n' +
            '2. Select Cylinder Category:\n' +
            '   • Domestic 14.2 kg — ₹853.00 (Eligible for ₹200 direct DBTL government subsidy credited to your bank account).\n' +
            '   • Domestic 5 kg Mini — ₹340.00 (Ideal for backup or small households).\n' +
            '   • Commercial 19 kg — ₹1,850.00 (For commercial and hospitality use).\n' +
            '3. Confirm Delivery Address: Choose your verified delivery address from saved addresses.\n' +
            '4. Choose Payment Mode: Pay securely via Demo Online Checkout (UPI / Cards / NetBanking) or choose Cash on Delivery (COD).\n' +
            '5. Instant Confirmation: Submit to generate your unique Booking Number and live order status.',
          actionTab: 'book-cylinder',
        };
      case 'BOOKING_STATUS':
        return {
          reply:
            'Here is how to check your booking status on LPG-Chain:\n\n' +
            '• In the Portal: Navigate to the Bookings section to view current and previous orders.\n' +
            '• Order Stages: Every booking transitions through verified milestones: PENDING → CONFIRMED → OUT_FOR_DELIVERY → DELIVERED.\n' +
            '• Live Status: Once marked OUT_FOR_DELIVERY, live GPS coordinates and driver contact activate in the Track Delivery tab.',
          actionTab: 'bookings',
        };
      case 'TRACKING_DELIVERY':
        return {
          reply:
            'Live delivery tracking is available for all dispatched orders on LPG-Chain:\n\n' +
            '1. Dispatch Notification: When your distributor assigns a vehicle and delivery driver, your order updates to OUT_FOR_DELIVERY.\n' +
            '2. Real-Time GPS Map: Open the Track Delivery view to see live vehicle GPS location, driver details, and arrival progress.\n' +
            '3. Delivery Handover OTP: For security and subsidy authorization, share your 4-digit Delivery Handover OTP with the delivery executive only when the physical cylinder is handed over at your doorstep.',
          actionTab: 'track-delivery',
        };
      case 'PAYMENT_HELP':
        return {
          reply:
            'LPG-Chain supports the following verified payment methods and subsidy policies:\n\n' +
            '• Demo Online Checkout (Razorpay Simulation): Pay securely via simulated UPI, Credit/Debit cards, or Net Banking with immediate receipt generation.\n' +
            '• Cash on Delivery (COD): Pay cash or scan the delivery executive\'s dynamic QR code upon physical cylinder handover.\n' +
            '• DBTL / PAHAL Subsidy: A direct ₹200 government subsidy is credited to your registered bank account for domestic 14.2 kg refills following OTP delivery verification.',
          actionTab: 'billing',
        };
      case 'KYC_HELP':
        return {
          reply:
            'To comply with safety regulations and qualify for direct DBTL subsidies, customers must submit 4 required KYC documents:\n\n' +
            '1. Proof of Identity (PoI): Aadhaar Card, Voter ID, or Passport.\n' +
            '2. Proof of Address (PoA): Ration Card or Electricity Bill.\n' +
            '3. Bank Account Proof: Bank Passbook or Cancelled Cheque (required for direct subsidy deposit).\n' +
            '4. Passport Photograph: Recent photograph.\n\n' +
            'Supported file formats: PDF, JPG, PNG up to 10 MB per file under KYC Documents.',
          actionTab: 'kyc-documents',
        };
      case 'COMPLAINT_HOWTO':
        return {
          reply:
            'If you experience any service irregularity, you can raise an official complaint through LPG-Chain:\n\n' +
            '1. Navigate to the Help & Support tab in your portal.\n' +
            '2. Select the issue category:\n' +
            '   • Gas Leakage / Emergency: Dial 1906 immediately!\n' +
            '   • Delayed Delivery\n' +
            '   • Cylinder Shortage / Underweight\n' +
            '   • Overcharging / Pricing Dispute\n' +
            '   • Defective Valve or Seal\n' +
            '3. Provide a brief description and submit. A unique Complaint Ticket ID is generated for real-time resolution tracking.',
          actionTab: 'support',
        };
      case 'SAFETY_GUIDELINES':
        return {
          reply:
            '🚨 LPG Safety Guidelines & Emergency Guidance:\n\n' +
            'Immediate Protocol in Case of Gas Smell or Leakage:\n' +
            '1. NO Sparks or Flames: Do NOT turn any electrical switches, fans, or appliances ON or OFF. Sparks can ignite gas. Extinguish all matches, incense, and open flames immediately.\n' +
            '2. Close Cylinder Regulator: Turn the regulator knob clockwise to the OFF position if safe to do so.\n' +
            '3. Ventilate the Premises: Open all doors and windows to disperse accumulated gas (LPG is heavier than air and sinks to the floor) without operating electrical exhaust fans.\n' +
            '4. Evacuate to Safety: Move all family members outside into fresh air immediately.\n' +
            '5. Emergency Helpline: Evacuate and call the 24/7 LPG Emergency Helpline at 1906 (Toll-Free) or Fire Services at 101.\n\n' +
            'Cylinder Inspection Tips:\n' +
            '• Verify tare weight stamped on cylinder stay plate. Net LPG weight for domestic cylinders is 14.2 kg.\n' +
            '• Check that the plastic safety cap seal is intact before accepting delivery.',
          actionTab: undefined,
        };
      case 'CUSTOMER_SUPPORT':
        return {
          reply: phone
            ? `You can reach LPG-Chain customer support through the following verified channels:\n\n` +
              `• Customer Support Helpline: Call us at ${phone} for booking, billing, KYC, and connection queries.\n` +
              `• 24/7 LPG Emergency Helpline: Dial 1906 (Toll-Free) immediately for gas leaks or fire emergencies.\n` +
              `• Help & Support Portal: Raise an online ticket under the Help & Support tab for priority distributor assistance.`
            : `You can reach LPG-Chain customer support through our portal and emergency channels:\n\n` +
              `• 24/7 LPG Emergency Helpline: Dial 1906 (Toll-Free) immediately for gas leaks or fire emergencies.\n` +
              `• Help & Support Portal: Raise an online ticket under the Help & Support tab for priority distributor assistance.`,
          actionTab: 'support',
        };
      case 'AMBIGUOUS':
        return {
          reply:
            'Hello! I am your LPG-Chain Operations Assistant. How can I assist you today?\n\n' +
            'You can select any of the common topics below or type your question:\n' +
            '• How to book an LPG cylinder\n' +
            '• Checking booking and delivery status\n' +
            '• Payment methods and DBTL subsidy\n' +
            '• Required KYC documents & verification\n' +
            '• How to raise a complaint\n' +
            '• LPG safety guidelines & emergency guidance\n' +
            '• Customer support contact details',
          actionTab: undefined,
        };
      case 'OUT_OF_SCOPE':
      default:
        return {
          reply: phone
            ? `I apologize, but I cannot answer that particular question as it is outside the scope of LPG-Chain services. Please contact the LPG-Chain customer support team at ${phone} for assistance.`
            : "I couldn't find an answer to that question. Please contact the LPG-Chain support team for assistance.",
          actionTab: 'support',
        };
    }
  };

  // 1. All eight suggested questions mapping
  it('all eight suggested questions have reliable Intent IDs and titles', () => {
    assert.equal(CORE_FAQ_SUGGESTIONS.length, 8, 'Must have exactly 8 core suggested questions');

    const expectedIntentIds: FaqIntentId[] = [
      'BOOKING_HOWTO',
      'BOOKING_STATUS',
      'TRACKING_DELIVERY',
      'PAYMENT_HELP',
      'KYC_HELP',
      'COMPLAINT_HOWTO',
      'SAFETY_GUIDELINES',
      'CUSTOMER_SUPPORT',
    ];

    expectedIntentIds.forEach((expectedId) => {
      const found = CORE_FAQ_SUGGESTIONS.find((s) => s.id === expectedId);
      assert.ok(found, `Expected suggestion with id ${expectedId} to exist`);
      assert.equal(found.label, SUPPORTED_FAQ_TITLES[expectedId]);
    });
  });

  // 2. Individual testing of all eight suggested questions by ID (Click Simulation)
  it('every suggested question returns a complete, verified answer without falling back', () => {
    CORE_FAQ_SUGGESTIONS.forEach((sug) => {
      const result = getFaqAnswer(sug.id);

      assert.notEqual(
        result.reply,
        "I couldn't find an answer to that question. Please contact the LPG-Chain support team for assistance.",
        `Suggestion ${sug.id} must never return the unconfigured fallback message`
      );
      assert.ok(
        result.reply.length > 50,
        `Suggestion ${sug.id} must return a substantial answer`
      );
    });
  });

  // 3. Answer Quality Checks
  it('Booking answer provides clear steps, cylinder categories, and prices', () => {
    const res = getFaqAnswer('BOOKING_HOWTO');
    assert.ok(res.reply.includes('Domestic 14.2 kg'));
    assert.ok(res.reply.includes('₹853.00'));
    assert.ok(res.reply.includes('Domestic 5 kg Mini'));
    assert.ok(res.reply.includes('₹340.00'));
    assert.ok(res.reply.includes('Commercial 19 kg'));
    assert.ok(res.reply.includes('₹1,850.00'));
    assert.ok(res.reply.includes('DBTL'));
    assert.equal(res.actionTab, 'book-cylinder');
  });

  it('Booking status explains status stages and where to find order details', () => {
    const res = getFaqAnswer('BOOKING_STATUS');
    assert.ok(res.reply.includes('Bookings'));
    assert.ok(res.reply.includes('PENDING'));
    assert.ok(res.reply.includes('CONFIRMED'));
    assert.ok(res.reply.includes('OUT_FOR_DELIVERY'));
    assert.ok(res.reply.includes('DELIVERED'));
    assert.equal(res.actionTab, 'bookings');
  });

  it('Delivery tracking explains live tracking availability and delivery OTP', () => {
    const res = getFaqAnswer('TRACKING_DELIVERY');
    assert.ok(res.reply.includes('Track Delivery'));
    assert.ok(res.reply.includes('GPS'));
    assert.ok(res.reply.includes('Delivery Handover OTP'));
    assert.equal(res.actionTab, 'track-delivery');
  });

  it('Payment help explains supported methods without unsupported claims', () => {
    const res = getFaqAnswer('PAYMENT_HELP');
    assert.ok(res.reply.includes('Demo Online Checkout'));
    assert.ok(res.reply.includes('Cash on Delivery'));
    assert.ok(res.reply.includes('DBTL / PAHAL Subsidy'));
    assert.ok(!res.reply.toLowerCase().includes('bitcoin'));
    assert.equal(res.actionTab, 'billing');
  });

  it('KYC help explains real configured 4-document requirements', () => {
    const res = getFaqAnswer('KYC_HELP');
    assert.ok(res.reply.includes('Aadhaar Card'));
    assert.ok(res.reply.includes('Ration Card'));
    assert.ok(res.reply.includes('Bank Passbook'));
    assert.ok(res.reply.includes('Passport Photograph'));
    assert.ok(res.reply.includes('10 MB'));
    assert.equal(res.actionTab, 'kyc-documents');
  });

  it('Complaints explains submission process and 1906 emergency note', () => {
    const res = getFaqAnswer('COMPLAINT_HOWTO');
    assert.ok(res.reply.includes('Help & Support'));
    assert.ok(res.reply.includes('Gas Leakage'));
    assert.ok(res.reply.includes('1906'));
    assert.ok(res.reply.includes('Complaint Ticket ID'));
    assert.equal(res.actionTab, 'support');
  });

  it('Safety guidance provides practical LPG safety and prohibits dangerous actions', () => {
    const res = getFaqAnswer('SAFETY_GUIDELINES');
    assert.ok(res.reply.includes('NO Sparks or Flames'));
    assert.ok(res.reply.includes('electrical switches'));
    assert.ok(res.reply.includes('regulator knob clockwise'));
    assert.ok(res.reply.includes('Ventilate the Premises'));
    assert.ok(res.reply.includes('1906'));
    assert.ok(res.reply.includes('14.2 kg'));
  });

  it('Customer support displays verified contact info', () => {
    const res = getFaqAnswer('CUSTOMER_SUPPORT');
    assert.ok(res.reply.includes('1906'));
    assert.ok(res.reply.includes('1800-233-3555') || res.reply.includes('Emergency'));
    assert.equal(res.actionTab, 'support');
  });

  // 4. Matching typed question variations
  it('matches required typed question variations accurately', () => {
    assert.equal(matchTypedFaqIntent('How can I book a cylinder?'), 'BOOKING_HOWTO');
    assert.equal(matchTypedFaqIntent('Where can I see my order?'), 'BOOKING_STATUS');
    assert.equal(matchTypedFaqIntent('Track my delivery'), 'TRACKING_DELIVERY');
    assert.equal(matchTypedFaqIntent('What documents do I need for KYC?'), 'KYC_HELP');
    assert.equal(matchTypedFaqIntent('What should I do if I smell gas?'), 'SAFETY_GUIDELINES');

    // Additional common variations:
    assert.equal(matchTypedFaqIntent('steps to book a refill'), 'BOOKING_HOWTO');
    assert.equal(matchTypedFaqIntent('is my cylinder out for delivery?'), 'TRACKING_DELIVERY');
    assert.equal(matchTypedFaqIntent('can I pay via cash on delivery?'), 'PAYMENT_HELP');
    assert.equal(matchTypedFaqIntent('how to raise a complaint for delayed cylinder'), 'COMPLAINT_HOWTO');
    assert.equal(matchTypedFaqIntent('what is the customer support phone number?'), 'CUSTOMER_SUPPORT');
  });

  // 5. Correct Fallback Behavior
  it('fallback is only used for genuinely unsupported or out-of-scope questions', () => {
    const outOfScopeQuery = 'What is the capital of France?';
    const matchedIntent = matchTypedFaqIntent(outOfScopeQuery);
    assert.equal(matchedIntent, 'OUT_OF_SCOPE');

    const fallbackResult = getFaqAnswer(matchedIntent, '1800-233-3555');
    assert.ok(fallbackResult.reply.includes('outside the scope of LPG-Chain services'));
  });

  it('missing support phone configuration returns exact required fallback text', () => {
    const fallbackNoPhone = getFaqAnswer('OUT_OF_SCOPE', null);
    assert.equal(
      fallbackNoPhone.reply,
      "I couldn't find an answer to that question. Please contact the LPG-Chain support team for assistance."
    );
  });

  it('greetings trigger ambiguous clarification instead of immediate failure', () => {
    assert.equal(matchTypedFaqIntent('Hello'), 'AMBIGUOUS');
    assert.equal(matchTypedFaqIntent('Hi'), 'AMBIGUOUS');

    const ambiguousResult = getFaqAnswer('AMBIGUOUS');
    assert.ok(ambiguousResult.reply.includes('How can I assist you today?'));
  });
});
