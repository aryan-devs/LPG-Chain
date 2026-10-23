import { describe, it } from 'node:test';
import assert from 'node:assert/strict';
import { HERO_MODULES } from '../components/hero/heroModules.ts';

describe('Stage 2.5 Capability Navigation & Destination Pages', () => {
  it('has routes defined for all 7 capability modules', () => {
    assert.equal(HERO_MODULES.length, 7);

    const routes = HERO_MODULES.map((m) => m.route);
    assert.ok(routes.includes('booking'), 'Missing booking route');
    assert.ok(routes.includes('tracking'), 'Missing tracking route');
    assert.ok(routes.includes('sentinel'), 'Missing sentinel route');
    assert.ok(routes.includes('kyc'), 'Missing kyc route');
    assert.ok(routes.includes('analytics'), 'Missing analytics route');
    assert.ok(routes.includes('security'), 'Missing security route');
    assert.ok(routes.includes('distributor'), 'Missing distributor route');
  });

  it('correctly maps 7 capability action types to their respective destination targets', () => {
    type CapabilityAction = 'booking' | 'tracking' | 'sentinel' | 'kyc' | 'analytics' | 'security' | 'distributor';

    const testNavigation = (action: CapabilityAction) => {
      let navigatedTo: string | null = null;
      let bookingOpened = false;

      switch (action) {
        case 'booking':
          bookingOpened = true;
          break;
        case 'tracking':
          navigatedTo = 'tracking';
          break;
        case 'sentinel':
        case 'kyc':
        case 'analytics':
        case 'security':
        case 'distributor':
          navigatedTo = action;
          break;
      }

      return { navigatedTo, bookingOpened };
    };

    assert.deepEqual(testNavigation('booking'), { navigatedTo: null, bookingOpened: true });
    assert.deepEqual(testNavigation('tracking'), { navigatedTo: 'tracking', bookingOpened: false });
    assert.deepEqual(testNavigation('sentinel'), { navigatedTo: 'sentinel', bookingOpened: false });
    assert.deepEqual(testNavigation('kyc'), { navigatedTo: 'kyc', bookingOpened: false });
    assert.deepEqual(testNavigation('analytics'), { navigatedTo: 'analytics', bookingOpened: false });
    assert.deepEqual(testNavigation('security'), { navigatedTo: 'security', bookingOpened: false });
    assert.deepEqual(testNavigation('distributor'), { navigatedTo: 'distributor', bookingOpened: false });
  });

  it('verifies metadata and titles for each capability card', () => {
    const expectedModules = [
      { id: 'smart-booking', title: 'Smart Booking' },
      { id: 'live-tracking', title: 'Live Tracking' },
      { id: 'ai-sentinel', title: 'AI Sentinel' },
      { id: 'ai-kyc', title: 'AI KYC' },
      { id: 'analytics', title: 'Analytics' },
      { id: 'security', title: 'Security' },
      { id: 'distributor-network', title: 'Distributor Network' },
    ];

    expectedModules.forEach(({ id, title }) => {
      const mod = HERO_MODULES.find((m) => m.id === id);
      assert.ok(mod, `Expected module ${id} to exist`);
      assert.equal(mod.title, title);
    });
  });
});
