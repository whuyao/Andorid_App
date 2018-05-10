package hpscil.cug.urbansensor;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.*;
import android.hardware.SensorManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RatingBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;



import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import static hpscil.cug.urbansensor.IpAdressUtils.*;


public class MainActivity extends AppCompatActivity {

    //
    private TextView mTextMessage;
    private Spinner mWeatherSelector;
    private RatingBar mMotionRating;
    private RatingBar mComfortRating;
    private RatingBar mPopultionRating;
    private RatingBar mCarStreamRating;
    private EditText mComment;
    private Button mPushBtn;

    private String[] msWeatherList = {"晴 (0)","阴 (10)","轻微霾 (11)","中度霾 (12)","重度霾 (13)","小雨 (20)","中雨 (21)","大雨 (22)","小雪 (30)","中雪 (31)","大雪 (32)"};
    private int[] mnWeatherState = {0, 10, 11, 12, 13, 20, 21, 22, 30, 31, 32};

    private Boolean mbCanUseGPS = false;
    private Boolean mbCanUseMicroPhone = false;
    private Boolean mbCanUseCamera = false;
    private Boolean mbCanUseWifi = false;
    private Boolean mbCanUseInternet = false;
    private Location mCurLocation = null;

    private String msIpAddr = null;
    private String msMacAddr = null;
    private String msBSSID = null;
    private double mdVolume = 0.0f;
    int mnWeatherCode = 0;
    int mnMotionLevel = 50;
    int mnComfortLevel = 50;
    int mnPopulationLevel = 50;
    int mnCarStreamLevel = 50;
    String msComment= "";

    private AudioRecordDemo mAudio;
    private ConnectDataBase mDB;
    private Date mCurTime;
    private Date mPreviousTime;
    private long mnGapSeconds;

    private double mdTemperature = 0.0f;
    private double mdHuminity = 0.0f;
    private double mdLight = 0.0f;

    private static final int MY_PERMISSION_ACCESS_FINE_LOCATION = 12;
    private static final int MY_PERMISSION_ACCESS_MICROPHONE = 13;
    private static final int MY_PERMISSION_ACCESS_CAMERA = 14;
    private static final int MY_PERMISSION_ACCESS_WIFI = 15;
    private static final int MY_PERMISSION_ACCESS_INTERNET = 16;

    private LocationManager mLocationManager;
    private String mLocationProvider;


