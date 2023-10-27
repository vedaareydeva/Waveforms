package com.example.waveforms;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.view.View;
import android.os.Bundle;
import android.widget.Button;

import com.google.firebase.FirebaseApp;

public class MainActivity extends AppCompatActivity {

    Button generaterandom;
    Button micinput;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //initialising buttons
        generaterandom = findViewById(R.id.BtnRandom);
        micinput = findViewById(R.id.BtnMic);

        //initialising Firebase
        FirebaseApp.initializeApp(this);

        //set click listener
        generaterandom.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                //Start random number activity
                Intent intent  = new Intent(MainActivity.this, GenerateRandomNumber.class);
                startActivity(intent);

            }
        });

        micinput.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                Intent intent = new Intent(MainActivity.this, MicrophoneInput.class);
                startActivity(intent);

            }
        });
    }
}