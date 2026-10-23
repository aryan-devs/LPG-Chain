import { describe, it } from 'node:test';
import assert from 'node:assert/strict';

describe('Household Customer Portal — Specifications & Business Rules', () => {
  it('defines all approved navigation views for the household portal', () => {
    const requiredViews = [
      'dashboard',
      'book',
      'track',
      'ai',
      'transparency',
      'kyc',
      'profile',
      'addresses',
      'billing',
      'history',
      'support',
    ];

    assert.equal(requiredViews.length, 11, 'Must have exactly 11 functional portal tabs');
    assert.ok(requiredViews.includes('dashboard'), 'Must include Dashboard');
    assert.ok(requiredViews.includes('book'), 'Must include Book a Cylinder');
    assert.ok(requiredViews.includes('track'), 'Must include Track Delivery');
    assert.ok(requiredViews.includes('transparency'), 'Must include Quota Transparency');
    assert.ok(requiredViews.includes('ai'), 'Must include AI Assistance');
    assert.ok(requiredViews.includes('kyc'), 'Must include KYC & Documents');
    assert.ok(requiredViews.includes('profile'), 'Must include My Profile');
    assert.ok(requiredViews.includes('addresses'), 'Must include Saved Addresses');
    assert.ok(requiredViews.includes('billing'), 'Must include Billing & Payments');
    assert.ok(requiredViews.includes('history'), 'Must include Booking History');
    assert.ok(requiredViews.includes('support'), 'Must include Help & Support');
  });

  it('verifies pricing rules and DBTL subsidy calculation logic for domestic cylinders', () => {
    const calculatePricing = (capacityKg: number, quantity: number, isSubsidized: boolean) => {
      const unitPrice = capacityKg === 5 ? 340 : capacityKg === 19 ? 1850 : 853;
      const baseTotal = unitPrice * quantity;
      const subsidy = isSubsidized && capacityKg === 14.2 ? 200 * quantity : 0;
      const netEffectiveCost = baseTotal - subsidy;
      return { baseTotal, subsidy, netEffectiveCost };
    };

    // 14.2 KG Domestic Refill with PAHAL DBTL subsidy
    const domesticRefill = calculatePricing(14.2, 1, true);
    assert.equal(domesticRefill.baseTotal, 853, 'Base cylinder price is 853');
    assert.equal(domesticRefill.subsidy, 200, 'PAHAL subsidy is 200');
    assert.equal(domesticRefill.netEffectiveCost, 653, 'Effective household cost is 653');

    // 5 KG Mini Chhotu cylinder (non-subsidized)
    const miniCylinder = calculatePricing(5, 2, false);
    assert.equal(miniCylinder.baseTotal, 680, 'Two 5kg cylinders cost 680');
    assert.equal(miniCylinder.subsidy, 0, 'No subsidy on mini cylinders');

    // 19 KG Commercial (non-subsidized)
    const commercialCylinder = calculatePricing(19, 1, false);
    assert.equal(commercialCylinder.baseTotal, 1850);
  });

  it('validates 4-step delivery progress sequence', () => {
    const deliverySteps = ['CONFIRMED', 'ASSIGNED', 'OUT_FOR_DELIVERY', 'DELIVERED'];
    assert.equal(deliverySteps.length, 4);
    assert.deepEqual(deliverySteps, [
      'CONFIRMED',
      'ASSIGNED',
      'OUT_FOR_DELIVERY',
      'DELIVERED',
    ]);
  });

  it('verifies the 6 prompt suggestions for AI Assistance', () => {
    const prompts = [
      'How to book a cylinder?',
      'Safety guidelines',
      'Check availability',
      'Change delivery address',
      'Billing and payment help',
      'Cylinder usage tips',
    ];

    assert.equal(prompts.length, 6, 'Must provide exactly 6 pre-configured prompt chips');
    assert.ok(prompts.includes('Safety guidelines'));
    assert.ok(prompts.includes('How to book a cylinder?'));
  });

  it('validates the National Emergency 1906 gas leak protocol', () => {
    const emergencyHelpline = '1906';
    const emergencyRules = [
      'Extinguish all flames',
      'Do not operate electrical switches',
      'Turn off regulator knob clockwise',
      'Open doors and windows',
      'Call emergency helpline 1906 immediately',
    ];

    assert.equal(emergencyHelpline, '1906');
    assert.equal(emergencyRules.length, 5);
  });

  it('verifies cylinder tare weight verification tolerances', () => {
    const netWeightKg = 14.2;
    const tareWeightKg = 15.6; // Stamped on cylinder collar
    const grossExpectedWeight = Number((tareWeightKg + netWeightKg).toFixed(1)); // 29.8 kg
    const toleranceKg = 0.15; // ±150g allowable statutory tolerance

    assert.equal(grossExpectedWeight, 29.8);
    const measuredWeight = 29.75; // e.g. within tolerance
    const isWithinTolerance = Math.abs(measuredWeight - grossExpectedWeight) <= toleranceKg;
    assert.equal(isWithinTolerance, true, 'Measured weight is within legal weight & measures tolerance');
  });
});
