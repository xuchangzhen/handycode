package com.fairycrane.handycode;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.OpenableColumns;
import java.io.File;
import java.io.FileNotFoundException;

/** Read-only access to the one verified APK, only through a temporary URI grant. */
public final class UpdateFileProvider extends ContentProvider {
    private static final String PATH = "/verified.apk";
    static final Uri URI = Uri.parse("content://com.fairycrane.handycode.updates" + PATH);
    static File apk(android.content.Context context) { return new File(context.getCacheDir(), "verified-update.apk"); }
    @Override public boolean onCreate() { return true; }
    private void validate(Uri uri) throws FileNotFoundException {
        if (uri == null || !URI.equals(uri)) throw new FileNotFoundException("Invalid update URI");
    }
    @Override public String getType(Uri uri) { return URI.equals(uri) ? "application/vnd.android.package-archive" : null; }
    @Override public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException {
        validate(uri);
        if (!"r".equals(mode)) throw new FileNotFoundException("Read only");
        return ParcelFileDescriptor.open(apk(getContext()), ParcelFileDescriptor.MODE_READ_ONLY);
    }
    @Override public Cursor query(Uri uri, String[] projection, String selection, String[] args, String sort) {
        if (!URI.equals(uri)) return null;
        String[] cols = projection == null ? new String[]{OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE} : projection;
        MatrixCursor cursor = new MatrixCursor(cols); Object[] row = new Object[cols.length];
        for (int i=0;i<cols.length;i++) {
            if (OpenableColumns.DISPLAY_NAME.equals(cols[i])) row[i] = "HandyCode-update.apk";
            else if (OpenableColumns.SIZE.equals(cols[i])) row[i] = apk(getContext()).length();
        }
        cursor.addRow(row); return cursor;
    }
    @Override public Uri insert(Uri uri, ContentValues values) { throw new UnsupportedOperationException(); }
    @Override public int delete(Uri uri, String selection, String[] args) { throw new UnsupportedOperationException(); }
    @Override public int update(Uri uri, ContentValues values, String selection, String[] args) { throw new UnsupportedOperationException(); }
}
