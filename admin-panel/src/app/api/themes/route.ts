import { NextResponse } from 'next/server';
import { db } from '@/lib/firebase';

export async function GET() {
  try {
    const snapshot = await db.collection('themes')
      .where('active', '==', true)
      .orderBy('createdAt', 'desc')
      .get();

    const themes = snapshot.docs.map((doc) => ({
      id: doc.id,
      ...doc.data(),
    }));

    return NextResponse.json(themes);
  } catch (error) {
    return NextResponse.json({ error: 'Failed to fetch themes' }, { status: 500 });
  }
}

export async function POST(req: Request) {
  try {
    const body = await req.json();
    const { id, ...data } = body;

    data.updatedAt = Date.now();

    if (data.isDefault) {
      await db.collection('themes').get().then((snapshot) => {
        snapshot.docs.forEach((doc) => doc.ref.update({ isDefault: false }));
      });
    }

    if (id) {
      await db.collection('themes').doc(id).update(data);
    } else {
      data.createdAt = Date.now();
      data.active = true;
      await db.collection('themes').add(data);
    }

    return NextResponse.json({ success: true });
  } catch (error) {
    return NextResponse.json({ error: 'Failed to save theme' }, { status: 500 });
  }
}

export async function DELETE(req: Request) {
  try {
    const { id } = await req.json();
    await db.collection('themes').doc(id).delete();
    return NextResponse.json({ success: true });
  } catch (error) {
    return NextResponse.json({ error: 'Failed to delete theme' }, { status: 500 });
  }
}
