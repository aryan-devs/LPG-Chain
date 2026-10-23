import { 
  ShieldAlert, 
  Truck, 
  Calendar, 
  UserCheck, 
  BarChart3, 
  Lock, 
  Network
} from 'lucide-react';
import type { LucideIcon } from 'lucide-react';

export type ModuleAccent = 'red' | 'orange' | 'cyan' | 'emerald';

export interface HeroModuleData {
  id: string;
  title: string;
  subtitle: string;
  description: string;
  iconName: string;
  icon: LucideIcon;
  accent: ModuleAccent;
  category: string;
  initialPreview: boolean; // Shown on cylinder hover
  coreSequenceOrder: number; // Order of emergence when Core is activated
  // Desktop orbital coordinates relative to cylinder center (% offset from center)
  orbitalOffset: {
    x: number; // -100 to 100 %
    y: number; // -100 to 100 %
  };
  route: 'tracking' | 'booking' | 'sentinel' | 'kyc' | 'analytics' | 'security' | 'distributor';
  technicalSpecs: {
    label: string;
    value: string;
  }[];
}

export const HERO_MODULES: HeroModuleData[] = [
  // ── 3 Initial Hover Capabilities (Matching V2 Reference Design) ──
  {
    id: 'live-tracking',
    title: 'Live Tracking',
    subtitle: 'Sub-meter IoT GPS telemetry',
    description: 'Sub-second GPS vehicle position, route progression, and live geofence delivery status.',
    iconName: 'Truck',
    icon: Truck,
    accent: 'red',
    category: 'Logistics',
    initialPreview: true,
    coreSequenceOrder: 1,
    orbitalOffset: { x: -28, y: -20 },
    route: 'tracking',
    technicalSpecs: [
      { label: 'GPS Refresh Rate', value: '1,000ms WebSocket' },
      { label: 'Route Engine', value: 'Live Map Telemetry' },
      { label: 'Protocol', value: 'STOMP over WSS' },
    ],
  },
  {
    id: 'smart-booking',
    title: 'Smart Booking',
    subtitle: '1-Tap instant refill dispatch',
    description: 'Predictive consumption refills, 1-click customer booking, and automated distributor dispatch.',
    iconName: 'Calendar',
    icon: Calendar,
    accent: 'red',
    category: 'Booking',
    initialPreview: true,
    coreSequenceOrder: 2,
    orbitalOffset: { x: -28, y: 12 },
    route: 'booking',
    technicalSpecs: [
      { label: 'Dispatch Latency', value: '< 1.8s' },
      { label: 'Predictive Window', value: '48h prior to depletion' },
      { label: 'Allocation Method', value: 'Geospatial Nearest Hub' },
    ],
  },
  {
    id: 'ai-sentinel',
    title: 'AI Sentinel',
    subtitle: 'Autonomous leak & safety grid',
    description: 'Autonomous IoT sensory validation monitoring pressure, temperature, and zero-leak threshold.',
    iconName: 'ShieldAlert',
    icon: ShieldAlert,
    accent: 'red',
    category: 'Safety',
    initialPreview: true,
    coreSequenceOrder: 3,
    orbitalOffset: { x: 28, y: -20 },
    route: 'sentinel',
    technicalSpecs: [
      { label: 'Sensor Polling', value: 'Micro-watt BLE & NB-IoT' },
      { label: 'Detection Threshold', value: '0.05% LEL Hydrocarbon' },
      { label: 'Emergency Cutoff', value: 'Sub-second Solenoid Valve' },
    ],
  },

  // ── 4 Additional Modules Revealed on Core Activation ──
  {
    id: 'ai-kyc',
    title: 'AI KYC',
    subtitle: 'Biometric consumer verification',
    description: 'Biometric & national ID automated verification preventing commercial diversion and subsidy leakage.',
    iconName: 'UserCheck',
    icon: UserCheck,
    accent: 'cyan',
    category: 'Compliance',
    initialPreview: false,
    coreSequenceOrder: 4,
    orbitalOffset: { x: 29, y: -2 },
    route: 'kyc',
    technicalSpecs: [
      { label: 'Match Confidence', value: '99.98% Automated' },
      { label: 'Subsidy Cross-Check', value: 'Deduplicated National Registry' },
      { label: 'Audit Trail', value: 'Immutable Cryptographic Log' },
    ],
  },
  {
    id: 'analytics',
    title: 'Analytics',
    subtitle: 'Demand forecasting insights',
    description: 'Regional supply forecasting, cylinder turnaround metrics, and predictive stock replenishment.',
    iconName: 'BarChart3',
    icon: BarChart3,
    accent: 'orange',
    category: 'Analytics',
    initialPreview: false,
    coreSequenceOrder: 5,
    orbitalOffset: { x: 29, y: 16 },
    route: 'analytics',
    technicalSpecs: [
      { label: 'Forecasting Horizon', value: '14-Day Demand Model' },
      { label: 'Turnaround Rate', value: '3.2 Days Avg. Cycle' },
      { label: 'Depot Stock Alerts', value: 'Auto-Trigger at 15%' },
    ],
  },
  {
    id: 'security',
    title: 'Security',
    subtitle: 'Tamper-evident seals & audit',
    description: 'Cryptographic digital seal pairing ensuring zero contamination or gas pilferage across transit.',
    iconName: 'Lock',
    icon: Lock,
    accent: 'red',
    category: 'Security',
    initialPreview: false,
    coreSequenceOrder: 6,
    orbitalOffset: { x: -28, y: 30 },
    route: 'security',
    technicalSpecs: [
      { label: 'Seal Verification', value: 'NFC + QR Dynamic Token' },
      { label: 'Weight Variance', value: '< 20g Tolerance Alert' },
      { label: 'Custody Handshake', value: 'Two-Party Cryptographic Sign' },
    ],
  },
  {
    id: 'distributor-network',
    title: 'Distributor Network',
    subtitle: '1,200+ certified regional hubs',
    description: 'Multi-node hub-and-spoke distribution linking bulk bottling plants to neighborhood retail agents.',
    iconName: 'Network',
    icon: Network,
    accent: 'emerald',
    category: 'Network',
    initialPreview: false,
    coreSequenceOrder: 7,
    orbitalOffset: { x: 29, y: 32 },
    route: 'distributor',
    technicalSpecs: [
      { label: 'Hub Coordination', value: 'Distributed Real-Time Sync' },
      { label: 'Route Optimization', value: 'Dynamic Traffic-Aware ETA' },
      { label: 'Bottling Throughput', value: 'Monitored Weighbridge Ingestion' },
    ],
  },
];
