import React, { useState, useMemo } from 'react';
import {
  Truck,
  Users,
  Eye,
} from 'lucide-react';
import {
  AdminDriverItem,
  AdminVehicleItem,
} from '../../../types/adminPortalTypes';
import { AdminTable, ColumnDef } from '../components/AdminTable';
import { AdminStatusBadge } from '../components/AdminStatusBadge';
import { AdminFilterBar, FilterDropdown } from '../components/AdminFilterBar';
import { AdminDetailModal } from '../components/AdminDetailModal';

const PREVIEW_DRIVERS: AdminDriverItem[] = [
  { id: 10, name: 'Suresh Patil', phone: '+91 97654 32109', licenseNumber: 'MH-04-2018-00912', distributorId: 1, distributorName: 'Apex Gas Distribution Agency', status: 'ON_DELIVERY', activeDeliveryId: 501, assignedVehicleReg: 'MH-04-AB-1234', completedDeliveriesTotal: 842, rating: 4.9, joinedDate: '2025-11-25' },
  { id: 11, name: 'Ramesh Sawant', phone: '+91 98333 77889', licenseNumber: 'MH-04-2019-11442', distributorId: 1, distributorName: 'Apex Gas Distribution Agency', status: 'AVAILABLE', assignedVehicleReg: 'MH-04-CD-5678', completedDeliveriesTotal: 615, rating: 4.8, joinedDate: '2026-01-10' },
  { id: 12, name: 'Ganesh More', phone: '+91 98220 55667', licenseNumber: 'MH-12-2020-88123', distributorId: 2, distributorName: 'Pune Green Gas Depot', status: 'ON_DELIVERY', activeDeliveryId: 502, assignedVehicleReg: 'MH-12-PQ-9081', completedDeliveriesTotal: 520, rating: 4.7, joinedDate: '2026-02-14' },
  { id: 13, name: 'Nitin Kamble', phone: '+91 98221 44332', licenseNumber: 'MH-12-2021-44781', distributorId: 2, distributorName: 'Pune Green Gas Depot', status: 'AVAILABLE', completedDeliveriesTotal: 310, rating: 4.9, joinedDate: '2026-03-01' },
  { id: 14, name: 'Amit Deshmukh', phone: '+91 98222 66778', licenseNumber: 'MH-31-2017-99014', distributorId: 3, distributorName: 'City Gas Services Ltd', status: 'AVAILABLE', assignedVehicleReg: 'MH-31-TR-4455', completedDeliveriesTotal: 940, rating: 5.0, joinedDate: '2025-10-15' },
  { id: 15, name: 'Dinesh Shinde', phone: '+91 98199 33221', licenseNumber: 'MH-04-2022-77120', distributorId: 4, distributorName: 'Western Bharat Gas Agency', status: 'OFF_DUTY', completedDeliveriesTotal: 180, rating: 4.6, joinedDate: '2026-04-01' },
];

