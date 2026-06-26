'use client';

import { useEffect, useState } from 'react';
import Link from 'next/link';
import AdminLayout from '@/components/AdminLayout';
import { Widget, Category, Theme, Announcement, Offer, AppConfig } from '@/lib/types';

interface DashboardStats {
  totalWidgets: number;
  totalCategories: number;
  totalThemes: number;
  totalAnnouncements: number;
  totalOffers: number;
  totalDownloads: number;
  proWidgets: number;
  freeWidgets: number;
}

export default function DashboardPage() {
  const [stats, setStats] = useState<DashboardStats>({
    totalWidgets: 0,
    totalCategories: 0,
    totalThemes: 0,
    totalAnnouncements: 0,
    totalOffers: 0,
    totalDownloads: 0,
    proWidgets: 0,
    freeWidgets: 0,
  });
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    loadStats();
  }, []);

  const loadStats = async () => {
    try {
      const [widgetsRes, categoriesRes, themesRes, announcementsRes, offersRes] = await Promise.all([
        fetch('/api/widgets'),
        fetch('/api/categories'),
        fetch('/api/themes'),
        fetch('/api/announcements'),
        fetch('/api/offers'),
      ]);

      const widgets: Widget[] = await widgetsRes.json();
      const categories: Category[] = await categoriesRes.json();
      const themes: Theme[] = await themesRes.json();
      const announcements: Announcement[] = await announcementsRes.json();
      const offers: Offer[] = await offersRes.json();

      setStats({
        totalWidgets: widgets.length,
        totalCategories: categories.length,
        totalThemes: themes.length,
        totalAnnouncements: announcements.length,
        totalOffers: offers.length,
        totalDownloads: widgets.reduce((sum, w) => sum + (w.downloadCount || 0), 0),
        proWidgets: widgets.filter((w) => w.isPro).length,
        freeWidgets: widgets.filter((w) => !w.isPro).length,
      });
    } catch (error) {
      console.error('Failed to load stats:', error);
    } finally {
      setLoading(false);
    }
  };

  const dashboardCards = [
    { title: 'Widgets', value: stats.totalWidgets, href: '/widgets', color: 'bg-accent text-black' },
    { title: 'Categories', value: stats.totalCategories, href: '/categories', color: 'bg-blue-500 text-white' },
    { title: 'Themes', value: stats.totalThemes, href: '/themes', color: 'bg-purple-500 text-white' },
    { title: 'Downloads', value: stats.totalDownloads, href: '/analytics', color: 'bg-green-500 text-white' },
    { title: 'Announcements', value: stats.totalAnnouncements, href: '/announcements', color: 'bg-orange-500 text-white' },
    { title: 'Offers', value: stats.totalOffers, href: '/offers', color: 'bg-pink-500 text-white' },
    { title: 'Pro Widgets', value: stats.proWidgets, href: '/widgets', color: 'bg-yellow-500 text-black' },
    { title: 'Free Widgets', value: stats.freeWidgets, href: '/widgets', color: 'bg-emerald-500 text-white' },
  ];

  return (
    <AdminLayout>
      <div className="p-6">
        <h1 className="text-2xl font-bold text-white mb-6">Dashboard</h1>

        {loading ? (
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
            {[...Array(8)].map((_, i) => (
              <div key={i} className="card animate-pulse h-24" />
            ))}
          </div>
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
            {dashboardCards.map((card) => (
              <Link
                key={card.title}
                href={card.href}
                className={`card ${card.color} hover:scale-[1.02] transition-transform`}
              >
                <p className="text-sm opacity-80">{card.title}</p>
                <p className="text-3xl font-bold mt-2">{card.value.toLocaleString()}</p>
              </Link>
            ))}
          </div>
        )}

        <div className="mt-8 grid grid-cols-1 lg:grid-cols-2 gap-6">
          <div className="card">
            <h2 className="text-lg font-semibold text-white mb-4">Quick Actions</h2>
            <div className="space-y-3">
              <Link href="/widgets" className="block btn-primary text-center">Manage Widgets</Link>
              <Link href="/announcements" className="block btn-secondary text-center">Create Announcement</Link>
              <Link href="/config" className="block btn-secondary text-center">App Configuration</Link>
            </div>
          </div>

          <div className="card">
            <h2 className="text-lg font-semibold text-white mb-4">System Status</h2>
            <div className="space-y-3">
              <div className="flex justify-between items-center">
                <span className="text-gray-400">Maintenance Mode</span>
                <span className="text-green-500">Off</span>
              </div>
              <div className="flex justify-between items-center">
                <span className="text-gray-400">Force Update</span>
                <span className="text-gray-400">Disabled</span>
              </div>
              <div className="flex justify-between items-center">
                <span className="text-gray-400">Firebase Connection</span>
                <span className="text-green-500">Connected</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </AdminLayout>
  );
}
