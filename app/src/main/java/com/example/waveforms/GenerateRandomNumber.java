package com.example.waveforms;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.example.waveforms.R;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class GenerateRandomNumber extends AppCompatActivity {

    //declare variables
    private LineChart lineChart;
    private List<Entry> entries = new ArrayList<>();
    private int xValue = 0; //x co-ordinates
    private Random random = new Random();   //generates random number
    private Handler handler = new Handler();    //schedules tasks on main UI thread
    private final int interval = 1000; // 1000 milliseconds = 1 second

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_generate_random_number);

        lineChart = findViewById(R.id.lineChart);   //initialise line chart

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
        LineDataSet dataSet = new LineDataSet(entries, "Dynamic Data");
        dataSet.setColor(Color.BLUE);
        dataSet.setCircleColor(Color.BLUE);
        dataSet.setLineWidth(2f);
        dataSet.setDrawValues(false);

        // Start generating random data points
        handler.postDelayed(runnable, interval);

        // Create a LineData object with the LineDataSet and set it on the chart
        LineData lineData = new LineData(dataSet);
        lineChart.setData(lineData);
        lineChart.invalidate(); // Initial chart rendering
    }

    private Runnable runnable = new Runnable() {
        @Override
        public void run() {
            // Generate a random value
            float newValue = generateRandomValue();

            // Add the new data point
            entries.add(new Entry(xValue++, newValue));

            // Notify the chart of the data update
            lineChart.notifyDataSetChanged();
            lineChart.invalidate();

            // Continue generating data points
            handler.postDelayed(this, interval);
        }
    };

    private float generateRandomValue() {
        return Math.abs(random.nextFloat() * 100);
    }

}
