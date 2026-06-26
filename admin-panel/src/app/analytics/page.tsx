'use client';

import { useEffect, useState } from 'react';
import AdminLayout from '@/components/AdminLayout';
import { Widget } from '@/lib/types';
import { BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer } from 'recharts';

export default function AnalyticsPage() {
  const [widgets, setWidgets] = useState<Widget[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    loadAnalytics();
  }, []);

  const loadAnalytics = async () => {
    try {
      const res = await fetch('/api/widgets');
      setWidgets(await res.json());
    } catch (err) {
      console.error('Failed to load analytics:', err);
    } finally {
      setLoading(false);
    }
  };

  const totalDownloads = widgets.reduce((sum, w) => sum + (w.downloadCount || 0), 0);
  const proCount = widgets.filter((w) => w.isPro).length;
  const freeCount = widgets.filter((w) => !w.isPro).length;
  const featuredCount = widgets.filter((w) => w.isFeatured).length;
  const trendingCount = widgets.filter((w) => w.isTrending).length;

  const chartData = [
    { name: 'Total', value: widgets.length },
    { name: 'Free', value: freeCount },
    { name: 'Pro', value: proCount },
    { name: 'Featured', value: featuredCount },
    { name: 'Trending', value: trendingCount },
  ];

  const downloadData = [
    { name: 'Downloads', value: totalDownloads },
  ];

  if (loading) {
    return (
      <AdminLayout>
        <div className="p-6">
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
            {[...Array(4)].map((_, i) => (
              <div key={i} className="card animate-pulse h-24" />
            ))}
          </div>
        </div>
      </AdminLayout>
    );
  }

  return (
    <AdminLayout>
      <div className="p-6">
        <h1 className="text-2xl font-bold text-white mb-6">Analytics</h1>

        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4 mb-8">
          <div className="card">
            <p className="text-gray-400 text-sm">Total Widgets</p>
            <p className="text-3xl font-bold text-white mt-2">{widgets.length}</p>
          </div>
          <div className="card">
            <p className="text-gray-400 text-sm">Total Downloads</p>
            <p className="text-3xl font-bold text-accent mt-2">{totalDownloads.toLocaleString()}</p>
          </div>
          <div className="card">
            <p className="text-gray-400 text-sm">Free / Pro</p>
            <p className="text-3xl font-bold text-white mt-2">
              {freeCount} <span className="text-sm text-gray-400">/</span> {proCount}
            </p>
          </div>
          <div className="card">
            <p className="text-gray-400 text-sm">Featured / Trending</p>
            <p className="text-3xl font-bold text-white mt-2">
              {featuredCount} <span className="text-sm text-gray-400">/</span> {trendingCount}
            </p>
          </div>
        </div>

        <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
          <div className="card">
            <h2 className="text-lg font-semibold text-white mb-4">Widget Distribution</h2>
            <div className="h-64">
              <ResponsiveContainer width="100%" height="100%">
                <BarChart data={chartData}>
                  <CartesianGrid strokeDasharray="3 3" stroke="#333" />
                  <XAxis dataKey="name" stroke="#666" />
                  <YAxis stroke="#666" />
                  <Tooltip
                    contentStyle={{ background: '#1a1a1a', border: '1px solid #333', borderRadius: '8px' }}
                    labelStyle={{ color: '#fff' }}
                  />
                  <Bar dataKey="value" fill="#FFD700" radius={[4, 4, 0, 0]} />
                </BarChart>
              </ResponsiveContainer>
            </div>
          </div>

          <div className="card">
            <h2 className="text-lg font-semibold text-white mb-4">Downloads</h2>
            <div className="h-64">
              <ResponsiveContainer width="100%" height="100%">
                <BarChart data={downloadData}>
                  <CartesianGrid strokeDasharray="3 3" stroke="#333" />
                  <XAxis dataKey="name" stroke="#666" />
                  <YAxis stroke="#666" />
                  <Tooltip
                    contentStyle={{ background: '#1a1a1a', border: '1px solid #333', borderRadius: '8px' }}
                    labelStyle={{ color: '#fff' }}
                  />
                  <Bar dataKey="value" fill="#4CAF50" radius={[4, 4, 0, 0]} />
                </BarChart>
              </ResponsiveContainer>
            </div>
          </div>
        </div>

        <div className="mt-6 card">
          <h2 className="text-lg font-semibold text-white mb-4">Top Widgets by Downloads</h2>
          <div className="space-y-3">
            {[...widgets]
              .sort((a, b) => (b.downloadCount || 0) - (a.downloadCount || 0))
              .slice(0, 10)
              .map((widget, idx) => (
                <div key={widget.id || idx} className="flex items-center justify-between bg-dark-700 rounded-lg px-4 py-3">
                  <div className="flex items-center gap-3">
                    <span className="text-gray-400 w-6">{idx + 1}.</span>
                    <span className="text-white">{widget.name}</span>
                    {widget.isPro && <span className="text-xs text-accent font-bold">PRO</span>}
                  </div>
                  <span className="text-accent font-bold">{widget.downloadCount || 0}</span>
                </div>
              ))}
          </div>
        </div>
      </div>
    </AdminLayout>
  );
}
