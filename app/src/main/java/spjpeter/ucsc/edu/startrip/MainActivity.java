package spjpeter.ucsc.edu.startrip;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.CountDownTimer;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import static android.util.Half.EPSILON;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    static final private String LOG_TAG = "startrip";
    // Sensors
    private SensorManager mSensorManager;
    private Sensor mSensor;

    // For timing how long the user spins
    private CountDownTimer timer = null;
    private long timeRemaining = 0;

    // Second per millis
    private static final int SECOND = 1000;
    // Length of timer
    private static final int SPINTIME = 5;

    // Things for calculating rotation
    // Create a constant to convert nanoseconds to seconds.
    private static final float NS2S = 1.0f / 1000000000.0f;
    private final float[] deltaRotationVector = new float[4];
    private float timestamp;

    // Elapsed radians rotated
    private float rotatedRads = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
    }

    public void onClickStart(View v) {
        startSpin();
    }

    private void startSpin() {
        // Disable start button

        startGyro();
        startTimer();
    }

    private void startGyro() {
        // Reset spin count
        rotatedRads = 0;
        // Set the gyro listener
        mSensorManager.registerListener(this, mSensor, SensorManager.SENSOR_DELAY_NORMAL);
    }

    private void stopGyro() {
        mSensorManager.unregisterListener(this);
        displayRotations();
    }

    private void startTimer() {
        timeRemaining = SPINTIME;
        displayTime();
        timer = new CountDownTimer(SPINTIME*SECOND, SECOND/2) {
            @Override
            public void onTick(long remaining) {
                timeRemaining = Math.max(0, remaining/SECOND);
                displayTime();
            }

            @Override
            public void onFinish() {
                timeRemaining = 0;
                timer = null;
                displayTime();
                // End the gyroscrope stuff
                stopGyro();
                // Process results
            }
        };
        timer.start();
    }

    private void displayTime() {
        TextView v = (TextView) findViewById(R.id.timeRemainingTextView);
        v.setText(String.format("%ds", timeRemaining));
    }

    private void displayRotations() {
        TextView v = (TextView) findViewById(R.id.recentSpinCountTextBox);
        v.setText(String.format("%.02f rotations", rotatedRads/Math.PI));
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        Log.d(LOG_TAG, "Recieved sensor change event");

        // Throw away non gyroscope data
        if (event.sensor.getType() != Sensor.TYPE_GYROSCOPE) {
            return;
        }

        // Code from https://developer.android.com/guide/topics/sensors/sensors_motion.html#sensors-motion-gyro.
        // Stuff not involved in calculating theta deleted.

        // This timestep's delta rotation to be multiplied by the current rotation
        // after computing it from the gyro sample data.
        if (timestamp != 0) {
            final float dT = (event.timestamp - timestamp) * NS2S;
            // Axis of the rotation sample, not normalized yet.
            float axisX = event.values[0];
            float axisY = event.values[1];
            float axisZ = event.values[2];

            // Calculate the angular speed of the sample
            float omegaMagnitude = (float) Math.sqrt(axisX*axisX + axisY*axisY + axisZ*axisZ);

            // Integrate around this axis with the angular speed by the timestep
            // in order to get a delta rotation from this sample over the timestep
            // We will convert this axis-angle representation of the delta rotation
            // into a quaternion before turning it into the rotation matrix.
            float theta = omegaMagnitude * dT;
            Log.d(LOG_TAG, "The rot delta is "+theta);

            rotatedRads += theta;
        }
        timestamp = event.timestamp;
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }
}