const PREVIEW_VEHICLES: AdminVehicleItem[] = [
  { id: 1, registrationNumber: 'MH-04-AB-1234', vehicleType: 'MINI_TRUCK', capacityUnits: 40, distributorId: 1, distributorName: 'Apex Gas Distribution Agency', status: 'IN_TRANSIT', assignedDriverName: 'Suresh Patil', gpsDeviceId: 'TRAC-MH-01', lastLocationUpdate: '2 mins ago', insuranceValidUntil: '2027-11-20' },
  { id: 2, registrationNumber: 'MH-04-CD-5678', vehicleType: 'MINI_TRUCK', capacityUnits: 40, distributorId: 1, distributorName: 'Apex Gas Distribution Agency', status: 'ACTIVE', assignedDriverName: 'Ramesh Sawant', gpsDeviceId: 'TRAC-MH-02', lastLocationUpdate: '15 mins ago', insuranceValidUntil: '2027-08-15' },
  { id: 3, registrationNumber: 'MH-12-PQ-9081', vehicleType: 'THREE_WHEELER', capacityUnits: 20, distributorId: 2, distributorName: 'Pune Green Gas Depot', status: 'IN_TRANSIT', assignedDriverName: 'Ganesh More', gpsDeviceId: 'TRAC-PN-01', lastLocationUpdate: 'Just now', insuranceValidUntil: '2027-06-30' },
  { id: 4, registrationNumber: 'MH-12-RS-3344', vehicleType: 'HEAVY_TRUCK', capacityUnits: 120, distributorId: 2, distributorName: 'Pune Green Gas Depot', status: 'MAINTENANCE', gpsDeviceId: 'TRAC-PN-02', lastLocationUpdate: '3 hours ago', insuranceValidUntil: '2027-03-10' },
  { id: 5, registrationNumber: 'MH-31-TR-4455', vehicleType: 'MINI_TRUCK', capacityUnits: 40, distributorId: 3, distributorName: 'City Gas Services Ltd', status: 'ACTIVE', assignedDriverName: 'Amit Deshmukh', gpsDeviceId: 'TRAC-NG-01', lastLocationUpdate: '10 mins ago', insuranceValidUntil: '2027-12-01' },
  { id: 6, registrationNumber: 'MH-04-XY-8899', vehicleType: 'VAN', capacityUnits: 25, distributorId: 4, distributorName: 'Western Bharat Gas Agency', status: 'ACTIVE', gpsDeviceId: 'TRAC-TH-01', lastLocationUpdate: '1 hour ago', insuranceValidUntil: '2027-09-18' },
];

