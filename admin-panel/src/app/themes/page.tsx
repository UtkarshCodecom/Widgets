'use client';

import { useEffect, useState } from 'react';
import AdminLayout from '@/components/AdminLayout';
import CrudTable from '@/components/CrudTable';
import Modal from '@/components/Modal';
import toast from 'react-hot-toast';
import { Theme } from '@/lib/types';

export default function ThemesPage() {
  const [themes, setThemes] = useState<Theme[]>([]);
  const [loading, setLoading] = useState(true);
  const [modalOpen, setModalOpen] = useState(false);
  const [editing, setEditing] = useState<Theme | null>(null);
  const [form, setForm] = useState<Partial<Theme>>({});

  useEffect(() => { loadThemes(); }, []);

  const loadThemes = async () => {
    try { const res = await fetch('/api/themes'); setThemes(await res.json()); }
    catch { toast.error('Failed to load'); }
    finally { setLoading(false); }
  };

  const handleAdd = () => { setEditing(null); setForm({ name: '', description: '', thumbnailUrl: '', isPro: false, isDefault: false, active: true, config: {} }); setModalOpen(true); };
  const handleEdit = (item: Record<string, unknown>) => { setEditing(item as unknown as Theme); setForm(item as unknown as Partial<Theme>); setModalOpen(true); };
  const handleDelete = async (id: string) => { await fetch('/api/themes', { method: 'DELETE', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ id }) }); await loadThemes(); };

  const handleSave = async (e: React.FormEvent) => {
    e.preventDefault();
    await fetch('/api/themes', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ ...form, id: editing?.id }) });
    toast.success(editing ? 'Updated' : 'Created');
    setModalOpen(false);
    await loadThemes();
  };

  return (
    <AdminLayout>
      <div className="p-6">
        <CrudTable
          title="Theme Management"
          columns={[
            { key: 'name', label: 'Name' },
            { key: 'isDefault', label: 'Default', render: (v: unknown) => v ? <span className="text-accent">Default</span> : '-' },
            { key: 'isPro', label: 'Type', render: (v: unknown) => <span className={v ? 'text-accent' : 'text-green-400'}>{v ? 'PRO' : 'FREE'}</span> },
          ]}
          data={themes as unknown as Record<string, unknown>[]}
          onAdd={handleAdd}
          onEdit={handleEdit}
          onDelete={handleDelete}
          loading={loading}
        />
        <Modal open={modalOpen} onClose={() => setModalOpen(false)} title={editing ? 'Edit Theme' : 'Add Theme'}>
          <form onSubmit={handleSave} className="space-y-4">
            <div className="input-group"><label>Name</label><input value={form.name || ''} onChange={(e) => setForm({ ...form, name: e.target.value })} required /></div>
            <div className="input-group"><label>Description</label><textarea value={form.description || ''} onChange={(e) => setForm({ ...form, description: e.target.value })} /></div>
            <div className="input-group"><label>Thumbnail URL</label><input value={form.thumbnailUrl || ''} onChange={(e) => setForm({ ...form, thumbnailUrl: e.target.value })} /></div>
            <div className="flex gap-6">
              <label className="flex items-center gap-2 text-white"><input type="checkbox" checked={form.isPro || false} onChange={(e) => setForm({ ...form, isPro: e.target.checked })} /> Pro</label>
              <label className="flex items-center gap-2 text-white"><input type="checkbox" checked={form.isDefault || false} onChange={(e) => setForm({ ...form, isDefault: e.target.checked })} /> Default</label>
            </div>
            <div className="flex justify-end gap-3 pt-4"><button type="button" onClick={() => setModalOpen(false)} className="btn-secondary">Cancel</button><button type="submit" className="btn-primary">{editing ? 'Update' : 'Create'}</button></div>
          </form>
        </Modal>
      </div>
    </AdminLayout>
  );
}
