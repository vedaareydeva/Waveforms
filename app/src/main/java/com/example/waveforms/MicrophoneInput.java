package com.example.waveforms;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MicrophoneInput extends AppCompatActivity {

    private LineChart lineChart;
    private LineDataSet dataSet;
    private LineData lineData;

    Button Start;
    Button Stop;
    Button Exit;

    private boolean isRecording = false;
    private AudioRecord audioRecord;
    private int sampleRate = 44100;
    private int bufferSize = AudioRecord.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);

    private int recordingCount = 0;
    private List<short[]> recordedAudioDataList = new ArrayList<>();

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

        //set click listener
        Exit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MicrophoneInput.this, MainActivity.class);
                startActivity(intent);
            }
        });

        Start.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (recordingCount < 5) {
                    startRecording();
                } else {
                    Toast.makeText(MicrophoneInput.this, "You have reached the recording limit.", Toast.LENGTH_SHORT).show();
                }
            }
        });

        Stop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                stopRecording();

            }
        });

        dataSet = new LineDataSet(null, "Audio Data");
        dataSet.setColor(getResources().getColor(R.color.black));
        dataSet.setDrawCircles(false);
        dataSet.setLineWidth(2f);

        lineData = new LineData(dataSet);
        lineChart.setData(lineData);

        startRecording();
        updateGraph();

    }

    private void startRecording() {

        if (!isRecording) {

            // Start a new recording
            isRecording = true;

            if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return;
            }
            audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, bufferSize);
            audioRecord.startRecording();

            Start.setEnabled(false);
            Stop.setEnabled(true);

        }
    }

    private void stopRecording() {
        if (isRecording) {
            // Stop the recording
            isRecording = false;
            audioRecord.stop();

            short[] audioData = new short[bufferSize];
            audioRecord.read(audioData, 0, bufferSize);
            recordedAudioDataList.add(audioData);
            recordingCount++;

            Start.setEnabled(true);
            Stop.setEnabled(false);

            if (recordingCount == 5) {
                // You've reached the recording limit, disable the Start button
                Start.setEnabled(false);
            }
            saveAudioToFile(audioData);
            uploadAudio(audioData);
        }
    }

    private void saveAudioToFile(short[] audioData) {
        String uniqueIdentifier = String.valueOf(System.currentTimeMillis());
        String fileName = "recording_" + uniqueIdentifier + ".mp3";
        File audioFile = new File(getExternalFilesDir(null), fileName);

        try {
            DataOutputStream dos = new DataOutputStream(new FileOutputStream(audioFile));
            for (short sample : audioData) {
                dos.writeShort(sample);
            }
            dos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void uploadAudio(short[] audioData) {
        FirebaseStorage storage = FirebaseStorage.getInstance();
        StorageReference storageRef = storage.getReference();
        String uniqueIdentifier = String.valueOf(System.currentTimeMillis());
        String fileName = "recording_" + uniqueIdentifier + ".mp3";

        File audioFile = new File(getExternalFilesDir(null), fileName);
        Uri fileUri = Uri.fromFile(audioFile);

        // Upload the audio file to Firebase Storage
        StorageReference audioRef = storageRef.child("audio/" + fileName);
        UploadTask uploadTask = audioRef.putFile(fileUri);

        uploadTask.addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                // Audio file uploaded successfully
                Toast.makeText(MicrophoneInput.this, "Audio uploaded to Firebase.", Toast.LENGTH_SHORT).show();
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                // Handle the upload failure
                Toast.makeText(MicrophoneInput.this, "Failed to upload audio to Firebase.", Toast.LENGTH_SHORT).show();
            }
        });
    }


    private void updateGraph() {
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (isRecording) {
                    short[] audioData = new short[bufferSize];
                    audioRecord.read(audioData, 0, bufferSize);

                    dataSet.clear();
                    lineChart.notifyDataSetChanged();

                    for (short sample : audioData) {
                        dataSet.addEntry(new Entry(dataSet.getEntryCount(), sample));
                    }
                    lineData.notifyDataChanged();
                    lineChart.notifyDataSetChanged();
                    lineChart.invalidate();

                    updateGraph(); // Recursive call to keep updating the graph
                }
            }
        }, 100); // Update graph every 1000ms
    }

//    private void clearGraphData() {
//        dataSet.clear(); // Clear the data set
//        lineData.notifyDataChanged();
//        lineChart.notifyDataSetChanged();
//        lineChart.invalidate();
//    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        isRecording = false;
        audioRecord.stop();
        audioRecord.release();
    }
}