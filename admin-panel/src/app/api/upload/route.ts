import { NextResponse } from 'next/server';
import { storage } from '@/lib/firebase';
import { v4 as uuidv4 } from 'uuid';

export async function POST(req: Request) {
  try {
    const formData = await req.formData();
    const file = formData.get('file') as File;
    const path = formData.get('path') as string || 'uploads';

    if (!file) {
      return NextResponse.json({ error: 'No file provided' }, { status: 400 });
    }

    const bytes = await file.arrayBuffer();
    const buffer = Buffer.from(bytes);
    const filename = `${uuidv4()}-${file.name}`;
    const filePath = `${path}/${filename}`;

    const blob = storage.file(filePath);
    const blobStream = blob.createWriteStream({
      metadata: {
        contentType: file.type,
      },
    });

    await new Promise<void>((resolve, reject) => {
      blobStream.on('error', reject);
      blobStream.on('finish', resolve);
      blobStream.end(buffer);
    });

    await blob.makePublic();
    const publicUrl = `https://storage.googleapis.com/${storage.name}/${filePath}`;

    return NextResponse.json({
      url: publicUrl,
      path: filePath,
      filename,
    });
  } catch (error) {
    console.error('Upload failed:', error);
    return NextResponse.json({ error: 'Upload failed' }, { status: 500 });
  }
}
