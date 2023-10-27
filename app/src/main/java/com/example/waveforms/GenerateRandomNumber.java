package com.example.waveforms;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.example.waveforms.R;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.github.mikephil.charting.utils.EntryXComparator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

public class GenerateRandomNumber extends AppCompatActivity {

    //declare variables
    private LineChart lineChart;
    private LineDataSet dataSet;
    private LineData lineData;
    private float xValue = 1.0F; //x co-ordinates
    private Random random = new Random();   //generates random number
    private Timer timer = new Timer();  //timer
    private final int interval = 1000; // 1000 milliseconds = 1 second

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_generate_random_number);

        lineChart = findViewById(R.id.lineChart);   //initialise line chart
        lineChart.setDragEnabled(true);
        lineChart.setScaleEnabled(true);

        Button exit = findViewById(R.id.exit);
        exit.setOnClickListener(v -> {
            Intent intent = new Intent(GenerateRandomNumber.this, MainActivity.class);
            startActivity(intent);
            finish();
        });


        // Add desc for line chart
        Description description = new Description();
        description.setText("Dynamic Updating Chart");
        lineChart.setDescription(description);

        // Create an empty LineDataSet
        dataSet = new LineDataSet(
                new ArrayList<>()
                , "Dynamic Data");
        dataSet.setColor(Color.BLUE);
        dataSet.setCircleColor(Color.BLUE);
        dataSet.setLineWidth(2f);
        dataSet.setDrawValues(true);

        lineData = new LineData(dataSet);
        lineChart.setData(lineData);

        // Start generating random data
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                addPointToChart();
            }
        }, interval, interval);

    }

    @Override
    protected void onDestroy() {
        // Stop the random data generation
        timer.cancel();
        super.onDestroy();
    }

    private void addPointToChart() {
        // Add new point to chart
        Entry entry = new Entry(xValue++, generateRandomValue());
        // add new entry to chart
        if (dataSet.getEntryCount() > 10) {
            dataSet.removeFirst();
        }
        dataSet.addEntry(entry);
        //update chart
        dataSet.notifyDataSetChanged();
        lineData.notifyDataChanged();
        lineChart.notifyDataSetChanged();
        // Refresh chart
        runOnUiThread(() -> lineChart.invalidate());


    }

    private float generateRandomValue() {
        return Math.abs(random.nextFloat() * 100);
    }

}
