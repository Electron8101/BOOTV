package tv.bootv.app;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;

import java.io.File;
import java.io.FileNotFoundException;

/**
 * Lightweight, zero-dependency ContentProvider to securely expose downloaded APKs
 * to the Android Package Installer on Android 7.0 through Android 15+.
 */
public class AppFileProvider extends ContentProvider {

    public static final String AUTHORITY = "tv.bootv.app.fileprovider";

    public static Uri getUriForFile(Context context, File file) {
        return Uri.parse("content://" + AUTHORITY + "/" + file.getName());
    }

    @Override
    public boolean onCreate() {
        return true;
    }

    @Override
    public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException {
        Context context = getContext();
        if (context == null) throw new FileNotFoundException("Context unavailable");

        String fileName = uri.getLastPathSegment();
        if (fileName == null || !fileName.endsWith(".apk")) {
            throw new FileNotFoundException("Invalid file requested: " + uri);
        }

        File targetFile = new File(context.getCacheDir(), fileName);
        if (!targetFile.exists()) {
            // Also check external cache if present
            File extCache = context.getExternalCacheDir();
            if (extCache != null) {
                targetFile = new File(extCache, fileName);
            }
        }

        if (!targetFile.exists()) {
            throw new FileNotFoundException("File not found on disk: " + fileName);
        }

        return ParcelFileDescriptor.open(targetFile, ParcelFileDescriptor.MODE_READ_ONLY);
    }

    @Override
    public String getType(Uri uri) {
        return "application/vnd.android.package-archive";
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        return null;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        return 0;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        return 0;
    }
}
