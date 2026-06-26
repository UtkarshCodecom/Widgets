import admin from 'firebase-admin';

if (!admin.apps.length) {
  admin.initializeApp({
    credential: admin.credential.applicationDefault(),
    storageBucket: process.env.NEXT_PUBLIC_FIREBASE_STORAGE_BUCKET,
  });
}

export const db = admin.firestore();
export const storage = admin.storage().bucket();
export const auth = admin.auth();
export const serverTimestamp = admin.firestore.FieldValue.serverTimestamp;
export const increment = admin.firestore.FieldValue.increment;

export default admin;
