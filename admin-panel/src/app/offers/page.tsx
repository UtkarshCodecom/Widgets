'use client';

import { useEffect, useState } from 'react';
import AdminLayout from '@/components/AdminLayout';
import CrudTable from '@/components/CrudTable';
import Modal from '@/components/Modal';
import toast from 'react-hot-toast';
import { Offer } from '@/lib/types';

export default function OffersPage() {
  const [items, setItems] = useState<Offer[]>([]);
  const [loading, setLoading] = useState(true);
  const [modalOpen, setModalOpen] = useState(false);
  const [editing, setEditing] = useState<Offer | null>(null);
  const [form, setForm] = useState<Partial<Offer>>({});

  useEffect(() => { load(); }, []);

  const load = async () => {
    try { const res = await fetch('/api/offers'); setItems(await res.json()); }
    catch { toast.error('Failed to load'); } finally { setLoading(false); }
  };

  const handleAdd = () => {
    setEditing(null);
    setForm({ title: '', description: '', discountPercent: 0, code: '', imageUrl: '', startAt: Date.now(), endAt: Date.now() + 86400000 * 30, active: true });
    setModalOpen(true);
  };

  const handleEdit = (item: Record<string, unknown>) => { setEditing(item as unknown as Offer); setForm(item as unknown as Partial<Offer>); setModalOpen(true); };
  const handleDelete = async (id: string) => { await fetch('/api/offers', { method: 'DELETE', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ id }) }); await load(); };

  const handleSave = async (e: React.FormEvent) => {
    e.preventDefault();
    await fetch('/api/offers', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ ...form, id: editing?.id }) });
    toast.success(editing ? 'Updated' : 'Created');
    setModalOpen(false);
    await load();
  };

  return (
    <AdminLayout>
      <div className="p-6">
        <CrudTable
          title="Offers"
          columns={[
            { key: 'title', label: 'Title' },
            { key: 'discountPercent', label: 'Discount', render: (v: unknown) => <span className="text-accent">{v}% OFF</span> },
            { key: 'code', label: 'Code' },
            { key: 'active', label: 'Status', render: (v: unknown) => <span className={v ? 'text-green-400' : 'text-red-400'}>{v ? 'Active' : 'Inactive'}</span> },
          ]}
          data={items as unknown as Record<string, unknown>[]}
          onAdd={handleAdd}
          onEdit={handleEdit}
          onDelete={handleDelete}
          loading={loading}
        />
        <Modal open={modalOpen} onClose={() => setModalOpen(false)} title={editing ? 'Edit Offer' : 'Add Offer'}>
          <form onSubmit={handleSave} className="space-y-4">
            <div className="input-group"><label>Title</label><input value={form.title || ''} onChange={(e) => setForm({ ...form, title: e.target.value })} required /></div>
            <div className="input-group"><label>Description</label><textarea value={form.description || ''} onChange={(e) => setForm({ ...form, description: e.target.value })} /></div>
            <div className="grid grid-cols-2 gap-4">
              <div className="input-group"><label>Discount %</label><input type="number" value={form.discountPercent || 0} onChange={(e) => setForm({ ...form, discountPercent: parseInt(e.target.value) })} /></div>
              <div className="input-group"><label>Code</label><input value={form.code || ''} onChange={(e) => setForm({ ...form, code: e.target.value })} /></div>
            </div>
            <label className="flex items-center gap-2 text-white"><input type="checkbox" checked={form.active || false} onChange={(e) => setForm({ ...form, active: e.target.checked })} /> Active</label>
            <div className="flex justify-end gap-3 pt-4"><button type="button" onClick={() => setModalOpen(false)} className="btn-secondary">Cancel</button><button type="submit" className="btn-primary">{editing ? 'Update' : 'Create'}</button></div>
          </form>
        </Modal>
      </div>
    </AdminLayout>
  );
}
