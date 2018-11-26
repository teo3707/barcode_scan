package com.apptreesoftware.barcodescan;

import android.app.Activity;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.support.v4.content.FileProvider;

import java.io.File;

/**
 * Providing a custom {@code FileProvider} prevents manifest {@code <provider>} name collisions.
 *
 * <p>See https://developer.android.com/guide/topics/manifest/provider-element.html for details.
 */
public class ImagePickerFileProvider extends FileProvider {}


class FileUriResolver {

    interface OnPathReadyListener {
        void onPathReady(String path);
    }

    Activity activity;

    FileUriResolver(Activity activity) {
        this.activity = activity;
    }

    public Uri resolveFileProviderUriForFile(String fileProviderName, File file) {
        return FileProvider.getUriForFile(activity, fileProviderName, file);
    }

    public void getFullImagePath(final Uri imageUri, final OnPathReadyListener listener) {
        MediaScannerConnection.scanFile(
                activity,
                new String[] {imageUri.getPath()},
                null,
                new MediaScannerConnection.OnScanCompletedListener() {
                    @Override
                    public void onScanCompleted(String path, Uri uri) {
                        listener.onPathReady(path);
                    }
                });
    }
}