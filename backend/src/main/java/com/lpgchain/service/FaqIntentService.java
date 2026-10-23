package com.lpgchain.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Pattern;

@Service
public class FaqIntentService {

    private static final Logger log = LoggerFactory.getLogger(FaqIntentService.class);

    @Value("${lpgchain.support.phone:1800-233-3555}")
    private String supportPhoneConfig;

    public enum FaqIntent {
        BOOKING_HOWTO,
        BOOKING_STATUS,
        TRACKING_DELIVERY,
        PAYMENT_HELP,
        KYC_HELP,
        COMPLAINT_HOWTO,
        SAFETY_GUIDELINES,
        CUSTOMER_SUPPORT,
        AMBIGUOUS_GREETING,
        OUT_OF_SCOPE
    }

    public static class IntentMatchResult {
        private final FaqIntent intent;
        private final boolean accountSpecific;
        private final double confidence;
        private final String matchedRule;

        public IntentMatchResult(FaqIntent intent, boolean accountSpecific, double confidence, String matchedRule) {
            this.intent = intent;
            this.accountSpecific = accountSpecific;
            this.confidence = confidence;
            this.matchedRule = matchedRule;
        }

        public FaqIntent getIntent() { return intent; }
        public boolean isAccountSpecific() { return accountSpecific; }
        public double confidence() { return confidence; }
        public String getMatchedRule() { return matchedRule; }
    }

    public static class FaqAnswer {
        private final String reply;
        private final String actionTab;
        private final String actionLabel;
        private final String supportPhone;
        private final List<String> suggestions;

        public FaqAnswer(String reply, String actionTab, String actionLabel, String supportPhone, List<String> suggestions) {
            this.reply = reply;
            this.actionTab = actionTab;
            this.actionLabel = actionLabel;
            this.supportPhone = supportPhone;
            this.suggestions = suggestions;
        }

        public String getReply() { return reply; }
        public String getActionTab() { return actionTab; }
        public String getActionLabel() { return actionLabel; }
        public String getSupportPhone() { return supportPhone; }
        public List<String> getSuggestions() { return suggestions; }
    }

    private static class FaqDefinition {
        final FaqIntent intent;
        final List<String> canonicalPhrases;
        final Set<String> coreKeywords;
        final Set<String> contextualKeywords;

        FaqDefinition(FaqIntent intent, List<String> canonicalPhrases, Set<String> coreKeywords, Set<String> contextualKeywords) {
            this.intent = intent;
            this.canonicalPhrases = canonicalPhrases;
            this.coreKeywords = coreKeywords;
            this.contextualKeywords = contextualKeywords;
        }
    }

    private final List<FaqDefinition> definitions = new ArrayList<>();
    private final Set<String> greetingTokens = new HashSet<>(Arrays.asList(
            "hi", "hello", "hey", "help", "greetings", "good morning", "good afternoon", "good evening"
    ));

    private final Set<String> outOfScopeIndicators = new HashSet<>(Arrays.asList(
            "weather", "recipe", "python", "president", "movie", "cricket", "football", "stocks",
            "crypto", "bitcoin", "france", "paris", "capital", "hotel booking", "flight", "train ticket",
            "pizza", "burger", "astronomy", "homework", "poem", "joke"
    ));

    public FaqIntentService() {
        initDefinitions();
    }

    public String getConfiguredSupportPhone() {
        if (supportPhoneConfig == null || supportPhoneConfig.trim().isEmpty() ||
                "none".equalsIgnoreCase(supportPhoneConfig.trim()) || "null".equalsIgnoreCase(supportPhoneConfig.trim())) {
            return null;
        }
        return supportPhoneConfig.trim();
    }

