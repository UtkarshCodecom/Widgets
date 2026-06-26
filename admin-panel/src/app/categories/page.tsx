'use client';

import { useEffect, useState } from 'react';
import AdminLayout from '@/components/AdminLayout';
import CrudTable from '@/components/CrudTable';
import Modal from '@/components/Modal';
import toast from 'react-hot-toast';
import { Category } from '@/lib/types';

export default function CategoriesPage() {
  const [categories, setCategories] = useState<Category[]>([]);
  const [loading, setLoading] = useState(true);
  const [modalOpen, setModalOpen] = useState(false);
  const [editing, setEditing] = useState<Category | null>(null);
  const [form, setForm] = useState<Partial<Category>>({});

  useEffect(() => { loadCategories(); }, []);

  const loadCategories = async () => {
    try {
      const res = await fetch('/api/categories');
      setCategories(await res.json());
    } catch { toast.error('Failed to load'); }
    finally { setLoading(false); }
  };

  const handleAdd = () => {
    setEditing(null);
    setForm({ name: '', icon: '', color: '#FFD700', order: 0, isActive: true });
    setModalOpen(true);
  };

  const handleEdit = (item: Record<string, unknown>) => {
    setEditing(item as unknown as Category);
    setForm(item as unknown as Partial<Category>);
    setModalOpen(true);
  };

  const handleDelete = async (id: string) => {
    await fetch('/api/categories', { method: 'DELETE', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ id }) });
    await loadCategories();
  };

  const handleSave = async (e: React.FormEvent) => {
    e.preventDefault();
    await fetch('/api/categories', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ ...form, id: editing?.id }) });
    toast.success(editing ? 'Updated' : 'Created');
    setModalOpen(false);
    await loadCategories();
  };

  return (
    <AdminLayout>
      <div className="p-6">
        <CrudTable
          title="Category Management"
          columns={[
            { key: 'name', label: 'Name' },
            { key: 'order', label: 'Order' },
            { key: 'isActive', label: 'Status', render: (v: unknown) => <span className={v ? 'text-green-400' : 'text-red-400'}>{v ? 'Active' : 'Inactive'}</span> },
          ]}
          data={categories as unknown as Record<string, unknown>[]}
          onAdd={handleAdd}
          onEdit={handleEdit}
          onDelete={handleDelete}
          loading={loading}
        />
        <Modal open={modalOpen} onClose={() => setModalOpen(false)} title={editing ? 'Edit Category' : 'Add Category'}>
          <form onSubmit={handleSave} className="space-y-4">
            <div className="input-group"><label>Name</label><input value={form.name || ''} onChange={(e) => setForm({ ...form, name: e.target.value })} required /></div>
            <div className="input-group"><label>Icon</label><input value={form.icon || ''} onChange={(e) => setForm({ ...form, icon: e.target.value })} /></div>
            <div className="input-group"><label>Color</label><input value={form.color || ''} onChange={(e) => setForm({ ...form, color: e.target.value })} /></div>
            <div className="input-group"><label>Order</label><input type="number" value={form.order || 0} onChange={(e) => setForm({ ...form, order: parseInt(e.target.value) })} /></div>
            <label className="flex items-center gap-2 text-white"><input type="checkbox" checked={form.isActive || false} onChange={(e) => setForm({ ...form, isActive: e.target.checked })} /> Active</label>
            <div className="flex justify-end gap-3 pt-4"><button type="button" onClick={() => setModalOpen(false)} className="btn-secondary">Cancel</button><button type="submit" className="btn-primary">{editing ? 'Update' : 'Create'}</button></div>
          </form>
        </Modal>
      </div>
    </AdminLayout>
  );
}
