package com.example.waveforms;

import android.media.AudioFormat;
import android.os.Build;

import androidx.annotation.RequiresApi;

@RequiresApi(api = Build.VERSION_CODES.S)
public class Constants {

    final static public int RECORDER_SAMPLERATE = 44100;
    final static public int RECORDER_CHANNELS = AudioFormat.CHANNEL_IN_MONO;
    final static public int RECORDER_AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;

    final static public int BufferElements2Rec = 1024; // want to play 2048 (2K) since 2 bytes we use only 1024
    final static public int BytesPerElement = 2; // 2 bytes in 16bit format


}