    private void initDefinitions() {
        // 1. HOW TO BOOK A CYLINDER
        definitions.add(new FaqDefinition(
                FaqIntent.BOOKING_HOWTO,
                Arrays.asList(
                        "how to book", "book cylinder", "book a cylinder", "book lpg", "order refill",
                        "book refill", "how can i order a cylinder", "steps to book", "how do i book",
                        "booking cylinder", "buy gas cylinder", "new refill", "order a cylinder",
                        "cylinder booking", "want to book", "need a cylinder", "order gas"
                ),
                new HashSet<>(Arrays.asList("book", "booking", "order", "refill", "procure")),
                new HashSet<>(Arrays.asList("cylinder", "gas", "lpg", "refill", "domestic", "commercial", "steps", "how", "quota"))
        ));

        // 2. HOW TO CHECK BOOKING STATUS
        definitions.add(new FaqDefinition(
                FaqIntent.BOOKING_STATUS,
                Arrays.asList(
                        "check booking status", "booking status", "my order status", "where is my booking",
                        "where is my order", "track my booking", "check my refill", "status of my cylinder",
                        "order status", "booking details", "check status", "refill status", "booking history",
                        "did my booking go through", "is my cylinder booked"
                ),
                new HashSet<>(Arrays.asList("status", "where", "check")),
                new HashSet<>(Arrays.asList("booking", "order", "refill", "cylinder", "placed", "latest"))
        ));

        // 3. HOW TO TRACK A DISPATCHED DELIVERY
        definitions.add(new FaqDefinition(
                FaqIntent.TRACKING_DELIVERY,
                Arrays.asList(
                        "track delivery", "track dispatched delivery", "where is my cylinder",
                        "delivery tracking", "live tracking", "gps location", "driver location",
                        "delivery status", "out for delivery", "delivery vehicle", "track driver",
                        "live map", "delivery otp", "dispatch status", "when will it arrive", "delivery truck"
                ),
                new HashSet<>(Arrays.asList("track", "tracking", "dispatch", "dispatched", "gps", "driver", "vehicle")),
                new HashSet<>(Arrays.asList("delivery", "location", "arrival", "map", "otp", "route", "van", "truck"))
        ));

        // 4. PAYMENT METHODS AND PAYMENT-STATUS HELP
        definitions.add(new FaqDefinition(
                FaqIntent.PAYMENT_HELP,
                Arrays.asList(
                        "payment methods", "payment status", "how to pay", "pay bill", "payment options",
                        "cash on delivery", "cod", "upi payment", "razorpay", "qr code", "dbtl subsidy",
                        "subsidy help", "subsidy status", "pahal subsidy", "payment help", "billing help",
                        "invoice details", "how much does it cost", "cylinder price", "subsidy amount"
                ),
                new HashSet<>(Arrays.asList("payment", "pay", "bill", "billing", "invoice", "subsidy", "dbtl", "pahal", "cod", "upi", "razorpay")),
                new HashSet<>(Arrays.asList("method", "options", "price", "cost", "status", "cash", "qr", "receipt", "account"))
        ));

        // 5. REQUIRED KYC DOCUMENTS AND DOCUMENT-UPLOAD HELP
        definitions.add(new FaqDefinition(
                FaqIntent.KYC_HELP,
                Arrays.asList(
                        "kyc documents", "required kyc documents", "document upload", "what documents required",
                        "kyc requirements", "upload aadhaar", "upload ration card", "kyc verification",
                        "how to upload kyc", "documents needed", "kyc help", "upload documents", "document requirements",
                        "proof of identity", "proof of address", "passbook upload"
                ),
                new HashSet<>(Arrays.asList("kyc", "document", "documents", "aadhaar", "aadhar", "ration", "passbook", "upload")),
                new HashSet<>(Arrays.asList("required", "needed", "identity", "address", "verification", "proof", "photo", "format"))
        ));

        // 6. HOW TO RAISE A COMPLAINT
        definitions.add(new FaqDefinition(
                FaqIntent.COMPLAINT_HOWTO,
                Arrays.asList(
                        "raise complaint", "how to raise complaint", "file complaint", "register complaint",
                        "report issue", "file grievance", "cylinder leakage complaint", "delivery delay complaint",
                        "overcharging complaint", "raise a grievance", "submit complaint", "log complaint",
                        "broken seal", "damaged cylinder", "complain"
                ),
                new HashSet<>(Arrays.asList("complaint", "complaints", "grievance", "grievances", "issue", "report")),
                new HashSet<>(Arrays.asList("raise", "file", "register", "delay", "leakage", "overcharging", "damaged", "dispute", "ticket"))
        ));

        // 7. LPG SAFETY GUIDELINES AND EMERGENCY GUIDANCE
        definitions.add(new FaqDefinition(
                FaqIntent.SAFETY_GUIDELINES,
                Arrays.asList(
                        "safety guidelines", "gas leak emergency", "lpg safety", "emergency guidance",
                        "smell gas", "gas leakage protocol", "cylinder weight check", "tare weight",
                        "safety tips", "fire emergency", "1906 helpline", "what to do if gas leaks",
                        "emergency number", "gas smell", "cylinder explosion", "regulator knob"
                ),
                new HashSet<>(Arrays.asList("safety", "emergency", "leak", "leakage", "smell", "fire", "1906", "tare", "weight")),
                new HashSet<>(Arrays.asList("guidelines", "protocol", "regulator", "valve", "sparks", "switch", "ventilation", "seal"))
        ));

        // 8. HOW TO CONTACT CUSTOMER SUPPORT
        definitions.add(new FaqDefinition(
                FaqIntent.CUSTOMER_SUPPORT,
                Arrays.asList(
                        "contact customer support", "customer support", "customer care", "contact support",
                        "helpline number", "support phone", "how to contact", "call support", "support desk",
                        "toll free", "support number", "speak to agent", "reach support", "helpline", "help line"
                ),
                new HashSet<>(Arrays.asList("support", "contact", "helpline", "helpdesk", "care", "call", "tollfree")),
                new HashSet<>(Arrays.asList("phone", "number", "customer", "agent", "executive", "reach", "talk", "desk"))
        ));
    }

