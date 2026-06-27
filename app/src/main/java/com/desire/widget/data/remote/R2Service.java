package com.desire.widget.data.remote;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import com.desire.widget.util.AppExecutors;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.UUID;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public class R2Service {
    private static final String TAG = "R2Service";
    private static R2Service instance;

    private static final String ACCESS_KEY = "f614429ba6aa5b24f78428d9725386be";
    private static final String SECRET_KEY = "4ebbc3c77da73f186e3231259fc6a9674f2e2dfea9081e4eef27cca251ef29b4";
    private static final String BUCKET = "wallpaper";
    private static final String ENDPOINT = "https://3abdc6da80177d38e57f385014487886.r2.cloudflarestorage.com";
    private static final String PUBLIC_BASE_URL = "https://waleandr.dpdns.org";
    private static final String REGION = "auto";
    private static final String SERVICE = "s3";

    private R2Service() {}

    public static synchronized R2Service getInstance() {
        if (instance == null) {
            instance = new R2Service();
        }
        return instance;
    }

    public interface UploadCallback {
        void onSuccess(String publicUrl);
        void onError(String error);
    }

    public void uploadWidgetThumbnail(Context context, Uri fileUri, UploadCallback callback) {
        String ext = getExtension(context, fileUri, ".jpg");
        String key = "widgets/thumbnails/" + UUID.randomUUID() + ext;
        uploadFile(context, key, fileUri, callback);
    }

    public void uploadWidgetPreview(Context context, Uri fileUri, UploadCallback callback) {
        String ext = getExtension(context, fileUri, ".jpg");
        String key = "widgets/previews/" + UUID.randomUUID() + ext;
        uploadFile(context, key, fileUri, callback);
    }

    public void uploadWidgetConfig(Context context, Uri fileUri, UploadCallback callback) {
        String ext = getExtension(context, fileUri, ".json");
        String key = "widgets/configs/" + UUID.randomUUID() + ext;
        uploadFile(context, key, fileUri, callback);
    }

    public void uploadThemeThumbnail(Context context, Uri fileUri, UploadCallback callback) {
        String ext = getExtension(context, fileUri, ".jpg");
        String key = "themes/thumbnails/" + UUID.randomUUID() + ext;
        uploadFile(context, key, fileUri, callback);
    }

    public void uploadFile(Context context, String key, Uri fileUri, UploadCallback callback) {
        AppExecutors.getInstance().networkIO().execute(() -> {
            HttpURLConnection conn = null;
            InputStream is = null;
            try {
                // 1. Get file metadata
                long fileSize = getFileSize(context, fileUri);
                if (fileSize < 0) {
                    callback.onError("Could not read selected file size");
                    return;
                }
                String contentType = getMimeType(key);

                // 2. Calculate Payload Hash (Required for strict SigV4)
                String payloadHash = calculatePayloadHash(context, fileUri);
                if (payloadHash == null) {
                    callback.onError("Failed to calculate file hash");
                    return;
                }

                // 3. Prepare SigV4 Components
                String dateStr = getDateStr();
                String dateTimeStr = getDateTimeStr();
                String host = new URL(ENDPOINT).getHost();
                String canonicalUri = "/" + BUCKET + "/" + key;
                String credentialScope = dateStr + "/" + REGION + "/" + SERVICE + "/aws4_request";

                // Headers must be lowercase and sorted alphabetically for the canonical request
                // We will sign: content-type, host, x-amz-content-sha256, x-amz-date
                String canonicalHeaders = "content-type:" + contentType + "\n"
                        + "host:" + host + "\n"
                        + "x-amz-content-sha256:" + payloadHash + "\n"
                        + "x-amz-date:" + dateTimeStr + "\n";
                String signedHeaders = "content-type;host;x-amz-content-sha256;x-amz-date";

                String canonicalRequest = "PUT\n"
                        + canonicalUri + "\n"
                        + "\n" // Query string is empty
                        + canonicalHeaders + "\n"
                        + signedHeaders + "\n"
                        + payloadHash;

                String stringToSign = "AWS4-HMAC-SHA256\n"
                        + dateTimeStr + "\n"
                        + credentialScope + "\n"
                        + sha256(canonicalRequest);

                // 4. Derive Signing Key
                byte[] signingKey = getSigningKey(dateStr);
                String signature = bytesToHex(hmacSha256(signingKey, stringToSign));

                String authorization = "AWS4-HMAC-SHA256 Credential=" + ACCESS_KEY + "/" + credentialScope
                        + ", SignedHeaders=" + signedHeaders
                        + ", Signature=" + signature;

                // 5. Perform Upload
                URL url = new URL(ENDPOINT + canonicalUri);
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("PUT");
                conn.setDoOutput(true);
                conn.setFixedLengthStreamingMode(fileSize);

                // Set headers exactly as signed
                conn.setRequestProperty("Content-Type", contentType);
                conn.setRequestProperty("Host", host);
                conn.setRequestProperty("x-amz-content-sha256", payloadHash);
                conn.setRequestProperty("x-amz-date", dateTimeStr);
                conn.setRequestProperty("Authorization", authorization);
                
                conn.setConnectTimeout(30000);
                conn.setReadTimeout(120000);

                is = context.getContentResolver().openInputStream(fileUri);
                if (is == null) throw new Exception("Could not open input stream");

                OutputStream os = conn.getOutputStream();
                byte[] buffer = new byte[16384];
                int bytesRead;
                while ((bytesRead = is.read(buffer)) != -1) {
                    os.write(buffer, 0, bytesRead);
                }
                os.flush();
                os.close();

                int responseCode = conn.getResponseCode();
                if (responseCode >= 200 && responseCode < 300) {
                    callback.onSuccess(PUBLIC_BASE_URL + "/" + key);
                } else {
                    String errorBody = readResponseBody(conn);
                    Log.e(TAG, "R2 Upload Failed (" + responseCode + "): " + errorBody);
                    callback.onError("Upload failed (" + responseCode + "): " + trimError(errorBody));
                }
            } catch (FileNotFoundException e) {
                Log.e(TAG, "Selected file was not found", e);
                callback.onError("Selected file could not be opened");
            } catch (Exception e) {
                Log.e(TAG, "R2 Upload Error", e);
                callback.onError("Error: " + e.getMessage());
            } finally {
                if (is != null) try { is.close(); } catch (Exception ignored) {}
                if (conn != null) conn.disconnect();
            }
        });
    }

    private String calculatePayloadHash(Context context, Uri uri) {
        try (InputStream is = context.getContentResolver().openInputStream(uri)) {
            if (is == null) return null;
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[16384];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                digest.update(buffer, 0, bytesRead);
            }
            return bytesToHex(digest.digest());
        } catch (Exception e) {
            Log.e(TAG, "Hash calculation error", e);
            return null;
        }
    }

    private byte[] getSigningKey(String date) throws Exception {
        byte[] kDate = hmacSha256(("AWS4" + SECRET_KEY).getBytes(StandardCharsets.UTF_8), date);
        byte[] kRegion = hmacSha256(kDate, REGION);
        byte[] kService = hmacSha256(kRegion, SERVICE);
        return hmacSha256(kService, "aws4_request");
    }

    private byte[] hmacSha256(byte[] key, String data) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(key, "HmacSHA256"));
        return mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
    }

    private String sha256(String data) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        return bytesToHex(digest.digest(data.getBytes(StandardCharsets.UTF_8)));
    }

    private String readResponseBody(HttpURLConnection conn) {
        try (InputStream es = conn.getErrorStream() != null ? conn.getErrorStream() : conn.getInputStream()) {
            if (es == null) return "No error body";
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int length;
            while ((length = es.read(buffer)) != -1) {
                baos.write(buffer, 0, length);
            }
            return baos.toString(StandardCharsets.UTF_8.name());
        } catch (Exception e) {
            return "Error reading error stream: " + e.getMessage();
        }
    }

    private long getFileSize(Context context, Uri uri) {
        try (android.database.Cursor cursor = context.getContentResolver().query(uri, null, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int sizeIndex = cursor.getColumnIndex(android.provider.OpenableColumns.SIZE);
                if (sizeIndex >= 0 && !cursor.isNull(sizeIndex)) {
                    return cursor.getLong(sizeIndex);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "File size error", e);
        }
        try (InputStream is = context.getContentResolver().openInputStream(uri)) {
            if (is == null) return -1;
            long total = 0;
            byte[] buffer = new byte[16384];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                total += bytesRead;
            }
            return total;
        } catch (Exception e) {
            Log.e(TAG, "File size fallback error", e);
        }
        return -1;
    }

    private String getExtension(Context context, Uri uri, String fallback) {
        String mimeType = context.getContentResolver().getType(uri);
        if (mimeType != null) {
            if (mimeType.contains("png")) return ".png";
            if (mimeType.contains("jpeg") || mimeType.contains("jpg")) return ".jpg";
            if (mimeType.contains("webp")) return ".webp";
            if (mimeType.contains("json")) return ".json";
        }
        String guessed = URLConnection.guessContentTypeFromName(uri.getLastPathSegment());
        if ("application/json".equals(guessed)) return ".json";
        if ("image/png".equals(guessed)) return ".png";
        if ("image/webp".equals(guessed)) return ".webp";
        if ("image/jpeg".equals(guessed)) return ".jpg";
        return fallback;
    }

    private String getMimeType(String key) {
        String k = key.toLowerCase();
        if (k.endsWith(".png")) return "image/png";
        if (k.endsWith(".webp")) return "image/webp";
        if (k.endsWith(".json")) return "application/json";
        if (k.endsWith(".bin")) return "application/octet-stream";
        return "image/jpeg";
    }

    private String trimError(String body) {
        if (body == null || body.trim().isEmpty()) return "No response details";
        String singleLine = body.replace('\n', ' ').replace('\r', ' ').trim();
        return singleLine.length() > 180 ? singleLine.substring(0, 180) + "..." : singleLine;
    }

    private String getDateStr() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd", Locale.US);
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        return sdf.format(new Date());
    }

    private String getDateTimeStr() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'", Locale.US);
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        return sdf.format(new Date());
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
