'use client';

import { useEffect, useState } from 'react';
import AdminLayout from '@/components/AdminLayout';
import CrudTable from '@/components/CrudTable';
import Modal from '@/components/Modal';
import toast from 'react-hot-toast';
import { Announcement } from '@/lib/types';

const now = Date.now();

export default function AnnouncementsPage() {
  const [items, setItems] = useState<Announcement[]>([]);
  const [loading, setLoading] = useState(true);
  const [modalOpen, setModalOpen] = useState(false);
  const [editing, setEditing] = useState<Announcement | null>(null);
  const [form, setForm] = useState<Partial<Announcement>>({});

  useEffect(() => { load(); }, []);

  const load = async () => {
    try { const res = await fetch('/api/announcements'); setItems(await res.json()); }
    catch { toast.error('Failed to load'); } finally { setLoading(false); }
  };

  const handleAdd = () => {
    setEditing(null);
    setForm({ title: '', message: '', type: 'info', priority: 0, startAt: now, endAt: now + 86400000 * 30, active: true });
    setModalOpen(true);
  };

  const handleEdit = (item: Record<string, unknown>) => { setEditing(item as unknown as Announcement); setForm(item as unknown as Partial<Announcement>); setModalOpen(true); };
  const handleDelete = async (id: string) => { await fetch('/api/announcements', { method: 'DELETE', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ id }) }); await load(); };

  const handleSave = async (e: React.FormEvent) => {
    e.preventDefault();
    await fetch('/api/announcements', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ ...form, id: editing?.id }) });
    toast.success(editing ? 'Updated' : 'Created');
    setModalOpen(false);
    await load();
  };

  return (
    <AdminLayout>
      <div className="p-6">
        <CrudTable
          title="Announcements"
          columns={[
            { key: 'title', label: 'Title' },
            { key: 'type', label: 'Type' },
            { key: 'priority', label: 'Priority' },
            { key: 'active', label: 'Status', render: (v: unknown) => <span className={v ? 'text-green-400' : 'text-red-400'}>{v ? 'Active' : 'Inactive'}</span> },
          ]}
          data={items as unknown as Record<string, unknown>[]}
          onAdd={handleAdd}
          onEdit={handleEdit}
          onDelete={handleDelete}
          loading={loading}
        />
        <Modal open={modalOpen} onClose={() => setModalOpen(false)} title={editing ? 'Edit Announcement' : 'Add Announcement'}>
          <form onSubmit={handleSave} className="space-y-4">
            <div className="input-group"><label>Title</label><input value={form.title || ''} onChange={(e) => setForm({ ...form, title: e.target.value })} required /></div>
            <div className="input-group"><label>Message</label><textarea value={form.message || ''} onChange={(e) => setForm({ ...form, message: e.target.value })} rows={3} /></div>
            <div className="grid grid-cols-2 gap-4">
              <div className="input-group"><label>Type</label><select value={form.type || 'info'} onChange={(e) => setForm({ ...form, type: e.target.value })}><option value="info">Info</option><option value="warning">Warning</option><option value="update">Update</option></select></div>
              <div className="input-group"><label>Priority</label><input type="number" value={form.priority || 0} onChange={(e) => setForm({ ...form, priority: parseInt(e.target.value) })} /></div>
            </div>
            <label className="flex items-center gap-2 text-white"><input type="checkbox" checked={form.active || false} onChange={(e) => setForm({ ...form, active: e.target.checked })} /> Active</label>
            <div className="flex justify-end gap-3 pt-4"><button type="button" onClick={() => setModalOpen(false)} className="btn-secondary">Cancel</button><button type="submit" className="btn-primary">{editing ? 'Update' : 'Create'}</button></div>
          </form>
        </Modal>
      </div>
    </AdminLayout>
  );
}
