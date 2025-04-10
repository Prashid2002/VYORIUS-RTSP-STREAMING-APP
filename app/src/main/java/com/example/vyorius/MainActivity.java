package com.example.vyorius;

import android.app.PictureInPictureParams;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Rational;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import org.videolan.libvlc.LibVLC;
import org.videolan.libvlc.Media;
import org.videolan.libvlc.MediaPlayer;
import org.videolan.libvlc.util.VLCVideoLayout;

import java.util.ArrayList;

import com.arthenica.ffmpegkit.FFmpegKit;
import com.arthenica.ffmpegkit.FFmpegSession;
import com.arthenica.ffmpegkit.ReturnCode;


public class MainActivity extends AppCompatActivity {

    private VLCVideoLayout videoLayout;
    private LibVLC libVLC;
    private MediaPlayer mediaPlayer;
    private EditText rtspURL;
    private Button playStream, stopBtn, recordBtn;
    private ImageView popOut;
    private boolean isRecording = false;
    private String recordingFilePath;
    private FFmpegSession ffmpegSession;
    private boolean isViewAttached = false; // NEW

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        // Initialize UI
        playStream = findViewById(R.id.playBtn);
        stopBtn = findViewById(R.id.stopBtn);
        recordBtn = findViewById(R.id.recordBtn);
        rtspURL = findViewById(R.id.etRtspUrl);
        popOut = findViewById(R.id.popout);

        FrameLayout videoContainer = findViewById(R.id.videoLayout);
        videoLayout = new VLCVideoLayout(this);
        videoContainer.addView(videoLayout);

        ArrayList<String> options = new ArrayList<>();
        options.add("--network-caching=150");
        options.add("--no-drop-late-frames");
        options.add("--no-skip-frames");

        libVLC = new LibVLC(this, options);
        mediaPlayer = new MediaPlayer(libVLC);

        // Attach views only once
        mediaPlayer.attachViews(videoLayout, null, false, false);
        isViewAttached = true;

        playStream.setOnClickListener(v -> playStream());
        stopBtn.setOnClickListener(v -> stopStream());
        recordBtn.setOnClickListener(v -> toggleRecording());
        popOut.setOnClickListener(v -> enterPiPMode());
    }

    private void playStream() {
        String url = rtspURL.getText().toString().trim();
        if (url.isEmpty()) {
            Toast.makeText(this, "Please enter RTSP URL", Toast.LENGTH_SHORT).show();
            return;
        }

        if (mediaPlayer.isPlaying()) {
            mediaPlayer.stop(); // Stop if already playing
        }

        // Set media and play
        Media media = new Media(libVLC, Uri.parse(url));
        media.setHWDecoderEnabled(true, false);
        media.addOption(":network-caching=150");
        media.addOption(":clock-jitter=0");
        media.addOption(":clock-synchro=0");

        mediaPlayer.setMedia(media);
        mediaPlayer.play();

        Toast.makeText(this, "Streaming started", Toast.LENGTH_SHORT).show();
    }

    private void stopStream() {
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.stop();
            Toast.makeText(this, "Stream stopped", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "No stream to stop", Toast.LENGTH_SHORT).show();
        }
    }

    private void toggleRecording() {
        String url = rtspURL.getText().toString().trim();
        if (url.isEmpty()) {
            Toast.makeText(this, "Enter URL first", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!isRecording) {
            recordingFilePath = getExternalFilesDir(null) + "/stream_" + System.currentTimeMillis() + ".mp4";
            String command = "-i \"" + url + "\" -c copy -f mp4 \"" + recordingFilePath + "\"";

            ffmpegSession = FFmpegKit.executeAsync(command, session -> {
                if (ReturnCode.isSuccess(session.getReturnCode())) {
                    runOnUiThread(() -> Toast.makeText(this, "Recording saved", Toast.LENGTH_SHORT).show());
                } else {
                    runOnUiThread(() -> Toast.makeText(this, "Recording failed", Toast.LENGTH_SHORT).show());
                }
            });

            isRecording = true;
            recordBtn.setText("Stop Recording");
            Toast.makeText(this, "Recording started", Toast.LENGTH_SHORT).show();
        } else {
            if (ffmpegSession != null) {
                ffmpegSession.cancel();
            }

            isRecording = false;
            recordBtn.setText("Record");

            MediaScannerConnection.scanFile(
                    this,
                    new String[]{recordingFilePath},
                    new String[]{"video/mp4"},
                    null
            );

            Toast.makeText(this, "Recording stopped & saved", Toast.LENGTH_SHORT).show();
        }
    }

    private void enterPiPMode() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)) {
                Toast.makeText(this, "PiP not supported on this device", Toast.LENGTH_SHORT).show();
                return;
            }

            Rational aspectRatio = new Rational(videoLayout.getWidth(), videoLayout.getHeight());
            PictureInPictureParams pipParams = new PictureInPictureParams.Builder()
                    .setAspectRatio(aspectRatio)
                    .build();
            enterPictureInPictureMode(pipParams);
        } else {
            Toast.makeText(this, "PiP requires Android 8.0+", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onUserLeaveHint() {
        super.onUserLeaveHint();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && mediaPlayer.isPlaying()) {
            enterPiPMode();
        }
    }

    @Override
    public void onPictureInPictureModeChanged(boolean isInPictureInPictureMode, Configuration newConfig) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig);
        // Optional UI logic when PiP mode changes
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (mediaPlayer != null) {
            if (mediaPlayer.isPlaying()) {
                mediaPlayer.stop();
            }
            mediaPlayer.detachViews(); // DETACH to prevent crash
            mediaPlayer.release();
            mediaPlayer = null;
        }

        if (libVLC != null) {
            libVLC.release();
            libVLC = null;
        }
    }
}