export const AdminDriversVehiclesView: React.FC = () => {
  const [activeTab, setActiveTab] = useState<'drivers' | 'vehicles'>('drivers');

  // Search & Filter state
  const [searchQuery, setSearchQuery] = useState('');
  const [distributorFilter, setDistributorFilter] = useState('ALL');
  const [statusFilter, setStatusFilter] = useState('ALL');

  // Detail Modals
  const [selectedDriver, setSelectedDriver] = useState<AdminDriverItem | null>(null);
  const [selectedVehicle, setSelectedVehicle] = useState<AdminVehicleItem | null>(null);

  const distributors = useMemo(() => {
    const list = Array.from(new Set(PREVIEW_DRIVERS.map((d) => d.distributorName)));
    return [{ label: 'All Agencies', value: 'ALL' }, ...list.map((d) => ({ label: d, value: d }))];
  }, []);

  const filteredDrivers = useMemo(() => {
    return PREVIEW_DRIVERS.filter((d) => {
      const matchSearch =
        d.name.toLowerCase().includes(searchQuery.toLowerCase()) ||
        d.phone.includes(searchQuery) ||
        d.licenseNumber.toLowerCase().includes(searchQuery.toLowerCase());

      const matchDist = distributorFilter === 'ALL' || d.distributorName === distributorFilter;
      const matchStatus = statusFilter === 'ALL' || d.status === statusFilter;

      return matchSearch && matchDist && matchStatus;
    });
  }, [searchQuery, distributorFilter, statusFilter]);

  const filteredVehicles = useMemo(() => {
    return PREVIEW_VEHICLES.filter((v) => {
      const matchSearch =
        v.registrationNumber.toLowerCase().includes(searchQuery.toLowerCase()) ||
        (v.assignedDriverName || '').toLowerCase().includes(searchQuery.toLowerCase()) ||
        (v.gpsDeviceId || '').toLowerCase().includes(searchQuery.toLowerCase());

      const matchDist = distributorFilter === 'ALL' || v.distributorName === distributorFilter;
      const matchStatus = statusFilter === 'ALL' || v.status === statusFilter;

      return matchSearch && matchDist && matchStatus;
    });
  }, [searchQuery, distributorFilter, statusFilter]);

  const filterDropdowns: FilterDropdown[] = [
    {
      id: 'distributor',
      label: 'Agency Depot',
      value: distributorFilter,
      onChange: setDistributorFilter,
      options: distributors,
    },
    {
      id: 'status',
      label: 'Status',
      value: statusFilter,
      onChange: setStatusFilter,
      options:
        activeTab === 'drivers'
          ? [
              { label: 'All Statuses', value: 'ALL' },
              { label: 'Available', value: 'AVAILABLE' },
              { label: 'On Delivery', value: 'ON_DELIVERY' },
              { label: 'Off Duty', value: 'OFF_DUTY' },
            ]
          : [
              { label: 'All Statuses', value: 'ALL' },
              { label: 'Active', value: 'ACTIVE' },
              { label: 'In Transit', value: 'IN_TRANSIT' },
              { label: 'Maintenance', value: 'MAINTENANCE' },
            ],
    },
  ];

  const driverColumns: ColumnDef<AdminDriverItem>[] = [
    {
      key: 'name',
      header: 'Driver Name',
      render: (d) => (
        <div className="flex items-center gap-2.5">
          <div className="w-8 h-8 rounded-full bg-slate-100 border border-slate-200 flex items-center justify-center font-bold text-slate-700 text-xs">
            {d.name.charAt(0)}
          </div>
          <div className="flex flex-col">
            <span className="font-bold text-slate-900">{d.name}</span>
            <span className="text-[11px] text-slate-400 font-mono">DL: {d.licenseNumber}</span>
          </div>
        </div>
      ),
    },
    {
      key: 'distributorName',
      header: 'Agency Depot',
      render: (d) => (
        <div className="flex flex-col text-xs">
          <span className="font-semibold text-slate-800">{d.distributorName}</span>
          <span className="text-[11px] text-slate-400">{d.phone}</span>
        </div>
      ),
    },
    {
      key: 'status',
      header: 'Duty Status',
      render: (d) => <AdminStatusBadge status={d.status} size="sm" />,
    },
    {
      key: 'vehicle',
      header: 'Assigned Vehicle',
      render: (d) =>
        d.assignedVehicleReg ? (
          <span className="font-mono text-xs font-semibold text-slate-800 bg-slate-100 px-2 py-0.5 rounded border border-slate-200">
            {d.assignedVehicleReg}
          </span>
        ) : (
          <span className="text-slate-400 italic text-xs">Unassigned</span>
        ),
    },
    {
      key: 'completedDeliveriesTotal',
      header: 'Deliveries',
      render: (d) => (
        <div className="flex items-center gap-1.5 text-xs">
          <span className="font-bold text-slate-900">{d.completedDeliveriesTotal}</span>
          <span className="text-[10px] text-emerald-600 font-bold">★ {d.rating}</span>
        </div>
      ),
    },
    {
      key: 'actions',
      header: 'Actions',
      className: 'text-right',
      render: (d) => (
        <button
          onClick={() => setSelectedDriver(d)}
          className="p-1.5 rounded-lg text-slate-500 hover:text-slate-800 hover:bg-slate-100"
        >
          <Eye size={15} />
        </button>
      ),
    },
  ];

  const vehicleColumns: ColumnDef<AdminVehicleItem>[] = [
    {
      key: 'registrationNumber',
      header: 'Registration & Type',
      render: (v) => (
        <div className="flex items-center gap-2.5">
          <div className="w-8 h-8 rounded-lg bg-blue-50 text-blue-600 flex items-center justify-center font-bold text-xs border border-blue-100">
            <Truck size={16} />
          </div>
          <div className="flex flex-col">
            <span className="font-bold text-slate-900 font-mono">{v.registrationNumber}</span>
            <span className="text-[11px] text-slate-400">{v.vehicleType.replace(/_/g, ' ')}</span>
          </div>
        </div>
      ),
    },
    {
      key: 'distributorName',
      header: 'Agency Depot',
      render: (v) => (
        <div className="flex flex-col text-xs">
          <span className="font-semibold text-slate-800">{v.distributorName}</span>
          <span className="text-[11px] text-slate-400">Cap: {v.capacityUnits} Cylinders</span>
        </div>
      ),
    },
    {
      key: 'status',
      header: 'Operational Status',
      render: (v) => <AdminStatusBadge status={v.status} size="sm" />,
    },
    {
      key: 'assignedDriverName',
      header: 'Assigned Driver',
      render: (v) =>
        v.assignedDriverName ? (
          <span className="font-semibold text-slate-800 text-xs">{v.assignedDriverName}</span>
        ) : (
          <span className="text-slate-400 italic text-xs">Unassigned</span>
        ),
    },
    {
      key: 'gpsDeviceId',
      header: 'GPS Telemetry',
      render: (v) => (
        <div className="flex flex-col text-[11px]">
          <span className="font-mono text-slate-700 font-semibold">{v.gpsDeviceId}</span>
          <span className="text-slate-400">{v.lastLocationUpdate}</span>
        </div>
      ),
    },
    {
      key: 'actions',
      header: 'Actions',
      className: 'text-right',
      render: (v) => (
        <button
          onClick={() => setSelectedVehicle(v)}
          className="p-1.5 rounded-lg text-slate-500 hover:text-slate-800 hover:bg-slate-100"
        >
          <Eye size={15} />
        </button>
      ),
    },
  ];

  return (
    <div className="space-y-5">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
        <div>
          <h2 className="text-lg font-bold text-slate-900 flex items-center gap-2">
            <Truck size={20} className="text-rose-600" />
            <span>Fleet Registry: Drivers & Delivery Vehicles</span>
          </h2>
          <p className="text-xs text-slate-500 mt-0.5">
            Cross-distributor directory of delivery personnel, transport vehicles, and telemetry trackers.
          </p>
        </div>

        {/* Tab switch */}
        <div className="flex items-center gap-1 p-1 bg-slate-100 rounded-xl border border-slate-200/80">
          <button
            onClick={() => {
              setActiveTab('drivers');
              setStatusFilter('ALL');
            }}
            className={`px-3 py-1.5 rounded-lg text-xs font-bold transition-all ${
              activeTab === 'drivers'
                ? 'bg-white text-slate-900 shadow-xs'
                : 'text-slate-600 hover:text-slate-900'
            }`}
          >
            Driver Directory ({PREVIEW_DRIVERS.length})
          </button>
          <button
            onClick={() => {
              setActiveTab('vehicles');
              setStatusFilter('ALL');
            }}
            className={`px-3 py-1.5 rounded-lg text-xs font-bold transition-all ${
              activeTab === 'vehicles'
                ? 'bg-white text-slate-900 shadow-xs'
                : 'text-slate-600 hover:text-slate-900'
            }`}
          >
            Vehicle Fleet ({PREVIEW_VEHICLES.length})
          </button>
        </div>
      </div>

      {/* Filter Bar */}
      <AdminFilterBar
        searchQuery={searchQuery}
        onSearchChange={setSearchQuery}
        searchPlaceholder={
          activeTab === 'drivers'
            ? 'Search driver name, phone, license...'
            : 'Search registration, driver, GPS device...'
        }
        dropdowns={filterDropdowns}
        onReset={() => {
          setSearchQuery('');
          setDistributorFilter('ALL');
          setStatusFilter('ALL');
        }}
        totalResults={activeTab === 'drivers' ? filteredDrivers.length : filteredVehicles.length}
      />

      {/* Tables */}
      {activeTab === 'drivers' ? (
        <AdminTable
          columns={driverColumns}
          data={filteredDrivers}
          keyExtractor={(d) => d.id}
          emptyTitle="No drivers found"
          pageSize={6}
          onRowClick={(d) => setSelectedDriver(d)}
        />
      ) : (
        <AdminTable
          columns={vehicleColumns}
          data={filteredVehicles}
          keyExtractor={(v) => v.id}
          emptyTitle="No vehicles found"
          pageSize={6}
          onRowClick={(v) => setSelectedVehicle(v)}
        />
      )}

      {/* Driver Detail Modal */}
      <AdminDetailModal
        isOpen={!!selectedDriver}
        title={selectedDriver?.name || 'Driver Profile'}
        subtitle={`Driver ID #${selectedDriver?.id} • Licensed with ${selectedDriver?.distributorName}`}
        badge={selectedDriver ? <AdminStatusBadge status={selectedDriver.status} size="sm" /> : undefined}
        icon={<Users size={18} className="text-rose-600" />}
        onClose={() => setSelectedDriver(null)}
      >
        {selectedDriver && (
          <div className="space-y-4 text-xs text-slate-700">
            <div className="p-4 rounded-xl bg-slate-50 border border-slate-200 grid grid-cols-2 gap-3">
              <div>
                <span className="text-[10px] text-slate-400 font-bold block">Mobile Contact</span>
                <span className="font-semibold text-slate-900 text-sm">{selectedDriver.phone}</span>
              </div>
              <div>
                <span className="text-[10px] text-slate-400 font-bold block">Commercial Driving License</span>
                <span className="font-mono font-semibold text-slate-900 text-sm">
                  {selectedDriver.licenseNumber}
                </span>
              </div>
              <div>
                <span className="text-[10px] text-slate-400 font-bold block">Active Vehicle</span>
                <span className="font-mono font-bold text-rose-600">
                  {selectedDriver.assignedVehicleReg || 'No active vehicle'}
                </span>
              </div>
              <div>
                <span className="text-[10px] text-slate-400 font-bold block">Completed Deliveries</span>
                <span className="font-bold text-slate-900">
                  {selectedDriver.completedDeliveriesTotal} handoffs ({selectedDriver.rating} ★)
                </span>
              </div>
            </div>
          </div>
        )}
      </AdminDetailModal>

      {/* Vehicle Detail Modal */}
      <AdminDetailModal
        isOpen={!!selectedVehicle}
        title={`Vehicle ${selectedVehicle?.registrationNumber}`}
        subtitle={`${selectedVehicle?.vehicleType.replace(/_/g, ' ')} • Depot: ${selectedVehicle?.distributorName}`}
        badge={selectedVehicle ? <AdminStatusBadge status={selectedVehicle.status} size="sm" /> : undefined}
        icon={<Truck size={18} className="text-rose-600" />}
        onClose={() => setSelectedVehicle(null)}
      >
        {selectedVehicle && (
          <div className="space-y-4 text-xs text-slate-700">
            <div className="p-4 rounded-xl bg-slate-50 border border-slate-200 grid grid-cols-2 gap-3">
              <div>
                <span className="text-[10px] text-slate-400 font-bold block">Cylinder Capacity</span>
                <span className="font-black text-slate-900 text-base">
                  {selectedVehicle.capacityUnits} Cylinders
                </span>
              </div>
              <div>
                <span className="text-[10px] text-slate-400 font-bold block">GPS Hardware ID</span>
                <span className="font-mono font-bold text-blue-600">
                  {selectedVehicle.gpsDeviceId}
                </span>
              </div>
              <div>
                <span className="text-[10px] text-slate-400 font-bold block">Assigned Driver</span>
                <span className="font-semibold text-slate-900">
                  {selectedVehicle.assignedDriverName || 'None assigned'}
                </span>
              </div>
              <div>
                <span className="text-[10px] text-slate-400 font-bold block">Insurance Validity</span>
                <span className="font-mono text-slate-700">{selectedVehicle.insuranceValidUntil}</span>
              </div>
            </div>
          </div>
        )}
      </AdminDetailModal>
    </div>
  );
};
