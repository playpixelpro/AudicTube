package com.liskovsoft.smartyoutubetv2.mobile.update;

import android.content.Context;

import androidx.annotation.Nullable;

import com.liskovsoft.sharedutils.helpers.FileHelpers;
import com.liskovsoft.sharedutils.okhttp.OkHttpManager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import okhttp3.Response;
import okhttp3.ResponseBody;

/** Downloads the selected mobile APK into private cache storage for package installation. */
public final class MobileApkInstaller {
    private static final String APK_NAME = "audictube-update.apk";
    private static final int BUFFER_SIZE = 16 * 1024;

    public interface Listener {
        void onProgress(int percent);
        void onCompleted(File apk);
        void onFailed(Exception error);
        void onCancelled();
    }

    private final Context mContext;
    private volatile boolean mCancelled;
    private Thread mThread;

    public MobileApkInstaller(Context context) {
        mContext = context.getApplicationContext();
    }

    public synchronized boolean isRunning() {
        return mThread != null && mThread.isAlive();
    }

    public synchronized void download(String url, Listener listener) {
        if (isRunning() || url == null || url.length() == 0) {
            return;
        }

        mCancelled = false;
        mThread = new Thread(() -> downloadInBackground(url, listener), "MobileApkInstaller");
        mThread.start();
    }

    public synchronized void cancel() {
        mCancelled = true;
        if (mThread != null) {
            mThread.interrupt();
        }
    }

    private void downloadInBackground(String url, Listener listener) {
        File output = null;
        try {
            File cacheDir = FileHelpers.getCacheDir(mContext);
            if (cacheDir == null) {
                throw new IOException("App cache is unavailable");
            }
            output = new File(cacheDir, APK_NAME);
            if (output.exists() && !output.delete()) {
                throw new IOException("Unable to replace the previous update");
            }

            try (Response response = OkHttpManager.instance(false).doGetRequest(url)) {
                if (response == null || !response.isSuccessful()) {
                    int code = response != null ? response.code() : -1;
                    throw new IOException("APK download failed (HTTP " + code + ")");
                }
                ResponseBody body = response.body();
                if (body == null) {
                    throw new IOException("APK download returned no data");
                }
                writeBody(body, output, listener);
            }

            if (mCancelled) {
                delete(output);
                listener.onCancelled();
            } else if (!isApk(output)) {
                delete(output);
                throw new IOException("Downloaded file is not an APK");
            } else {
                listener.onCompleted(output);
            }
        } catch (Exception e) {
            if (mCancelled || e instanceof InterruptedException) {
                delete(output);
                listener.onCancelled();
            } else {
                delete(output);
                listener.onFailed(e);
            }
        } finally {
            synchronized (this) {
                mThread = null;
            }
        }
    }

    private void writeBody(ResponseBody body, File output, Listener listener) throws IOException {
        long total = body.contentLength();
        long downloaded = 0;
        int lastPercent = -1;
        byte[] buffer = new byte[BUFFER_SIZE];
        try (InputStream input = body.byteStream(); FileOutputStream file = new FileOutputStream(output)) {
            int count;
            while ((count = input.read(buffer)) != -1) {
                if (mCancelled || Thread.currentThread().isInterrupted()) {
                    throw new InterruptedException();
                }
                file.write(buffer, 0, count);
                downloaded += count;
                if (total > 0) {
                    int percent = (int) Math.min(100, downloaded * 100 / total);
                    if (percent != lastPercent) {
                        lastPercent = percent;
                        listener.onProgress(percent);
                    }
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Download cancelled", e);
        }
    }

    private static boolean isApk(@Nullable File file) throws IOException {
        if (file == null || !file.isFile() || file.length() < 4) {
            return false;
        }
        try (InputStream input = new java.io.FileInputStream(file)) {
            return input.read() == 'P' && input.read() == 'K';
        }
    }

    private static void delete(@Nullable File file) {
        if (file != null && file.exists()) {
            //noinspection ResultOfMethodCallIgnored
            file.delete();
        }
    }
}
