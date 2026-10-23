import { describe, it } from 'node:test';
import assert from 'node:assert/strict';
import { HERO_MODULES } from '../components/hero/heroModules.ts';

describe('Hero Module Data & Ecosystem Integrity', () => {
  it('defines exactly 7 core supply chain modules', () => {
    assert.equal(HERO_MODULES.length, 7);
  });

  it('contains the 3 designated initial hover preview capabilities', () => {
    const previewModules = HERO_MODULES.filter((m) => m.initialPreview);
    assert.equal(previewModules.length, 3);

    const previewIds = previewModules.map((m) => m.id);
    assert.ok(previewIds.includes('smart-booking'));
    assert.ok(previewIds.includes('live-tracking'));
    assert.ok(previewIds.includes('ai-sentinel'));
  });

  it('contains the 4 core expansion modules', () => {
    const coreExpansionModules = HERO_MODULES.filter((m) => !m.initialPreview);
    assert.equal(coreExpansionModules.length, 4);

    const expansionIds = coreExpansionModules.map((m) => m.id);
    assert.ok(expansionIds.includes('ai-kyc'));
    assert.ok(expansionIds.includes('analytics'));
    assert.ok(expansionIds.includes('security'));
    assert.ok(expansionIds.includes('distributor-network'));
  });

  it('ensures each module has required title, subtitle, description, and specs', () => {
    HERO_MODULES.forEach((mod) => {
      assert.ok(mod.id && mod.id.length > 0, `Module missing id`);
      assert.ok(mod.title && mod.title.length > 0, `Module ${mod.id} missing title`);
      assert.ok(mod.subtitle && mod.subtitle.length > 0, `Module ${mod.id} missing subtitle`);
      assert.ok(mod.description && mod.description.length > 0, `Module ${mod.id} missing description`);
      assert.ok(mod.icon !== undefined, `Module ${mod.id} missing icon`);
      assert.ok(['red', 'orange', 'cyan', 'emerald'].includes(mod.accent), `Invalid accent for ${mod.id}`);
      assert.ok(Array.isArray(mod.technicalSpecs) && mod.technicalSpecs.length >= 2, `Module ${mod.id} lacks specs`);
    });
  });

  it('verifies navigation route mapping for live tracking and booking', () => {
    const trackingMod = HERO_MODULES.find((m) => m.id === 'live-tracking');
    assert.equal(trackingMod?.route, 'tracking');

    const bookingMod = HERO_MODULES.find((m) => m.id === 'smart-booking');
    assert.equal(bookingMod?.route, 'booking');
  });

  it('verifies orbital offsets for desktop placement', () => {
    HERO_MODULES.forEach((mod) => {
      assert.ok(Math.abs(mod.orbitalOffset.x) <= 60, `Offset x out of bounds for ${mod.id}`);
      assert.ok(Math.abs(mod.orbitalOffset.y) <= 60, `Offset y out of bounds for ${mod.id}`);
    });
  });

  it('ensures compact single-line subtitles for all capability cards', () => {
    HERO_MODULES.forEach((mod) => {
      assert.ok(mod.subtitle.length <= 40, `Subtitle for ${mod.id} is too verbose for compact cards: "${mod.subtitle}"`);
      assert.ok(!mod.subtitle.includes('\n'), `Subtitle for ${mod.id} contains line breaks`);
    });
  });

  it('guarantees clean separation between hover preview and expansion sets', () => {
    const previewIds = new Set(HERO_MODULES.filter((m) => m.initialPreview).map((m) => m.id));
    const expansionIds = new Set(HERO_MODULES.filter((m) => !m.initialPreview).map((m) => m.id));

    assert.equal(previewIds.size, 3);
    assert.equal(expansionIds.size, 4);

    // No overlap
    previewIds.forEach((id) => {
      assert.ok(!expansionIds.has(id), `Duplicate ID in preview and expansion: ${id}`);
    });
  });
});
