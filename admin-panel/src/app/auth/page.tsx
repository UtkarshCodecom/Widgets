'use client';

import { useState } from 'react';
import { useRouter } from 'next/navigation';
import { auth, signInWithEmailAndPassword } from '@/lib/firebase-client';
import toast from 'react-hot-toast';
import { setCookie } from 'cookies-next';

export default function AuthPage() {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [loading, setLoading] = useState(false);
  const router = useRouter();

  const handleLogin = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);

    try {
      const userCredential = await signInWithEmailAndPassword(auth, email, password);
      const token = await userCredential.user.getIdToken();
      setCookie('session', token, {
        maxAge: 60 * 60 * 24 * 7,
        path: '/',
      });
      toast.success('Welcome back!');
      router.push('/dashboard');
    } catch (error: unknown) {
      const err = error as { code?: string; message?: string };
      toast.error(err.message || 'Login failed');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen flex items-center justify-center bg-dark-900 p-4">
      <div className="w-full max-w-md">
        <div className="text-center mb-8">
          <div className="w-16 h-16 bg-accent rounded-2xl flex items-center justify-center mx-auto mb-4">
            <svg className="w-8 h-8 text-black" fill="currentColor" viewBox="0 0 24 24">
              <path d="M3,3h8v8H3V3zM13,3h8v8h-8V3zM3,13h8v8H3v-8zM13,13h8v8h-8v-8z" />
            </svg>
          </div>
          <h1 className="text-2xl font-bold text-white">Widgets Admin</h1>
          <p className="text-gray-400 mt-2">Sign in to manage your widgets</p>
        </div>

        <form onSubmit={handleLogin} className="card space-y-4">
          <div className="input-group">
            <label>Email</label>
            <input
              type="email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              placeholder="admin@example.com"
              required
            />
          </div>

          <div className="input-group">
            <label>Password</label>
            <input
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              placeholder="••••••••"
              required
            />
          </div>

          <button
            type="submit"
            disabled={loading}
            className="btn-primary w-full disabled:opacity-50"
          >
            {loading ? 'Signing in...' : 'Sign In'}
          </button>
        </form>
      </div>
    </div>
  );
}
