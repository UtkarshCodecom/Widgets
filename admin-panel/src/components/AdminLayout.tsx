'use client';

import { useState } from 'react';
import Link from 'next/link';
import { useRouter, usePathname } from 'next/navigation';
import { auth, signOut } from '@/lib/firebase-client';
import { deleteCookie } from 'cookies-next';
import toast from 'react-hot-toast';
import {
  HiOutlineSquares2X2,
  HiOutlineCube,
  HiOutlineTag,
  HiOutlinePaintBrush,
  HiOutlineMegaphone,
  HiOutlineGift,
  HiOutlineCog6Tooth,
  HiOutlineChartBar,
  HiOutlineArrowRightOnRectangle,
  HiOutlineBars3,
  HiOutlineXMark,
} from 'react-icons/hi2';

const navItems = [
  { href: '/dashboard', label: 'Dashboard', icon: HiOutlineSquares2X2 },
  { href: '/widgets', label: 'Widgets', icon: HiOutlineCube },
  { href: '/categories', label: 'Categories', icon: HiOutlineTag },
  { href: '/themes', label: 'Themes', icon: HiOutlinePaintBrush },
  { href: '/announcements', label: 'Announcements', icon: HiOutlineMegaphone },
  { href: '/offers', label: 'Offers', icon: HiOutlineGift },
  { href: '/analytics', label: 'Analytics', icon: HiOutlineChartBar },
  { href: '/config', label: 'Configuration', icon: HiOutlineCog6Tooth },
];

export default function AdminLayout({ children }: { children: React.ReactNode }) {
  const [sidebarOpen, setSidebarOpen] = useState(false);
  const router = useRouter();
  const pathname = usePathname();

  const handleSignOut = async () => {
    try {
      await signOut(auth);
      deleteCookie('session', { path: '/' });
      toast.success('Signed out');
      router.push('/auth');
    } catch {
      toast.error('Failed to sign out');
    }
  };

  return (
    <div className="min-h-screen bg-dark-900 flex">
      {/* Mobile sidebar backdrop */}
      {sidebarOpen && (
        <div
          className="fixed inset-0 bg-black/50 z-40 lg:hidden"
          onClick={() => setSidebarOpen(false)}
        />
      )}

      {/* Sidebar */}
      <aside
        className={`fixed lg:static inset-y-0 left-0 z-50 w-64 bg-dark-800 border-r border-dark-500 transform transition-transform lg:translate-x-0 ${
          sidebarOpen ? 'translate-x-0' : '-translate-x-full'
        }`}
      >
        <div className="p-6 border-b border-dark-500">
          <Link href="/dashboard" className="flex items-center gap-3">
            <div className="w-10 h-10 bg-accent rounded-xl flex items-center justify-center">
              <svg className="w-5 h-5 text-black" fill="currentColor" viewBox="0 0 24 24">
                <path d="M3,3h8v8H3V3zM13,3h8v8h-8V3zM3,13h8v8H3v-8zM13,13h8v8h-8v-8z" />
              </svg>
            </div>
            <div>
              <p className="text-white font-bold">Widgets</p>
              <p className="text-gray-400 text-xs">Admin Panel</p>
            </div>
          </Link>
        </div>

        <nav className="p-4 space-y-1">
          {navItems.map((item) => {
            const Icon = item.icon;
            const isActive = pathname === item.href;
            return (
              <Link
                key={item.href}
                href={item.href}
                onClick={() => setSidebarOpen(false)}
                className={`flex items-center gap-3 px-4 py-2.5 rounded-lg text-sm transition-colors ${
                  isActive
                    ? 'bg-accent/20 text-accent'
                    : 'text-gray-400 hover:bg-dark-700 hover:text-white'
                }`}
              >
                <Icon className="w-5 h-5" />
                {item.label}
              </Link>
            );
          })}
        </nav>

        <div className="absolute bottom-0 left-0 right-0 p-4 border-t border-dark-500">
          <button
            onClick={handleSignOut}
            className="flex items-center gap-3 px-4 py-2.5 rounded-lg text-sm text-gray-400 hover:bg-dark-700 hover:text-red-500 w-full transition-colors"
          >
            <HiOutlineArrowRightOnRectangle className="w-5 h-5" />
            Sign Out
          </button>
        </div>
      </aside>

      {/* Main content */}
      <main className="flex-1 min-h-screen overflow-auto">
        {/* Top bar */}
        <header className="bg-dark-800 border-b border-dark-500 px-6 py-4 flex items-center justify-between lg:justify-end">
          <button
            onClick={() => setSidebarOpen(true)}
            className="lg:hidden text-gray-400 hover:text-white"
          >
            <HiOutlineBars3 className="w-6 h-6" />
          </button>
          <p className="text-gray-400 text-sm">Welcome, Admin</p>
        </header>

        {children}
      </main>
    </div>
  );
}