    public String normalize(String input) {
        if (input == null) return "";
        String s = input.toLowerCase().trim();
        // Replace non-alphanumeric (except whitespace)
        s = s.replaceAll("[^a-z0-9\\s]", " ");
        // Collapse spaces
        s = s.replaceAll("\\s+", " ").trim();
        return s;
    }

    public IntentMatchResult matchIntent(String rawQuery, String explicitIntentId) {
        if (explicitIntentId != null && !explicitIntentId.trim().isEmpty()) {
            try {
                FaqIntent parsed = FaqIntent.valueOf(explicitIntentId.trim().toUpperCase());
                String normalized = normalize(rawQuery);
                List<String> tokens = Arrays.asList(normalized.split("\\s+"));
                boolean isAccount = isAccountSpecificQuery(normalized, tokens);
                return new IntentMatchResult(parsed, isAccount, 1.0, "EXPLICIT_INTENT_ID: " + explicitIntentId);
            } catch (IllegalArgumentException ignored) {}
        }
        return matchIntent(rawQuery);
    }

    public IntentMatchResult matchIntent(String rawQuery) {
        if (rawQuery == null || rawQuery.trim().isEmpty()) {

            return new IntentMatchResult(FaqIntent.AMBIGUOUS_GREETING, false, 1.0, "EMPTY_QUERY");
        }

        String normalized = normalize(rawQuery);
        List<String> tokens = Arrays.asList(normalized.split("\\s+"));

        // Check for ambiguous greetings / single-word greeting tokens
        if (tokens.size() <= 2 && tokens.stream().anyMatch(greetingTokens::contains)) {
            return new IntentMatchResult(FaqIntent.AMBIGUOUS_GREETING, false, 1.0, "GREETING");
        }

        // Check if query contains strong out-of-scope indicators without any LPG keywords
        boolean hasOutOfScopeTerm = tokens.stream().anyMatch(outOfScopeIndicators::contains);
        boolean hasLpgContext = tokens.stream().anyMatch(t ->
                t.equals("lpg") || t.equals("cylinder") || t.equals("gas") || t.equals("refill") ||
                t.equals("subsidy") || t.equals("booking") || t.equals("kyc") || t.equals("1906")
        );
        if (hasOutOfScopeTerm && !hasLpgContext) {
            return new IntentMatchResult(FaqIntent.OUT_OF_SCOPE, false, 0.0, "OUT_OF_SCOPE_KEYWORD");
        }

        // Determine if asking for personal account records
        boolean isAccountSpecific = isAccountSpecificQuery(normalized, tokens);

        // 1. Direct canonical phrase match
        for (FaqDefinition def : definitions) {
            for (String phrase : def.canonicalPhrases) {
                if (normalized.contains(phrase)) {
                    // Check if a phrase matches status check vs how-to
                    if (def.intent == FaqIntent.BOOKING_HOWTO && isAccountSpecific) {
                        return new IntentMatchResult(FaqIntent.BOOKING_STATUS, true, 1.0, "CANONICAL_PHRASE: " + phrase);
                    }
                    return new IntentMatchResult(def.intent, isAccountSpecific, 1.0, "CANONICAL_PHRASE: " + phrase);
                }
            }
        }

        // 2. Token overlap & confidence scoring
        FaqDefinition bestDef = null;
        double bestScore = 0.0;

        for (FaqDefinition def : definitions) {
            double score = calculateConfidence(tokens, def);
            if (score > bestScore) {
                bestScore = score;
                bestDef = def;
            }
        }

        // Confidence threshold = 0.45
        if (bestDef != null && bestScore >= 0.45) {
            FaqIntent resolvedIntent = bestDef.intent;
            if (resolvedIntent == FaqIntent.BOOKING_HOWTO && isAccountSpecific) {
                resolvedIntent = FaqIntent.BOOKING_STATUS;
            }
            return new IntentMatchResult(resolvedIntent, isAccountSpecific, bestScore, "CONFIDENCE_SCORE: " + bestScore);
        }

        // If intent is unclear or no sufficient confidence, treat as OUT_OF_SCOPE
        return new IntentMatchResult(FaqIntent.OUT_OF_SCOPE, false, bestScore, "LOW_CONFIDENCE_FALLBACK");
    }

