package com.example.waveforms;

import java.io.File;

// Model class for audio files
public class AudioModel {
    // This class is used to store the audio files in a list
    private String fileName;
    private File audioSource;
    private String extension;
    private String duration;
    private String date;

    public AudioModel(String fileName, File audioSource, String extension, String duration, String date) {
        this.fileName = fileName;
        this.audioSource = audioSource;
        this.extension = extension;
        this.duration = duration;
        this.date = date;
    }

    public String getFileName() {
        return fileName;
    }

    public File getAudioSource() {
        return audioSource;
    }

    public String getExtension() {
        return extension;
    }

    public String getDuration() {
        return duration;
    }

    public String getDate() {
        return date;
    }
}

