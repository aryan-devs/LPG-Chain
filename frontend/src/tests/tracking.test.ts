import { describe, it } from 'node:test';
import assert from 'node:assert/strict';
import { isValidCoordinate, parseTrackingEvent } from '../types/tracking.ts';

describe('Tracking Types & Validation', () => {
  it('validates correct geographical coordinates', () => {
    assert.equal(isValidCoordinate(18.5204, 73.8567), true);
    assert.equal(isValidCoordinate(0, 0), true);
    assert.equal(isValidCoordinate(-90, -180), true);
    assert.equal(isValidCoordinate(90, 180), true);
  });

  it('rejects out-of-range or invalid coordinates', () => {
    assert.equal(isValidCoordinate(91, 73), false);
    assert.equal(isValidCoordinate(-91, 73), false);
    assert.equal(isValidCoordinate(18, 181), false);
    assert.equal(isValidCoordinate(18, -181), false);
    assert.equal(isValidCoordinate(NaN, 73), false);
    assert.equal(isValidCoordinate(18, NaN), false);
    assert.equal(isValidCoordinate(null, 73), false);
    assert.equal(isValidCoordinate(undefined, undefined), false);
    assert.equal(isValidCoordinate('18.52', '73.85'), false);
  });

  it('safely parses valid TrackingEvent payloads', () => {
    const rawPayload = {
      deliveryId: 10,
      vehicleId: 1,
      vehicleNumber: 'MH-12-AB-1234',
      latitude: 18.5204,
      longitude: 73.8567,
      speed: 42.5,
      heading: 90.0,
      recordedAt: '2026-10-04T12:00:00Z',
    };

    const event = parseTrackingEvent(rawPayload);
    assert.ok(event !== null);
    assert.equal(event?.deliveryId, 10);
    assert.equal(event?.vehicleId, 1);
    assert.equal(event?.vehicleNumber, 'MH-12-AB-1234');
    assert.equal(event?.latitude, 18.5204);
    assert.equal(event?.longitude, 73.8567);
    assert.equal(event?.speed, 42.5);
    assert.equal(event?.heading, 90.0);
    assert.equal(event?.recordedAt, '2026-10-04T12:00:00Z');
  });

  it('parses numeric string representations in payloads', () => {
    const rawPayload = {
      deliveryId: '10',
      vehicleId: '2',
      vehicleNumber: 'MH-14-XY-9999',
      latitude: '18.6298',
      longitude: '73.7997',
      speed: '35',
      heading: '180',
      recordedAt: '2026-10-04T12:30:00Z',
    };

    const event = parseTrackingEvent(rawPayload);
    assert.ok(event !== null);
    assert.equal(event?.deliveryId, 10);
    assert.equal(event?.vehicleId, 2);
    assert.equal(event?.latitude, 18.6298);
    assert.equal(event?.longitude, 73.7997);
    assert.equal(event?.speed, 35);
    assert.equal(event?.heading, 180);
  });

  it('returns null for malformed or incomplete tracking events', () => {
    assert.equal(parseTrackingEvent(null), null);
    assert.equal(parseTrackingEvent(undefined), null);
    assert.equal(parseTrackingEvent('string-event'), null);

    // Missing required vehicleNumber
    assert.equal(
      parseTrackingEvent({
        deliveryId: 10,
        vehicleId: 1,
        vehicleNumber: '',
        latitude: 18.52,
        longitude: 73.85,
      }),
      null
    );

    // Invalid coordinates
    assert.equal(
      parseTrackingEvent({
        deliveryId: 10,
        vehicleId: 1,
        vehicleNumber: 'MH-12-AB-1234',
        latitude: 999,
        longitude: 73.85,
      }),
      null
    );
  });
});
