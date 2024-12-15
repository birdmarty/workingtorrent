package com.example.myapplication;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.github.se_bastiaan.torrentstream.StreamStatus;
import com.github.se_bastiaan.torrentstream.Torrent;
import com.github.se_bastiaan.torrentstream.TorrentOptions;
import com.github.se_bastiaan.torrentstream.TorrentStream;
import com.github.se_bastiaan.torrentstream.listeners.TorrentListener;

import java.io.File;

public class MainActivity extends AppCompatActivity implements TorrentListener {
    private static final String TAG = "MainActivity";
    private static final int STORAGE_PERMISSION_CODE = 101;

    private ProgressBar progressbar;
    private TorrentStream torrentStream;
    private TextView statusText;
    private Button startButton;
    private Handler progressHandler;
    private long lastToastTime = 0;

    private static final String TORRENT_URL = "magnet:?xt=urn:btih:88594aaacbde40ef3e2510c47374ec0aa396c08e&dn=bbb%5Fsunflower%5F1080p%5F30fps%5Fnormal.mp4&tr=udp%3A%2F%2Ftracker.openbittorrent.com%3A80%2Fannounce&tr=udp%3A%2F%2Ftracker.publicbt.com%3A80%2Fannounce&ws=http%3A%2F%2Fdistribution.bbb3d.renderfarming.net%2Fvideo%2Fmp4%2Fbbb%5Fsunflower%5F1080p%5F30fps%5Fnormal.mp4";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        // Initialize views and handler
        statusText = findViewById(R.id.status_text);
        startButton = findViewById(R.id.start_button);
        progressbar = findViewById(R.id.progressBar2);
        progressHandler = new Handler();

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Request permissions immediately
        requestPermissions();
    }

    private void requestPermissions() {
        String[] permissions = {
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.INTERNET
        };

        boolean allPermissionsGranted = true;
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                allPermissionsGranted = false;
                break;
            }
        }

        if (!allPermissionsGranted) {
            ActivityCompat.requestPermissions(this, permissions, STORAGE_PERMISSION_CODE);
        } else {
            initTorrentStream();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == STORAGE_PERMISSION_CODE) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }

            if (allGranted) {
                initTorrentStream();
                Toast.makeText(this, "Permissions granted, initializing TorrentStream", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Permissions required for downloading", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void initTorrentStream() {
        try {
            // Create download directory
            File saveLocation = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "TorrentStream");
            boolean dirCreated = saveLocation.mkdirs();
            Log.d(TAG, "Directory created: " + dirCreated + ", Path: " + saveLocation.getAbsolutePath());

            // Initialize TorrentStream with options
            TorrentOptions torrentOptions = new TorrentOptions.Builder()
                    .saveLocation(saveLocation)
                    .removeFilesAfterStop(false)
                    .maxConnections(200)
                    .maxDownloadSpeed(0) // No limit
                    .maxUploadSpeed(0) // No limit
                    .build();

            torrentStream = TorrentStream.init(torrentOptions);

            if (torrentStream != null) {
                torrentStream.addListener(this);
                Toast.makeText(this, "TorrentStream initialized successfully", Toast.LENGTH_SHORT).show();
                Log.d(TAG, "TorrentStream initialized successfully");
            } else {
                Toast.makeText(this, "Failed to initialize TorrentStream", Toast.LENGTH_SHORT).show();
                Log.e(TAG, "TorrentStream initialization returned null");
            }
        } catch (Exception e) {
            String errorMsg = "Error initializing TorrentStream: " + e.getMessage();
            Toast.makeText(this, errorMsg, Toast.LENGTH_LONG).show();
            Log.e(TAG, errorMsg, e);
        }
    }

    public void download(View view) {
        if (torrentStream == null) {
            Toast.makeText(this, "TorrentStream not initialized. Retrying initialization...", Toast.LENGTH_SHORT).show();
            initTorrentStream();
            return;
        }

        if (!torrentStream.isStreaming()) {
            try {
                view.setEnabled(false);
                Toast.makeText(this, "Starting download...", Toast.LENGTH_SHORT).show();
                Log.d(TAG, "Starting torrent stream with URL: " + TORRENT_URL);
                torrentStream.startStream(TORRENT_URL);
            } catch (Exception e) {
                String errorMsg = "Error starting download: " + e.getMessage();
                Toast.makeText(this, errorMsg, Toast.LENGTH_LONG).show();
                Log.e(TAG, errorMsg, e);
                view.setEnabled(true);
            }
        } else {
            Toast.makeText(this, "Download already in progress", Toast.LENGTH_SHORT).show();
        }
    }

    private void showProgressToast(String message) {
        long currentTime = System.currentTimeMillis();
        // Only show toast if more than 1 second has passed since last toast
        if (currentTime - lastToastTime >= 1000) {
            runOnUiThread(() -> {
                Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show();
            });
            lastToastTime = currentTime;
        }
    }

    private void updateStatus(String status) {
        runOnUiThread(() -> {
            statusText.setText(status);
            Log.d(TAG, status);
        });
    }

    // TorrentListener implementation
    @Override
    public void onStreamPrepared(Torrent torrent) {
        updateStatus("Stream prepared");
        showProgressToast("Torrent stream prepared");
    }

    @Override
    public void onStreamStarted(Torrent torrent) {
        updateStatus("Stream started");
        showProgressToast("Download started");
    }

    @Override
    public void onStreamError(Torrent torrent, Exception e) {
        String errorMessage = "Error: " + e.getMessage();
        updateStatus(errorMessage);
        showProgressToast(errorMessage);
        runOnUiThread(() -> startButton.setEnabled(true));
    }

    @Override
    public void onStreamReady(Torrent torrent) {
        updateStatus("Stream ready");
        showProgressToast("Stream ready to start downloading");
    }

    @Override
    public void onStreamProgress(Torrent torrent, StreamStatus status) {
        String progress = String.format("Progress: %.2f%% Speed: %.2f MB/s Seeds: %d",
                status.progress * 100,
                status.downloadSpeed / 1024.0 / 1024.0,
                status.seeds);
        updateStatus(progress);
        showProgressToast(String.format("Downloaded: %.1f%%", status.progress * 100));
    }

    @Override
    public void onStreamStopped() {
        updateStatus("Stream stopped");
        showProgressToast("Download complete!");
        runOnUiThread(() -> startButton.setEnabled(true));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (torrentStream != null) {
            torrentStream.removeListener(this);
            torrentStream.stopStream();
        }
        progressHandler.removeCallbacksAndMessages(null);
    }
}