    private boolean isAccountSpecificQuery(String normalized, List<String> tokens) {
        if (normalized.contains("where is my") || normalized.contains("check my") ||
                normalized.contains("status of my") || normalized.contains("my booking") ||
                normalized.contains("my order") || normalized.contains("my cylinder") ||
                normalized.contains("my delivery") || normalized.contains("my payment") ||
                normalized.contains("my kyc") || normalized.contains("my documents") ||
                normalized.contains("my complaint") || normalized.contains("did i book") ||
                normalized.contains("did my payment") || normalized.contains("am i approved")) {
            return true;
        }
        return tokens.contains("my") || tokens.contains("mine");
    }

    private double calculateConfidence(List<String> tokens, FaqDefinition def) {
        int coreMatches = 0;
        int contextMatches = 0;

        for (String token : tokens) {
            if (def.coreKeywords.contains(token)) {
                coreMatches++;
            } else if (def.contextualKeywords.contains(token)) {
                contextMatches++;
            }
        }

        // Must have at least 1 core keyword match to be confident
        if (coreMatches == 0) return 0.0;

        double score = (coreMatches * 0.4) + (contextMatches * 0.2);
        return Math.min(score, 1.0);
    }

    public FaqAnswer getGeneralAnswer(FaqIntent intent) {
        String phone = getConfiguredSupportPhone();

        switch (intent) {
            case BOOKING_HOWTO:
                return new FaqAnswer(
                        "To book an LPG cylinder with LPG-Chain, follow these simple steps:\n\n" +
                        "1. **Select Cylinder Category**:\n" +
                        "   • **Domestic 14.2 kg** — ₹853.00 (Eligible for ₹200 direct DBTL government subsidy credited to your bank account).\n" +
                        "   • **Domestic 5 kg Mini** — ₹340.00 (Ideal for immediate backup or small households).\n" +
                        "   • **Commercial 19 kg** — ₹1,850.00 (For commercial and hospitality establishments).\n" +
                        "2. **Confirm Delivery Address**: Select from your saved household delivery addresses.\n" +
                        "3. **Choose Payment Mode**: Pay securely via Demo Online Checkout (UPI / Cards / NetBanking) or choose Cash on Delivery (COD).\n" +
                        "4. **Instant Confirmation**: Receive your Booking Reference Number and live order status.\n\n" +
                        "You can start your order anytime from the Book Cylinder view.",
                        "book-cylinder",
                        "Go to Book Cylinder",
                        phone,
                        Arrays.asList("Check my booking status", "Payment methods available", "How to track delivery")
                );

            case BOOKING_STATUS:
                return new FaqAnswer(
                        "Here is how to check your booking status:\n\n" +
                        "• **In the Portal**: Navigate to the **Bookings** section to view current status (`PENDING`, `CONFIRMED`, `OUT_FOR_DELIVERY`, or `DELIVERED`).\n" +
                        "• **Live Updates**: Once assigned to a delivery vehicle, live GPS tracking and estimated arrival time activate under Track Delivery.\n" +
                        "• **Account Verification**: To look up your live active orders, please ensure you are logged in to your account.",
                        "bookings",
                        "View Bookings",
                        phone,
                        Arrays.asList("How to book an LPG cylinder", "How to track a dispatched delivery", "Payment methods available")
                );

            case TRACKING_DELIVERY:
                return new FaqAnswer(
                        "Here is how live delivery tracking works on LPG-Chain:\n\n" +
                        "1. **Dispatch Notification**: When your distributor assigns a vehicle and driver, your order enters `OUT_FOR_DELIVERY` status.\n" +
                        "2. **Real-Time GPS Map**: Open the **Track Delivery** view to see vehicle movements, driver contact details, and distance to your address.\n" +
                        "3. **Delivery OTP Verification**: For security and subsidy authorization, share your 4-digit Delivery Handover OTP with the delivery executive upon cylinder handover.\n\n" +
                        "To view live GPS tracking for your active order, please visit the Track Delivery view.",
                        "track-delivery",
                        "Track Delivery",
                        phone,
                        Arrays.asList("Check my booking status", "LPG safety guidelines", "How to contact customer support")
                );

            case PAYMENT_HELP:
                return new FaqAnswer(
                        "LPG-Chain supports the following verified payment methods and subsidy policies:\n\n" +
                        "• **Demo Online Checkout (Razorpay Simulation)**: Pay securely via simulated UPI, Credit/Debit cards, or Net Banking with immediate payment receipt.\n" +
                        "• **Cash on Delivery (COD)**: Pay cash or scan the driver's dynamic QR code upon physical cylinder handover.\n" +
                        "• **DBTL / PAHAL Subsidy**: A direct ₹200 government subsidy is credited to your registered bank account for domestic 14.2 kg refills following OTP delivery verification.\n\n" +
                        "You can review past transaction history and download invoices under the Billing & Payments section.",
                        "billing",
                        "Billing & Payments",
                        phone,
                        Arrays.asList("How to book an LPG cylinder", "Check my booking status", "Required KYC documents")
                );

            case KYC_HELP:
                return new FaqAnswer(
                        "To comply with LPG safety regulations and qualify for direct DBTL subsidies, customers must submit 4 required KYC documents:\n\n" +
                        "1. **Proof of Identity (PoI)**: Aadhaar Card, Voter ID, or Passport.\n" +
                        "2. **Proof of Address (PoA)**: Ration Card or Electricity Bill.\n" +
                        "3. **Bank Account Proof**: Bank Passbook or Cancelled Cheque (required for direct subsidy deposit).\n" +
                        "4. **Passport Photograph**: Recent photograph.\n\n" +
                        "**Upload Guidelines**:\n" +
                        "• Accepted formats: PDF, JPG, or PNG (up to 10 MB per file).\n" +
                        "• Submit your documents under the **KYC Documents** view for distributor verification.",
                        "kyc-documents",
                        "Go to KYC Documents",
                        phone,
                        Arrays.asList("Check DBTL subsidy status", "How to book an LPG cylinder", "How to contact customer support")
                );

            case COMPLAINT_HOWTO:
                return new FaqAnswer(
                        "If you experience any service irregularity, you can raise an official complaint through LPG-Chain:\n\n" +
                        "1. Navigate to the **Help & Support** tab in your portal.\n" +
                        "2. Select the issue category:\n" +
                        "   • **Gas Leakage / Safety Issue**: *Dial 1906 immediately!*\n" +
                        "   • **Delayed Delivery**\n" +
                        "   • **Cylinder Shortage / Underweight**\n" +
                        "   • **Overcharging / Pricing Dispute**\n" +
                        "   • **Defective Valve or Seal**\n" +
                        "3. Provide a brief description and submit. A unique Complaint Ticket ID is generated for real-time resolution tracking.\n\n" +
                        "Distributors are required to resolve consumer grievances within 24 to 48 hours.",
                        "support",
                        "Raise a Complaint",
                        phone,
                        Arrays.asList("LPG safety guidelines", "How to contact customer support", "Check my booking status")
                );

            case SAFETY_GUIDELINES:
                return new FaqAnswer(
                        "🚨 **LPG Safety Guidelines & Emergency Guidance**:\n\n" +
                        "**Immediate Protocol in Case of Gas Smell or Leakage**:\n" +
                        "1. **NO Sparks or Flames**: Do NOT turn any electrical switches ON or OFF. Do not light matches or incense.\n" +
                        "2. **Close Cylinder Regulator**: Turn the regulator knob clockwise to the OFF position immediately.\n" +
                        "3. **Ventilate the Premises**: Open all doors and windows to disperse accumulated gas (LPG is heavier than air).\n" +
                        "4. **Emergency Helpline**: Evacuate everyone and call the **24/7 LPG Emergency Helpline at 1906** (Toll-Free) or Fire Services at 101.\n\n" +
                        "**Cylinder Inspection Tips**:\n" +
                        "• Verify tare weight stamped on cylinder stay plate. Net LPG weight for domestic cylinders is 14.2 kg.\n" +
                        "• Check that the safety cap seal is intact before accepting delivery from the driver.",
                        null,
                        null,
                        phone,
                        Arrays.asList("How to raise a complaint", "How to contact customer support", "How to book an LPG cylinder")
                );

            case CUSTOMER_SUPPORT:
                if (phone != null) {
                    return new FaqAnswer(
                            "You can reach LPG-Chain customer support through the following channels:\n\n" +
                            "• **Customer Support Helpline**: Call us at **" + phone + "** for booking, billing, KYC, and connection queries.\n" +
                            "• **24/7 LPG Emergency Helpline**: Dial **1906** (Toll-Free) immediately for gas leaks or fire emergencies.\n" +
                            "• **Help & Support Portal**: Raise an online ticket under the Help & Support tab for priority distributor assistance.\n\n" +
                            "Support hours: Monday to Saturday, 8:00 AM – 8:00 PM.",
                            "support",
                            "Visit Help & Support",
                            phone,
                            Arrays.asList("How to raise a complaint", "How to book an LPG cylinder", "LPG safety guidelines")
                    );
                } else {
                    return new FaqAnswer(
                            "You can reach LPG-Chain customer support through our portal and emergency channels:\n\n" +
                            "• **24/7 LPG Emergency Helpline**: Dial **1906** (Toll-Free) immediately for gas leaks or fire emergencies.\n" +
                            "• **Help & Support Portal**: Raise an online ticket under the Help & Support tab for priority distributor assistance.",
                            "support",
                            "Visit Help & Support",
                            null,
                            Arrays.asList("How to raise a complaint", "How to book an LPG cylinder", "LPG safety guidelines")
                    );
                }

            case AMBIGUOUS_GREETING:
                return new FaqAnswer(
                        "Hello! I am your LPG-Chain Operations Assistant. How can I assist you today?\n\n" +
                        "You can ask me about:\n" +
                        "• How to book an LPG cylinder\n" +
                        "• Checking booking and delivery status\n" +
                        "• Payment methods and DBTL subsidy\n" +
                        "• Required KYC documents & verification\n" +
                        "• How to raise a complaint\n" +
                        "• LPG safety guidelines & emergency guidance\n" +
                        "• How to contact customer support",
                        null,
                        null,
                        phone,
                        Arrays.asList("How to book an LPG cylinder", "How to check booking status", "LPG safety guidelines", "How to contact customer support")
                );

            case OUT_OF_SCOPE:
            default:
                if (phone != null) {
                    return new FaqAnswer(
                            "I apologize, but I cannot answer that particular question as it is outside the scope of LPG-Chain services. " +
                            "Please contact the LPG-Chain customer support team at " + phone + " for assistance.",
                            "support",
                            "Contact Support",
                            phone,
                            Arrays.asList("How to book an LPG cylinder", "How to check booking status", "LPG safety guidelines", "How to contact customer support")
                    );
                } else {
                    return new FaqAnswer(
                            "I couldn't find an answer to that question. Please contact the LPG-Chain support team for assistance.",
                            "support",
                            "Contact Support",
                            null,
                            Arrays.asList("How to book an LPG cylinder", "How to check booking status", "LPG safety guidelines", "How to contact customer support")
                    );
                }
        }
    }
}
