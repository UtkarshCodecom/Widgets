'use client';

import { useEffect, useState } from 'react';
import AdminLayout from '@/components/AdminLayout';
import CrudTable from '@/components/CrudTable';
import Modal from '@/components/Modal';
import toast from 'react-hot-toast';
import { Widget } from '@/lib/types';

export default function WidgetsPage() {
  const [widgets, setWidgets] = useState<Widget[]>([]);
  const [loading, setLoading] = useState(true);
  const [modalOpen, setModalOpen] = useState(false);
  const [editingWidget, setEditingWidget] = useState<Widget | null>(null);
  const [form, setForm] = useState<Partial<Widget>>({});

  useEffect(() => {
    loadWidgets();
  }, []);

  const loadWidgets = async () => {
    try {
      const res = await fetch('/api/widgets');
      const data = await res.json();
      setWidgets(data);
    } catch {
      toast.error('Failed to load widgets');
    } finally {
      setLoading(false);
    }
  };

  const handleAdd = () => {
    setEditingWidget(null);
    setForm({
      name: '',
      description: '',
      categoryId: '',
      categoryName: '',
      thumbnailUrl: '',
      previewUrl: '',
      configJson: '',
      isPro: false,
      isFeatured: false,
      isTrending: false,
      active: true,
    });
    setModalOpen(true);
  };

  const handleEdit = (widget: Record<string, unknown>) => {
    setEditingWidget(widget as unknown as Widget);
    setForm(widget as unknown as Partial<Widget>);
    setModalOpen(true);
  };

  const handleDelete = async (id: string) => {
    await fetch('/api/widgets', {
      method: 'DELETE',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ id }),
    });
    await loadWidgets();
  };

  const handleSave = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      await fetch('/api/widgets', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ ...form, id: editingWidget?.id }),
      });
      toast.success(editingWidget ? 'Widget updated' : 'Widget created');
      setModalOpen(false);
      await loadWidgets();
    } catch {
      toast.error('Failed to save widget');
    }
  };

  const columns = [
    { key: 'name', label: 'Name' },
    { key: 'categoryName', label: 'Category' },
    {
      key: 'isPro',
      label: 'Type',
      render: (value: unknown) => (
        <span className={value ? 'text-accent' : 'text-green-400'}>
          {value ? 'PRO' : 'FREE'}
        </span>
      ),
    },
    {
      key: 'downloadCount',
      label: 'Downloads',
    },
    {
      key: 'active',
      label: 'Status',
      render: (value: unknown) => (
        <span className={value ? 'text-green-400' : 'text-red-400'}>
          {value ? 'Active' : 'Inactive'}
        </span>
      ),
    },
  ];

  return (
    <AdminLayout>
      <div className="p-6">
        <CrudTable
          title="Widget Management"
          columns={columns}
          data={widgets as unknown as Record<string, unknown>[]}
          onAdd={handleAdd}
          onEdit={handleEdit}
          onDelete={handleDelete}
          loading={loading}
        />

        <Modal
          open={modalOpen}
          onClose={() => setModalOpen(false)}
          title={editingWidget ? 'Edit Widget' : 'Add Widget'}
        >
          <form onSubmit={handleSave} className="space-y-4">
            <div className="input-group">
              <label>Name</label>
              <input
                value={form.name || ''}
                onChange={(e) => setForm({ ...form, name: e.target.value })}
                required
              />
            </div>
            <div className="input-group">
              <label>Description</label>
              <textarea
                value={form.description || ''}
                onChange={(e) => setForm({ ...form, description: e.target.value })}
                rows={3}
              />
            </div>
            <div className="grid grid-cols-2 gap-4">
              <div className="input-group">
                <label>Category ID</label>
                <input
                  value={form.categoryId || ''}
                  onChange={(e) => setForm({ ...form, categoryId: e.target.value })}
                />
              </div>
              <div className="input-group">
                <label>Category Name</label>
                <input
                  value={form.categoryName || ''}
                  onChange={(e) => setForm({ ...form, categoryName: e.target.value })}
                />
              </div>
            </div>
            <div className="input-group">
              <label>Thumbnail URL</label>
              <input
                value={form.thumbnailUrl || ''}
                onChange={(e) => setForm({ ...form, thumbnailUrl: e.target.value })}
              />
            </div>
            <div className="input-group">
              <label>Preview URL</label>
              <input
                value={form.previewUrl || ''}
                onChange={(e) => setForm({ ...form, previewUrl: e.target.value })}
              />
            </div>
            <div className="input-group">
              <label>Config JSON</label>
              <textarea
                value={form.configJson || ''}
                onChange={(e) => setForm({ ...form, configJson: e.target.value })}
                rows={4}
              />
            </div>
            <div className="flex gap-6">
              <label className="flex items-center gap-2 text-white">
                <input
                  type="checkbox"
                  checked={form.isPro || false}
                  onChange={(e) => setForm({ ...form, isPro: e.target.checked })}
                />
                Pro Widget
              </label>
              <label className="flex items-center gap-2 text-white">
                <input
                  type="checkbox"
                  checked={form.isFeatured || false}
                  onChange={(e) => setForm({ ...form, isFeatured: e.target.checked })}
                />
                Featured
              </label>
              <label className="flex items-center gap-2 text-white">
                <input
                  type="checkbox"
                  checked={form.isTrending || false}
                  onChange={(e) => setForm({ ...form, isTrending: e.target.checked })}
                />
                Trending
              </label>
            </div>
            <div className="flex justify-end gap-3 pt-4">
              <button type="button" onClick={() => setModalOpen(false)} className="btn-secondary">
                Cancel
              </button>
              <button type="submit" className="btn-primary">
                {editingWidget ? 'Update' : 'Create'}
              </button>
            </div>
          </form>
        </Modal>
      </div>
    </AdminLayout>
  );
}
