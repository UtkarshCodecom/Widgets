'use client';

import { useEffect, useState } from 'react';
import AdminLayout from '@/components/AdminLayout';
import toast from 'react-hot-toast';
import { AppConfig } from '@/lib/types';

export default function ConfigPage() {
  const [config, setConfig] = useState<AppConfig>({
    latestVersion: 1,
    forceUpdate: false,
    forceUpdateMessage: '',
    maintenanceMode: false,
    maintenanceMessage: '',
    minSupportedVersion: 1,
    featureFlags: {},
    trendingWidgetIds: [],
    featuredWidgetIds: [],
  });
  const [loading, setLoading] = useState(true);

  useEffect(() => { loadConfig(); }, []);

  const loadConfig = async () => {
    try {
      const res = await fetch('/api/config');
      const data = await res.json();
      if (data) setConfig(data);
    } catch { toast.error('Failed to load config'); }
    finally { setLoading(false); }
  };

  const handleSave = async () => {
    try {
      await fetch('/api/config', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(config),
      });
      toast.success('Configuration saved');
    } catch { toast.error('Failed to save'); }
  };

  const addFlag = () => {
    const key = prompt('Enter feature flag name:');
    if (key) setConfig({ ...config, featureFlags: { ...config.featureFlags, [key]: false } });
  };

  const removeFlag = (key: string) => {
    const flags = { ...config.featureFlags };
    delete flags[key];
    setConfig({ ...config, featureFlags: flags });
  };

  if (loading) return <AdminLayout><div className="p-6"><div className="card animate-pulse h-64" /></div></AdminLayout>;

  return (
    <AdminLayout>
      <div className="p-6">
        <h1 className="text-2xl font-bold text-white mb-6">App Configuration</h1>

        <div className="space-y-6 max-w-2xl">
          <div className="card space-y-4">
            <h2 className="text-lg font-semibold text-white">Version Settings</h2>
            <div className="grid grid-cols-2 gap-4">
              <div className="input-group">
                <label>Latest Version Code</label>
                <input type="number" value={config.latestVersion} onChange={(e) => setConfig({ ...config, latestVersion: parseInt(e.target.value) })} />
              </div>
              <div className="input-group">
                <label>Min Supported Version</label>
                <input type="number" value={config.minSupportedVersion} onChange={(e) => setConfig({ ...config, minSupportedVersion: parseInt(e.target.value) })} />
              </div>
            </div>
          </div>

          <div className="card space-y-4">
            <h2 className="text-lg font-semibold text-white">Maintenance & Updates</h2>
            <label className="flex items-center gap-2 text-white">
              <input type="checkbox" checked={config.forceUpdate} onChange={(e) => setConfig({ ...config, forceUpdate: e.target.checked })} />
              Force Update
            </label>
            {config.forceUpdate && (
              <div className="input-group">
                <label>Force Update Message</label>
                <textarea value={config.forceUpdateMessage} onChange={(e) => setConfig({ ...config, forceUpdateMessage: e.target.value })} />
              </div>
            )}
            <label className="flex items-center gap-2 text-white">
              <input type="checkbox" checked={config.maintenanceMode} onChange={(e) => setConfig({ ...config, maintenanceMode: e.target.checked })} />
              Maintenance Mode
            </label>
            {config.maintenanceMode && (
              <div className="input-group">
                <label>Maintenance Message</label>
                <textarea value={config.maintenanceMessage} onChange={(e) => setConfig({ ...config, maintenanceMessage: e.target.value })} />
              </div>
            )}
          </div>

          <div className="card space-y-4">
            <div className="flex items-center justify-between">
              <h2 className="text-lg font-semibold text-white">Feature Flags</h2>
              <button onClick={addFlag} className="btn-secondary text-sm">Add Flag</button>
            </div>
            {Object.keys(config.featureFlags).length === 0 ? (
              <p className="text-gray-400">No feature flags configured</p>
            ) : (
              <div className="space-y-2">
                {Object.entries(config.featureFlags).map(([key, value]) => (
                  <div key={key} className="flex items-center justify-between bg-dark-700 rounded-lg px-4 py-2">
                    <span className="text-white">{key}</span>
                    <div className="flex items-center gap-3">
                      <input type="checkbox" checked={value} onChange={(e) => setConfig({ ...config, featureFlags: { ...config.featureFlags, [key]: e.target.checked } })} />
                      <button onClick={() => removeFlag(key)} className="text-red-400 hover:text-red-300 text-sm">Remove</button>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>

          <button onClick={handleSave} className="btn-primary w-full">Save Configuration</button>
        </div>
      </div>
    </AdminLayout>
  );
}
