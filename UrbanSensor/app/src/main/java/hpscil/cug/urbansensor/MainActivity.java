package hpscil.cug.urbansensor;

import android.Manifest;
import android.accounts.NetworkErrorException;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;

import static hpscil.cug.urbansensor.IpAdressUtils.*;
import hpscil.cug.urbansensor.AudioRecordDemo;


public class MainActivity extends AppCompatActivity {

    private TextView mTextMessage;
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
    private AudioRecordDemo mAudio;
    private ConnectDataBase mDB;
    private Date mCurTime;
    private Date mPreviousTime;
    private long mnGapSeconds;

    private static final int MY_PERMISSION_ACCESS_FINE_LOCATION = 12;
    private static final int MY_PERMISSION_ACCESS_MICROPHONE = 13;
    private static final int MY_PERMISSION_ACCESS_CAMERA = 14;
    private static final int MY_PERMISSION_ACCESS_WIFI = 15;
    private static final int MY_PERMISSION_ACCESS_INTERNET = 16;

    private LocationManager mLocationManager;
    private String mLocationProvider;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON, WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_main);


        //获取权限
        checkInternetAuthority();
        checkGPSAuthority();
        checkCameraAuthority();
        checkMicroPhoneAuthority();
        checkWifiAuthority();



        //权限获取后开始执行
        Context context = getApplicationContext();
        mTextMessage = (TextView)findViewById(R.id.message);
        msIpAddr = getIpAddressPlus(context);
        msMacAddr = getWifiMac(context);
        msBSSID = getBSSID();
        mAudio = new AudioRecordDemo();

        mPreviousTime = new Date();
        mCurTime = new Date();
        mnGapSeconds = 30;      //采集数据间隔秒数




        try{
            mDB = new ConnectDataBase();
        } catch (Exception ex){
            Toast.makeText(this, "network error.", Toast.LENGTH_SHORT).show();
        }


        //execute
        locationObtain();
        showInfo();
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
            mCurTime = new Date();
            long gapSeconds = (long)((double)(mCurTime.getTime()-mPreviousTime.getTime()) / 1000.0f + 0.5f);
            if (gapSeconds < mnGapSeconds) return;

            /*
            if (!mDB.getIsConnected()){
                //Toast.makeText(this, "Database connection error.", Toast.LENGTH_SHORT).show();
                return;
            }*/

            if (bIsTrack) {
                mDB.writeIntoDataBase(msMacAddr, msBSSID, msIpAddr, getCurDateTime(),
                        mCurLocation.getProvider(), mCurLocation.getLatitude(), mCurLocation.getLongitude(),
                        mCurLocation.getAltitude(), mCurLocation.getAccuracy(), mCurLocation.getSpeed(), mCurLocation.getBearing(), mAudio.getNoiseDB(),
                        0, 50, 50, 50, 50, "", true);
            } else
            {
                mDB.writeIntoDataBase(msMacAddr, msBSSID, msIpAddr, getCurDateTime(),
                        mCurLocation.getProvider(), mCurLocation.getLatitude(), mCurLocation.getLongitude(),
                        mCurLocation.getAltitude(), mCurLocation.getAccuracy(), mCurLocation.getSpeed(), mCurLocation.getBearing(), mAudio.getNoiseDB(),
                        0, 50, 50, 50, 50, "", true);
            }

        } catch (Exception ex){
            Toast.makeText(this, "Network error. Upload data error.", Toast.LENGTH_SHORT).show();
        }

        mPreviousTime = new Date();
    }



    void showInfo() {
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

        if (mbCanUseMicroPhone)
           s += "Noise: " + mAudio.getNoiseDB() + " dB";


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
            locationObtain();
            mCurLocation = location;
            showInfo();
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
    }




}
