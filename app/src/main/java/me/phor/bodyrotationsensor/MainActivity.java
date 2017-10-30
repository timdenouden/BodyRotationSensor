package me.phor.bodyrotationsensor;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements SensorEventListener{
    public static final String TAG = "MainActivity";
    private static final String FILENAME = "bodyRotationSensorOutput.csv";

    TextView statusText;
    Button startButton, shareButton;
    private SensorManager sensorManager;
    private Sensor sensor;

    boolean running = true;
    float[] rotationMatrix = new float[16];
    float[] orientationValues = new float[4];
    ArrayList<float[]> data = new ArrayList<>();
    float[] zeroValues = new float[3];
    long previousTime = System.nanoTime();

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        statusText = (TextView) findViewById(R.id.statusText);
        startButton = (Button) findViewById(R.id.startButton);
        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchState();
            }
        });
        shareButton = (Button) findViewById(R.id.shareButton);
        shareButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                shareFile();
            }
        });

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        sensor = sensorManager.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR);
        sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_GAME);

        switchState();
    }

    private void shareFile() {
        File file = new File(Environment.getExternalStorageDirectory(), FILENAME);

        Intent intentShareFile = new Intent(Intent.ACTION_SEND);

        if(file.exists()) {
            intentShareFile.setType("text/csv");

            intentShareFile.putExtra(Intent.EXTRA_STREAM, Uri.parse("file://" + file.getAbsolutePath()));

            intentShareFile.putExtra(Intent.EXTRA_SUBJECT,
                    "bodyRotationSensorOutput.csv");
            intentShareFile.putExtra(Intent.EXTRA_TEXT, "output");

            startActivity(Intent.createChooser(intentShareFile, "Share File"));
        }
    }

    private boolean saveFile() {
        File file = new File(Environment.getExternalStorageDirectory(), FILENAME);

        String output = "Yaw(Z),Pitch(X),Roll(Y),DeltaTime(ns)\n"; //do a thing
        for(int i = 0; i < data.size(); i++) {
            for(int j = 0; j < data.get(i).length; j++) {
                output = output.concat(String.valueOf(data.get(i)[j]));
                if(j < data.get(i).length - 1) {
                    output = output.concat(",");
                }
            }
            if(i < data.size() - 1) {
                output = output.concat("\n");
            }
        }
        try {
            FileOutputStream fos = new FileOutputStream(file);
            fos.write(output.getBytes());
            fos.close();
            Toast.makeText(this, "Saving file at: " + file.getPath(), Toast.LENGTH_LONG).show();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    private void switchState() {
        running = !running;
        if(running) {
            startButton.setBackgroundColor(Color.RED);
            startButton.setText("Stop");
            shareButton.setEnabled(false);
            statusText.setText("Started");
            data.clear();
            zeroMeausurement();
        }
        else {
            startButton.setBackgroundColor(Color.GREEN);
            startButton.setText("Start");
            statusText.setText("Stopped \nDataPointsCollected: " + data.size());
            if(saveFile()) {
                shareButton.setEnabled(true);
            }
            else {
                statusText.setText("File didn't save properly\nTry enabling permissions\nor Try inserting SD card");
            }
        }
    }

    private void determineOrientation(float[] rotationMatrix)
    {
        SensorManager.getOrientation(rotationMatrix, orientationValues);
        double azimuth = toPositiveDegrees(Math.toDegrees(orientationValues[0] - zeroValues[0]));
        double pitch = toPositiveDegrees(Math.toDegrees(orientationValues[1] - zeroValues[1]));
        double roll = toPositiveDegrees(Math.toDegrees(orientationValues[2] - zeroValues[2]));
        orientationValues[3] = (float)calcuateDelta();
        if(running) {
            float[] step = new float[orientationValues.length];
            step[0] = (float) azimuth;
            step[1] = (float) pitch;
            step[2] = (float) roll;
            step[3] = orientationValues[3];
            data.add(step);
            statusText.setText("yaw Z: " + String.valueOf(Math.round(azimuth)) +
                    "\npitch X: " + String.valueOf(Math.round(pitch)) +
                    "\nroll Y: " + String.valueOf(Math.round(roll)) +
                    "\ndelta ms: " + String.valueOf(Math.round(orientationValues[3] / 1000000)));
        }
    }

    private double calcuateDelta() {
        long currentTime = System.nanoTime();
        double deltaTime = previousTime - currentTime;
        previousTime = currentTime;
        return -deltaTime;
    }

    private void zeroMeausurement() {
        zeroValues = orientationValues.clone();
    }

    private double toPositiveDegrees(double degrees) {
        if(degrees < 0) {
            return 360.0 + degrees;
        }
        return degrees;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if(event.sensor.getType() == Sensor.TYPE_GAME_ROTATION_VECTOR) {
                for(int i = 0; i < rotationMatrix.length; i++) {
                    rotationMatrix[i] = 0f;
                }
                sensorManager.getRotationMatrixFromVector(rotationMatrix, event.values);
                determineOrientation(rotationMatrix);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }
}
