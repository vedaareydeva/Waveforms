package com.example.waveforms;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;

public class MicrophoneInput extends AppCompatActivity {

    private LineChart lineChart;
    private LineDataSet dataSet;
    private LineData lineData;

    Button Start, Stop, Exit;

    private AudioRecord audioRecord;
    private final MediaPlayer mediaPlayer = new MediaPlayer();
    private Thread recordingThread;
    private static final int BUFFER_SIZE = AudioRecord.getMinBufferSize(Constants.RECORDER_SAMPLERATE, Constants.RECORDER_CHANNELS, Constants.RECORDER_AUDIO_ENCODING );
    private final AtomicBoolean isRecording = new AtomicBoolean(false);
    private final AtomicBoolean isPlayingAudio = new AtomicBoolean(false);

    private String fileName = null;
    private int recordingCount = 0;
    private Timer timer;

    // Just added a list to see the audio files
    private RecyclerView recyclerView;
    private List<AudioModel> audioModelList;
    private AudioAdapter audioAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_microphone_input);

        lineChart = findViewById(R.id.lineChart);
        lineChart.getDescription().setEnabled(false);
        lineChart.getAxisLeft().setEnabled(false);
        lineChart.getAxisRight().setEnabled(false);
        lineChart.getXAxis().setEnabled(false);

        Start = findViewById(R.id.BtnStart);
        Stop = findViewById(R.id.BtnStop);
        Exit = findViewById(R.id.BtnExit);

        // Assigned the recycler view to ids
        recyclerView = findViewById(R.id.AudioList);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        audioModelList = new ArrayList<>();
        audioAdapter = new AudioAdapter(audioModelList,
            new AudioAdapter.ClickListener() {
                // click to play audio file
                @Override
                public void onItemClick(int position, View view) {
                    AudioModel audioModel = audioModelList.get(position);
                    File audioFile = audioModel.getAudioSource();
                    handleAudio(audioFile, view);
                }
                // long click to delete audio file
                @Override
                public void onItemLongClick(int position, View v) {
                    deleteAudioFile(position);
                }
            });
        recyclerView.setAdapter(audioAdapter);
        readFiles();

        // timer is better than handler as it doesnt block the main thread
        timer = new Timer();
        Exit.setOnClickListener(view -> {
            Intent intent = new Intent(MicrophoneInput.this, MainActivity.class);
            startActivity(intent);
        });

        Start.setOnClickListener(view -> {
            if (recordingCount < 5) {
                startRecording();
            } else {
                Toast.makeText(MicrophoneInput.this, "You have reached the recording limit.", Toast.LENGTH_SHORT).show();
            }
        });

        Stop.setOnClickListener(view -> stopRecording());

        dataSet = new LineDataSet(null, "Audio Data");
        dataSet.setColor(getResources().getColor(R.color.black));
        dataSet.setDrawCircles(false);
        dataSet.setLineWidth(2f);

        lineData = new LineData(dataSet);
        lineChart.setData(lineData);
    }

    public void handleAudio(File audioFile, View view)  {
        // this method is used to play audio file and change the play icon to pause icon and vice versa
        if(isPlayingAudio.get()) {
            Toast.makeText(MicrophoneInput.this, "Audio is already playing.", Toast.LENGTH_SHORT).show();
        }else {
            isPlayingAudio.set(true);
            audioAdapter.handlePlayIcon(view, true);
            handleVisibilityIcon();
            view.findViewById(R.id.PlayIcon).setVisibility(View.VISIBLE);
            try{
                mediaPlayer.reset();
                mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
                mediaPlayer.setDataSource(audioFile.getAbsolutePath());
                mediaPlayer.prepare();
                mediaPlayer.start();
            } catch (IOException e) {
                e.printStackTrace();
                isPlayingAudio.set(false);
                audioAdapter.handlePlayIcon(view, false);
                handleVisibilityIcon();
                Toast.makeText(MicrophoneInput.this, "Failed to play audio.", Toast.LENGTH_SHORT).show();
            }
        }
        // added some listeners to handle errors and completion of audio
        mediaPlayer.setOnErrorListener((mp, what, extra) -> {
            isPlayingAudio.set(false);
            audioAdapter.handlePlayIcon(view, false);
            handleVisibilityIcon();
            Toast.makeText(MicrophoneInput.this, "Failed to play audio.", Toast.LENGTH_SHORT).show();
            return false;
        });
        mediaPlayer.setOnCompletionListener(mp -> {
            isPlayingAudio.set(false);
            audioAdapter.handlePlayIcon(view, false);
            handleVisibilityIcon();
        });
    }

    @Override
    protected void onStop() {
        super.onStop();
        timer.cancel();
        finish();
    }

    private void handleVisibilityIcon() {
        // this method is used to make all the play icons invisible when audio is playing
        if(isPlayingAudio.get()) {
            //make all other icons invisible
            for(int i = 0; i < recyclerView.getChildCount(); i++) {
                View childView = recyclerView.getChildAt(i);
                ImageView playIcon = childView.findViewById(R.id.PlayIcon);
                playIcon.setVisibility(View.GONE);
            }
        }else {
            //make all icons visible
            for(int i = 0; i < recyclerView.getChildCount(); i++) {
                View childView = recyclerView.getChildAt(i);
                ImageView playIcon = childView.findViewById(R.id.PlayIcon);
                playIcon.setVisibility(View.VISIBLE);
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Start.setEnabled(true);
        Stop.setEnabled(false);
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopRecording();
    }

    private void startRecording() {
        // Start recording audio
        if (!isRecording.get()) {
            // Start a new recording
            isRecording.set(true);
            // check for microphone permission
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    this.requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, 1234);
                }
            } else {
                audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC,Constants.RECORDER_SAMPLERATE, Constants.RECORDER_CHANNELS ,Constants.RECORDER_AUDIO_ENCODING ,BUFFER_SIZE );
                audioRecord.startRecording();
                fileName = "Recording_" + System.currentTimeMillis() + ".pcm";
                isRecording.set(true);
                recordingThread = new Thread(new RecordingRunnable(), "Recording Thread");
                recordingThread.start();
                Start.setEnabled(false);
                Stop.setEnabled(true);
                updateGraph();
            }

        }
    }

    private void stopRecording() {
        // Stop recording audio
        if (isRecording.get() && audioRecord != null) {
            // Stop the recording
            isRecording.set(false);
            audioRecord.stop();
            recordingCount++;
            Start.setEnabled(true);
            Stop.setEnabled(true);
            if (recordingCount == 5) {
                // You've reached the recording limit, disable the Start button
                Start.setEnabled(false);
            }
            saveAudioToFile();
            addNewAudioFile();
            clearGraphData();
//          uploadAudio(audioData);
            audioRecord.release();
            audioRecord = null;
            recordingThread = null;
        }
    }

    public void readFiles() {
        // Read all the audio files from the external storage
        File[] files = Objects.requireNonNull(getExternalFilesDir(null)).listFiles();
        if (files == null) {
            return;
        }
        for (File file : files) {
            Log.println(Log.DEBUG, "File", file.getName());
            String fileName = file.getName();
            String duration = getAudioDuration(file);
            String extension = fileName.substring(fileName.lastIndexOf(".") + 1);
            String dateOfFileCreated = String.format(Locale.ROOT, "%1$td/%1$tm/%1$tY", file.lastModified());
            AudioModel audioModel = new AudioModel(fileName, file, extension, duration, dateOfFileCreated);
            audioModelList.add(audioModel);
        }
        audioAdapter.notifyDataSetChanged();
    }

    public void addNewAudioFile() {
        // Add the newly recorded audio file to the list
        File[] files = Objects.requireNonNull(getExternalFilesDir(null)).listFiles();
        if (files == null || files.length == 0) {
            return;
        }
        File file = files[files.length - 1];
        String fileName = file.getName();
        String duration = getAudioDuration(file);
        String extension = fileName.substring(fileName.lastIndexOf(".") + 1);
        String dateOfFileCreated = String.format(Locale.ROOT, "%1$td/%1$tm/%1$tY", file.lastModified());
        AudioModel audioModel = new AudioModel(fileName, file, extension, duration, dateOfFileCreated);
        audioModelList.add(audioModel);
        audioAdapter.notifyItemChanged(audioModelList.size() - 1);
    }


    private String getAudioDuration(File file) {
        // Retrieve audio duration using MediaMetadataRetriever ( helper class from android to get duration of wav file)
        try {
            MediaMetadataRetriever retriever = new MediaMetadataRetriever();
            retriever.setDataSource(this, Uri.fromFile(file));
            String duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
            if (duration == null) {
                return "--:--";
            }
            long durationMillis = Long.parseLong(duration);
            long seconds = durationMillis / 1000;
            long minutes = seconds / 60;
            seconds %= 60;
            return String.format(Locale.ROOT, "%02d:%02d", minutes, seconds);
        } catch (Exception e) {
            e.printStackTrace();
            return "--:--";
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        // the correct way to request permission and handle a function after it is sucessfully granted
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1234) {// If request is cancelled, the result arrays are empty.
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startRecording();

            } else {
                Toast.makeText(MicrophoneInput.this, "Permission denied to record audio", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void saveAudioToFile() {
        // pcm(raw) to wav conversion and save to external storage
        File file = new File(getExternalCacheDir(), fileName);
        String savingFileName = "Recording_" + System.currentTimeMillis() + ".wav";
        File savingFile = new File(getExternalFilesDir(null), savingFileName);
        try{
            Utils.rawToWave(file, savingFile.getAbsolutePath(), Constants.RECORDER_SAMPLERATE);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void uploadAudio(short[] audioData) {
        // I havent tested this method yet, but try it out and see if it works correctly (take care of file extensions and other things)
        FirebaseStorage storage = FirebaseStorage.getInstance();
        StorageReference storageRef = storage.getReference();
        String uniqueIdentifier = String.valueOf(System.currentTimeMillis());
        String fileName = "Recording_" + uniqueIdentifier + ".mp3";

        File audioFile = new File(getExternalFilesDir(null), fileName);
        Uri fileUri = Uri.fromFile(audioFile);

        // Upload the audio file to Firebase Storage
        StorageReference audioRef = storageRef.child("audio/" + fileName);
        UploadTask uploadTask = audioRef.putFile(fileUri);

        uploadTask.addOnSuccessListener(taskSnapshot -> {
            // Audio file uploaded successfully
            Toast.makeText(MicrophoneInput.this, "Audio uploaded to Firebase.", Toast.LENGTH_SHORT).show();
        }).addOnFailureListener(e -> {
            // Handle the upload failure
            Toast.makeText(MicrophoneInput.this, "Failed to upload audio to Firebase.", Toast.LENGTH_SHORT).show();
        });
    }

    private class RecordingRunnable implements Runnable {
        /*
            This a thread class which runs concurrently with the main thread to record audio
            and save it to a pcm file which is then converted to wav file and saved to external storage
         */
        @Override
        public void run() {
            final File file = new File(getExternalCacheDir(), fileName);
            final ByteBuffer buffer = ByteBuffer.allocateDirect(BUFFER_SIZE);

            try (final FileOutputStream outStream = new FileOutputStream(file)) {
                while (isRecording.get()) {
                    int result = audioRecord.read(buffer, BUFFER_SIZE);
                    if (result < 0) {
                        throw new RuntimeException("Reading of audio buffer failed: " +
                                getBufferReadFailureReason(result));
                    }
                    outStream.write(buffer.array(), 0, BUFFER_SIZE);
                    buffer.clear();
                }
            } catch (IOException e) {
                throw new RuntimeException("Writing of recorded audio failed", e);
            }
        }

        private String getBufferReadFailureReason(int errorCode) {
            switch (errorCode) {
                case AudioRecord.ERROR_INVALID_OPERATION:
                    return "ERROR_INVALID_OPERATION";
                case AudioRecord.ERROR_BAD_VALUE:
                    return "ERROR_BAD_VALUE";
                case AudioRecord.ERROR_DEAD_OBJECT:
                    return "ERROR_DEAD_OBJECT";
                case AudioRecord.ERROR:
                    return "ERROR";
                default:
                    return "Unknown (" + errorCode + ")";
            }
        }
    }

    private void deleteAudioFile(int position) {
        // Delete the audio file from the external storage
        AudioModel audioModel = audioModelList.get(position);
        File audioFile = audioModel.getAudioSource();
        audioFile.delete();
        audioModelList.remove(position);
        audioAdapter.notifyItemRemoved(position);
        audioAdapter.notifyItemRangeChanged(position, audioModelList.size());
    }


    private void updateGraph() {
        // Update the graph with the audio data
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if (isRecording.get()) {
                    short[] audioData = new short[BUFFER_SIZE];
                    audioRecord.read(audioData, 0, BUFFER_SIZE);
                    List<Entry> entries = new ArrayList<>();
                    for (int i = 0; i < audioData.length; i++) {
                        entries.add(new Entry(i, audioData[i]));
                    }
                    dataSet.setValues(entries);
                    lineData.notifyDataChanged();
                    lineChart.notifyDataSetChanged();
                    lineChart.postInvalidate();
                }
            }
        }, 0, 16);
    }

    private void clearGraphData() {
        dataSet.clear(); // Clear the data set
        lineData.notifyDataChanged();
        lineChart.notifyDataSetChanged();
        lineChart.invalidate();
    }


    @Override
    protected void onDestroy() {
        // Stop the recording and release the audio record object
        super.onDestroy();
        if (audioRecord != null) {
            if (audioRecord.getRecordingState() != AudioRecord.RECORDSTATE_STOPPED) {
                audioRecord.stop();
            }
            audioRecord.release();
        }
    }
}

