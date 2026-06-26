'use client';

import { useState } from 'react';
import toast from 'react-hot-toast';

interface Column {
  key: string;
  label: string;
  render?: (value: unknown, row: Record<string, unknown>) => React.ReactNode;
}

interface CrudTableProps {
  title: string;
  columns: Column[];
  data: Record<string, unknown>[];
  onAdd: () => void;
  onEdit: (item: Record<string, unknown>) => void;
  onDelete: (id: string) => Promise<void>;
  loading?: boolean;
}

export default function CrudTable({
  title,
  columns,
  data,
  onAdd,
  onEdit,
  onDelete,
  loading,
}: CrudTableProps) {
  const [deletingId, setDeletingId] = useState<string | null>(null);

  const handleDelete = async (id: string) => {
    if (!confirm('Are you sure you want to delete this item?')) return;
    setDeletingId(id);
    try {
      await onDelete(id);
      toast.success('Deleted successfully');
    } catch {
      toast.error('Failed to delete');
    } finally {
      setDeletingId(null);
    }
  };

  return (
    <div>
      <div className="flex items-center justify-between mb-6">
        <h1 className="text-2xl font-bold text-white">{title}</h1>
        <button onClick={onAdd} className="btn-primary">
          Add New
        </button>
      </div>

      <div className="card overflow-x-auto">
        {loading ? (
          <div className="space-y-4">
            {[...Array(5)].map((_, i) => (
              <div key={i} className="h-12 bg-dark-600 rounded animate-pulse" />
            ))}
          </div>
        ) : data.length === 0 ? (
          <p className="text-gray-400 text-center py-8">No data found</p>
        ) : (
          <table className="w-full text-sm">
            <thead>
              <tr className="border-b border-dark-500">
                {columns.map((col) => (
                  <th key={col.key} className="text-left py-3 px-4 text-gray-400 font-medium">
                    {col.label}
                  </th>
                ))}
                <th className="text-right py-3 px-4 text-gray-400 font-medium">Actions</th>
              </tr>
            </thead>
            <tbody>
              {data.map((row, idx) => (
                <tr key={row.id as string || idx} className="border-b border-dark-500 hover:bg-dark-700">
                  {columns.map((col) => (
                    <td key={col.key} className="py-3 px-4 text-white">
                      {col.render
                        ? col.render(row[col.key], row)
                        : String(row[col.key] ?? '')}
                    </td>
                  ))}
                  <td className="py-3 px-4 text-right">
                    <button
                      onClick={() => onEdit(row)}
                      className="text-accent hover:text-accent-400 mr-3"
                    >
                      Edit
                    </button>
                    <button
                      onClick={() => handleDelete(row.id as string)}
                      disabled={deletingId === row.id}
                      className="text-red-400 hover:text-red-300 disabled:opacity-50"
                    >
                      {deletingId === row.id ? 'Deleting...' : 'Delete'}
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>
    </div>
  );
}
