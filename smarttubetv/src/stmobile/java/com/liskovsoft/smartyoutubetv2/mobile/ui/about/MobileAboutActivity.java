package com.liskovsoft.smartyoutubetv2.mobile.ui.about;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.liskovsoft.smartyoutubetv2.mobile.update.MobileUpdateChecker;
import com.liskovsoft.smartyoutubetv2.mobile.update.MobileApkInstaller;
import com.liskovsoft.sharedutils.helpers.Helpers;
import com.liskovsoft.smartyoutubetv2.tv.BuildConfig;
import com.liskovsoft.smartyoutubetv2.tv.R;

/**
 * About screen — the fork-attribution surface. Shows app name, current version (from
 * the stmobile flavor block in build.gradle, NOT upstream defaultConfig), and explicit
 * credit + links to upstream SmartTube and this fork. This is the in-app "this is a
 * fork, not original code" disclosure.
 *
 * "Check for updates" does a real, scheme-aware check against the fork's GitHub releases
 * (see {@link MobileUpdateChecker}): it parses the SmarterTube version out of each release
 * tag, honours the release channel, and picks the APK for this device's ABI — rather than
 * the upstream AppUpdateChecker, which points at SmartTube's own (TV) release URLs.
 */
public class MobileAboutActivity extends AppCompatActivity {
    private boolean mChecking;
    private MobileApkInstaller mApkInstaller;
    private AlertDialog mDownloadDialog;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.mobile_about_activity);
        mApkInstaller = new MobileApkInstaller(this);

        ((TextView) findViewById(R.id.about_version))
                .setText(getString(R.string.mobile_about_version, BuildConfig.VERSION_NAME));

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        findViewById(R.id.link_upstream).setOnClickListener(v ->
                openUrl(getString(R.string.mobile_about_url_upstream)));
        findViewById(R.id.link_fork).setOnClickListener(v ->
                openUrl(getString(R.string.mobile_about_url_fork)));

        findViewById(R.id.btn_check_updates).setOnClickListener(v -> checkForUpdates());

        findViewById(R.id.btn_support_kofi).setOnClickListener(v ->
                openUrl(getString(R.string.mobile_about_url_kofi)));
        findViewById(R.id.btn_support_buymeacoffee).setOnClickListener(v ->
                openUrl(getString(R.string.mobile_about_url_buymeacoffee)));
    }

    /**
     * Kicks off a scheme-aware update check. {@link MobileUpdateChecker} runs the network/parse
     * work off the main thread and calls back here on the main thread; we bail if finishing.
     */
    private void checkForUpdates() {
        if (mChecking) {
            return;
        }
        mChecking = true;
        ((TextView) findViewById(R.id.btn_check_updates)).setText(R.string.mobile_about_checking);

        MobileUpdateChecker.check(
                getString(R.string.mobile_about_url_releases_api),
                BuildConfig.VERSION_NAME,
                result -> {
                    mChecking = false;
                    if (isFinishing()) {
                        return;
                    }
                    ((TextView) findViewById(R.id.btn_check_updates))
                            .setText(R.string.mobile_about_check_updates);
                    showResult(result);
                });
    }

    private void showResult(MobileUpdateChecker.Result result) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        switch (result.status) {
            case UPDATE_AVAILABLE:
                builder.setMessage(getString(R.string.mobile_about_update_available, result.latestTag))
                        .setPositiveButton(R.string.mobile_about_download, (d, w) -> downloadAndInstall(result.assetUrl))
                        .setNeutralButton(R.string.mobile_about_open_releases,
                                (d, w) -> openUrl(releaseOrFallback(result.releaseUrl)))
                        .setNegativeButton(android.R.string.cancel, null);
                break;
            case NO_COMPATIBLE_ASSET:
                builder.setMessage(getString(R.string.mobile_about_no_asset))
                        .setPositiveButton(R.string.mobile_about_open_releases,
                                (d, w) -> openUrl(releaseOrFallback(result.releaseUrl)))
                        .setNegativeButton(android.R.string.cancel, null);
                break;
            case ERROR:
                builder.setMessage(R.string.mobile_about_check_failed)
                        .setPositiveButton(R.string.mobile_about_open_releases,
                                (d, w) -> openUrl(getString(R.string.mobile_about_url_releases)))
                        .setNegativeButton(android.R.string.cancel, null);
                break;
            case UP_TO_DATE:
            default:
                builder.setMessage(getString(R.string.mobile_about_up_to_date, BuildConfig.VERSION_NAME))
                        .setPositiveButton(android.R.string.ok, null);
                break;
        }
        builder.show();
    }

    private void downloadAndInstall(String assetUrl) {
        if (assetUrl == null || mApkInstaller.isRunning()) {
            return;
        }

        ProgressBar progress = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progress.setMax(100);
        progress.setIndeterminate(true);
        mDownloadDialog = new AlertDialog.Builder(this)
                .setTitle(R.string.mobile_about_downloading)
                .setMessage(R.string.mobile_about_downloading_message)
                .setView(progress)
                .setNegativeButton(android.R.string.cancel, (dialog, which) -> mApkInstaller.cancel())
                .setOnCancelListener(dialog -> mApkInstaller.cancel())
                .create();
        mDownloadDialog.show();

        mApkInstaller.download(assetUrl, new MobileApkInstaller.Listener() {
            @Override
            public void onProgress(int percent) {
                runOnUiThread(() -> {
                    if (mDownloadDialog != null && mDownloadDialog.isShowing()) {
                        progress.setIndeterminate(false);
                        progress.setProgress(percent);
                    }
                });
            }

            @Override
            public void onCompleted(java.io.File apk) {
                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) {
                        return;
                    }
                    dismissDownloadDialog();
                    Helpers.installPackage(MobileAboutActivity.this, apk.getAbsolutePath());
                });
            }

            @Override
            public void onFailed(Exception error) {
                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) {
                        return;
                    }
                    dismissDownloadDialog();
                    new AlertDialog.Builder(MobileAboutActivity.this)
                            .setMessage(R.string.mobile_about_download_failed)
                            .setPositiveButton(android.R.string.ok, null)
                            .show();
                });
            }

            @Override
            public void onCancelled() {
                runOnUiThread(MobileAboutActivity.this::dismissDownloadDialog);
            }
        });
    }

    private void dismissDownloadDialog() {
        if (mDownloadDialog != null) {
            mDownloadDialog.dismiss();
            mDownloadDialog = null;
        }
    }

    @Override
    protected void onDestroy() {
        if (mApkInstaller != null) {
            mApkInstaller.cancel();
        }
        dismissDownloadDialog();
        super.onDestroy();
    }

    /** Fall back to the fork releases page when a release has no notes URL. */
    private String releaseOrFallback(@Nullable String releaseUrl) {
        return releaseUrl != null ? releaseUrl : getString(R.string.mobile_about_url_releases);
    }

    private void openUrl(String url) {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        } catch (Exception ignored) {
        }
    }
}
