package hpscil.cug.urbansensor;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.util.Log;

import java.math.BigDecimal;

/**
 * Created by astro on 2018/5/10.
 */

public class HumidityListener implements SensorEventListener {
    public double dHuminity = 0.0f;


    @Override
    public void onSensorChanged(SensorEvent event) {
        dHuminity = event.values[0] / 100.0f;
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        Log.i("Sensor", "onAccuracyChanged");
    }
}