    private SensorManager mSensorManager;
    private Sensor mTemperatureSensor;
    private Sensor mHumiditySensor;
    private Sensor mLightSensor;
    private TempListener mTempListener;
    private HumidityListener mHumidityListener;
    private LightListener mLightListener;




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON, WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_main);


        //获取权限
        checkAllAuthority();



        //获取传感器
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        List<Sensor> deviceSensors = mSensorManager.getSensorList(Sensor.TYPE_ALL);
        int nCount = 0;
        for (Sensor sensor : deviceSensors) {
            nCount++;
            Log.i("sensor", "------------------");
            Log.i("No. ", String.format("%d", nCount));
            Log.i("sensor", sensor.getName());
            Log.i("sensor", sensor.getVendor());
            Log.i("sensor", Integer.toString(sensor.getType()));
            Log.i("sensor", "------------------");
        }

        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mTemperatureSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_AMBIENT_TEMPERATURE);  //小米没有这两个传感器
        mHumiditySensor = mSensorManager.getDefaultSensor(Sensor.TYPE_RELATIVE_HUMIDITY);       //小米没有这两个传感器
        mLightSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);

        mTempListener = new TempListener();
        mHumidityListener = new HumidityListener();
        mLightListener = new LightListener();

        mSensorManager.registerListener(mTempListener, mTemperatureSensor, SensorManager.SENSOR_DELAY_NORMAL);
        mSensorManager.registerListener(mHumidityListener, mHumiditySensor, SensorManager.SENSOR_DELAY_NORMAL);
        mSensorManager.registerListener(mLightListener, mLightSensor, SensorManager.SENSOR_DELAY_NORMAL);



        //权限获取后开始执行
        Context context = getApplicationContext();
        mTextMessage = (TextView)findViewById(R.id.message);
        mTextMessage.setMovementMethod(new ScrollingMovementMethod());

        //设置控件
        mWeatherSelector = (Spinner) findViewById(R.id.weatherSpin);
        mWeatherSelector.setDropDownVerticalOffset(100);
        ArrayAdapter<String> arrayAdapter =new ArrayAdapter<String>(this,android.R.layout.simple_list_item_1, msWeatherList);
        mWeatherSelector.setAdapter(arrayAdapter);

        //配置控件
        mMotionRating = (RatingBar) findViewById(R.id.motionBar);
        mComfortRating = (RatingBar) findViewById(R.id.confortBar);
        mPopultionRating = (RatingBar) findViewById(R.id.populationBar);
        mCarStreamRating = (RatingBar) findViewById(R.id.carStreamBar);
        mComment = (EditText) findViewById(R.id.editText);
        mPushBtn = (Button) findViewById(R.id.uploadBtn);


        msIpAddr = getIpAddressPlus(context);
        msMacAddr = getWifiMac(context);
        msBSSID = getBSSID();
        mAudio = new AudioRecordDemo();

        mPreviousTime = new Date();
        mCurTime = new Date();
        mnGapSeconds = 15;      //采集数据间隔秒数

        try{
            mDB = new ConnectDataBase();
        } catch (Exception ex){
            Toast.makeText(this, "network error.", Toast.LENGTH_SHORT).show();
        }

        // 时间监听器: 寻找最优的位置
        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            public void run() {
                try{
                    //获取可用的位置管理器
                    LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
                    List<String> providers = locationManager.getProviders(true);


                    Criteria criteria = new Criteria();
                    criteria.setAccuracy(Criteria.ACCURACY_FINE);
                    String locationProvider = locationManager.getBestProvider(criteria, true);
                    if (locationProvider != null){
                        mLocationManager = locationManager;
                        mLocationProvider = locationProvider;
                    }

                } catch (Exception ex){
                    Log.e("timer: ", ex.toString());
                }
            }
        }, 0,2000);// 设定指定的时间time,此处为2000毫秒

        //execute
        locationObtain();
        mainRun();

        //按钮点击事件
        mPushBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updateAllUserData();
                mainRun();
                recordToDatabase(false);
                //Toast.makeText(getApplicationContext(), "Update location.", Toast.LENGTH_SHORT).show();
            }
        });

        //设置焦点
        mTextMessage.setFocusable(true);
        mTextMessage.setFocusableInTouchMode(true);
        mTextMessage.requestFocus();
        mTextMessage.requestFocusFromTouch();


    }

    protected void locationObtain(){
        checkGPSAuthority();
        try{
            //获取可用的位置管理器
            mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
            List<String> providers = mLocationManager.getProviders(true);


            Criteria criteria = new Criteria();
            criteria.setAccuracy(Criteria.ACCURACY_FINE);
            mLocationProvider = mLocationManager.getBestProvider(criteria, true);
            if (mLocationProvider == null){
                Toast.makeText(this, "没有可用的位置提供器", Toast.LENGTH_SHORT).show();
                return ;
            }


            //获取Location
            mCurLocation = mLocationManager.getLastKnownLocation(mLocationProvider);

            //监视地理位置变化
            mLocationManager.requestLocationUpdates(mLocationProvider, 1000, 0.5f , locationListener);
        } catch (Exception ex){
            //Log.println(ex.getMessage());
            Toast.makeText(this, "location service error.", Toast.LENGTH_SHORT).show();

        }
    }

    void checkAllAuthority (){
        checkGPSAuthority();
        checkInternetAuthority();
        checkMicroPhoneAuthority();
        checkWifiAuthority();
        checkCameraAuthority();
    }


    void checkGPSAuthority(){
        if(!mbCanUseGPS){
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED){
                requestPermissions(new String[] {android.Manifest.permission.ACCESS_FINE_LOCATION}, MY_PERMISSION_ACCESS_FINE_LOCATION);
            }
            else {
                mbCanUseGPS = true;
            }
        }
    }

    void checkWifiAuthority(){
        if(!mbCanUseWifi){
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_WIFI_STATE) != PackageManager.PERMISSION_GRANTED){
                requestPermissions(new String[] {android.Manifest.permission.ACCESS_WIFI_STATE}, MY_PERMISSION_ACCESS_WIFI);
            }
            else {
                mbCanUseWifi = true;
            }
        }
    }

    void checkCameraAuthority () {
        if (!mbCanUseCamera) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED){
                requestPermissions(new String[] {android.Manifest.permission.CAMERA}, MY_PERMISSION_ACCESS_CAMERA);
            }
            else {
                mbCanUseCamera = true;
            }
        }
    }

    void checkMicroPhoneAuthority () {
        if (!mbCanUseMicroPhone) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED){
                requestPermissions(new String[] {android.Manifest.permission.RECORD_AUDIO}, MY_PERMISSION_ACCESS_MICROPHONE);
            }
            else {
                mbCanUseMicroPhone = true;
            }
        }
    }

    void checkInternetAuthority() {
        if (!mbCanUseInternet){
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.INTERNET) != PackageManager.PERMISSION_GRANTED){
                requestPermissions(new String[] {android.Manifest.permission.INTERNET}, MY_PERMISSION_ACCESS_INTERNET);
            }
            else {
                mbCanUseInternet = true;
            }
        }
    }



    void recordToDatabase (Boolean bIsTrack){
        try{
            if (bIsTrack) {
                mCurTime = new Date();
                long gapSeconds = (long)((double)(mCurTime.getTime()-mPreviousTime.getTime()) / 1000.0f + 0.5f);
                if (gapSeconds < mnGapSeconds) return;

                mDB.writeIntoDataBase(msMacAddr, msBSSID, msIpAddr, getCurDateTime(),
                        mCurLocation.getProvider(), mCurLocation.getLatitude(), mCurLocation.getLongitude(),
                        mCurLocation.getAltitude(), mCurLocation.getAccuracy(), mCurLocation.getSpeed(), mCurLocation.getBearing(), mAudio.getNoiseDB(),
                        mnWeatherCode, mnMotionLevel, mnComfortLevel, mnPopulationLevel, mnCarStreamLevel, mdTemperature, mdHuminity, mdLight, "", bIsTrack);
            } else
            {
                mDB.writeIntoDataBase(msMacAddr, msBSSID, msIpAddr, getCurDateTime(),
                        mCurLocation.getProvider(), mCurLocation.getLatitude(), mCurLocation.getLongitude(),
                        mCurLocation.getAltitude(), mCurLocation.getAccuracy(), mCurLocation.getSpeed(), mCurLocation.getBearing(), mAudio.getNoiseDB(),
                        mnWeatherCode, mnMotionLevel, mnComfortLevel, mnPopulationLevel, mnCarStreamLevel, mdTemperature, mdHuminity, mdLight, msComment, bIsTrack);

                Toast.makeText(this, "Push data to database success. Thank you.", Toast.LENGTH_SHORT).show();
            }

        } catch (Exception ex){
            Toast.makeText(this, ex.toString(), Toast.LENGTH_SHORT).show();
        }

        mPreviousTime = new Date();
    }

    void updateAllUserData(){
        Context context = getApplicationContext();
        msIpAddr = getIpAddressPlus(context);
        msMacAddr = getWifiMac(context);
        msBSSID = getBSSID();

        mnMotionLevel = (int)(mMotionRating.getRating() * 20.0f + 0.5f);
        mnComfortLevel = (int)(mComfortRating.getRating() * 20.0f + 0.5f);
        mnPopulationLevel = (int)(mPopultionRating.getRating() * 20.0f + 0.5f);
        mnCarStreamLevel = (int)(mCarStreamRating.getRating() * 20.0f + 0.5f);
        msComment = mComment.getText().toString();

        //
        int nWeatherId = mWeatherSelector.getSelectedItemPosition();
        if (nWeatherId >= 0)
            mnWeatherCode = mnWeatherState[nWeatherId];

        if (mTempListener != null){
            mdTemperature = mTempListener.getValue();
        }

        if (mHumidityListener != null) {
            mdHuminity = mHumidityListener.dHuminity;
        }

        if (mLightListener != null) {
            mdLight = mLightListener.dLight;
        }
    }

    @Override
    protected void onResume() {
        mTemperatureSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_AMBIENT_TEMPERATURE);  //小米没有这两个传感器
        mHumiditySensor = mSensorManager.getDefaultSensor(Sensor.TYPE_RELATIVE_HUMIDITY);       //小米没有这两个传感器
        mLightSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE);
        mSensorManager.registerListener(mTempListener, mTemperatureSensor, SensorManager.SENSOR_DELAY_NORMAL);
        mSensorManager.registerListener(mHumidityListener, mHumiditySensor, SensorManager.SENSOR_DELAY_NORMAL);
        mSensorManager.registerListener(mLightListener, mLightSensor, SensorManager.SENSOR_DELAY_NORMAL);

        super.onResume();

    }

    @Override
    protected void onPause() {
        super.onPause();
        mSensorManager.unregisterListener(mTempListener);
        mSensorManager.unregisterListener(mHumidityListener);
        mSensorManager.unregisterListener(mLightListener);
    }

    void mainRun() {

        checkAllAuthority();

        Context context = getApplicationContext();
        msIpAddr = getIpAddressPlus(context);
        msMacAddr = getWifiMac(context);
        msBSSID = getBSSID();

        locationObtain();
        String s = getCurDateTime() + "\n";
        s += "定位权限：" + mbCanUseGPS + "\n";
        s += "相机权限：" + mbCanUseCamera + "\n";
        s += "录音权限：" + mbCanUseMicroPhone + "\n";
        s += "WIFI权限：" + mbCanUseWifi + "\n";
        s += "INTERNET权限：" + mbCanUseInternet + "\n";

        if (mCurLocation != null)
        {
            s += "GPS provider = " + mCurLocation.getProvider() + "\n";
            s += "Location: (" + mCurLocation.getLongitude() + ", "+mCurLocation.getLatitude() + ", "+ mCurLocation.getAccuracy() + ", "+ mCurLocation.getSpeed()+" meters/seconds)\n" ;
            s += "Bearing: "+ mCurLocation.getBearing() + "\n";
        }

        s += "IP addr: " + msIpAddr + "\n";
        s += "Mac addr: " + msMacAddr + "\n";
        s += "BSSID addr: " + msBSSID + "\n";

        if (mbCanUseMicroPhone){
            s += "Noise: " + mAudio.getNoiseDB() + " dB\n";
            mdVolume = mAudio.getNoiseDB();
        }



        if (mTempListener != null){
            mdTemperature = mTempListener.getValue();
            s += "Temperature: " + mdTemperature + " C\n";
        }

        if (mHumidityListener != null) {
            mdHuminity = mHumidityListener.dHuminity;
            s += "Huminity: " + mdHuminity + " \n";
        }

        if (mLightListener != null) {
            mdLight = mLightListener.dLight;
            s += "Lightness: " + mdLight + " \n";
        }

        mTextMessage.setText(s);

        recordToDatabase(true);
    }


    public String getCurDateTime() {
        Date now = new Date();
        SimpleDateFormat dtFmt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return dtFmt.format(new Date());
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode){
            case MY_PERMISSION_ACCESS_FINE_LOCATION:{
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted
                    mbCanUseGPS = true;
                } else {
                    // permission denied
                    mbCanUseGPS = false;
                }
                break;
            }

            case MY_PERMISSION_ACCESS_MICROPHONE:{
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted
                    mbCanUseMicroPhone = true;
                } else {
                    // permission denied
                    mbCanUseMicroPhone = false;
                }
                break;
            }

            case MY_PERMISSION_ACCESS_CAMERA:{
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted
                    mbCanUseCamera = true;
                } else {
                    // permission denied
                    mbCanUseCamera = false;
                }
                break;
            }

            case MY_PERMISSION_ACCESS_WIFI:{
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted
                    mbCanUseWifi = true;
                } else {
                    // permission denied
                    mbCanUseWifi = false;
                }
                break;
            }

            case MY_PERMISSION_ACCESS_INTERNET :{
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted
                    mbCanUseInternet = true;
                } else {
                    // permission denied
                    mbCanUseInternet = false;
                }
                break;
            }

        }
    }


    /**
     * LocationListern监听器
     * 参数：地理位置提供器、监听位置变化的时间间隔、位置变化的距离间隔、LocationListener监听器
     */
    LocationListener locationListener =  new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {
            //locationObtain();
            mCurLocation = location;
            mainRun();
        }

        @Override
        public void onProviderDisabled(String provider) {

        }

        @Override
        public void onProviderEnabled(String provider) {

        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {

        }
    };



    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mLocationManager != null){
            mLocationManager.removeUpdates(locationListener);
        }

        mSensorManager.unregisterListener(mTempListener);
        mSensorManager.unregisterListener(mHumidityListener);
        mSensorManager.unregisterListener(mLightListener);

    }




}
