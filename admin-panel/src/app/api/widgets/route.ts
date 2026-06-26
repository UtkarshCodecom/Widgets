import { NextResponse } from 'next/server';
import { db } from '@/lib/firebase';

export async function GET() {
  try {
    const snapshot = await db.collection('widgets')
      .where('active', '==', true)
      .orderBy('updatedAt', 'desc')
      .get();

    const widgets = snapshot.docs.map((doc) => ({
      id: doc.id,
      ...doc.data(),
    }));

    return NextResponse.json(widgets);
  } catch (error) {
    console.error('Failed to fetch widgets:', error);
    return NextResponse.json({ error: 'Failed to fetch widgets' }, { status: 500 });
  }
}

export async function POST(req: Request) {
  try {
    const body = await req.json();
    const { id, ...data } = body;

    data.updatedAt = Date.now();

    if (id) {
      await db.collection('widgets').doc(id).update(data);
    } else {
      data.createdAt = Date.now();
      data.downloadCount = 0;
      data.favoriteCount = 0;
      data.version = 1;
      data.active = true;
      await db.collection('widgets').add(data);
    }

    return NextResponse.json({ success: true });
  } catch (error) {
    console.error('Failed to save widget:', error);
    return NextResponse.json({ error: 'Failed to save widget' }, { status: 500 });
  }
}

export async function DELETE(req: Request) {
  try {
    const { id } = await req.json();
    await db.collection('widgets').doc(id).delete();
    return NextResponse.json({ success: true });
  } catch (error) {
    console.error('Failed to delete widget:', error);
    return NextResponse.json({ error: 'Failed to delete widget' }, { status: 500 });
  }
}
