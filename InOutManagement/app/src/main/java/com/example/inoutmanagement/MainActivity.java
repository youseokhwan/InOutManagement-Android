package com.example.inoutmanagement;

import android.Manifest;
import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.text.Html;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;

import com.google.gson.JsonArray;

import java.util.Calendar;
import java.util.List;
import java.util.Set;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * 이탈 관리 및 에너지 정보 관리 App
 * 2019-12-16(월)
 *
 * @author youseokhwan
 */
public class MainActivity extends Activity {

    Button wifiBtn, bluetoothBtn, getBtn;
    TextView info;

    Intent detectWifiIntent;
    ComponentName service;

    WifiInfo currentWifi;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        wifiBtn = findViewById(R.id.wifiBtn);
        bluetoothBtn = findViewById(R.id.bluetoothBtn);
        getBtn = findViewById(R.id.getBtn);
        info = findViewById(R.id.info);
        detectWifiIntent = new Intent(MainActivity.this, DetectWifi.class);

        checkWifiPermission();

        wifiBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getWifiInformation();
                getWifiList();
            }
        });

        bluetoothBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getBluetoothInformation();
            }
        });

        getBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                testGet();
            }
        });
    }

    @Override
    protected void onPause() {
//        service = startService(detectWifiIntent);
        super.onPause();
    }

    @Override
    protected void onRestart() {
//        stopService(new Intent(this, service.getClass()));
        super.onRestart();
    }

    /**
     * ACCESS_FINE_LOCATION 권한을 확인하고 없을 경우 요청
     */
    private void checkWifiPermission() {
        if(ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)
            ActivityCompat.requestPermissions(this, new String[] { Manifest.permission.ACCESS_FINE_LOCATION }, 1);

        // 권한 요청을 거부했을 때 대처해야 함
    }

    /**
     * 기기에 연결된 Wi-fi에 대한 정보 currentWifi 변수에 저장하고 TextView에 디스플레이
     */
    public void getWifiInformation() {
        WifiManager wifiManager = (WifiManager)getApplicationContext().getSystemService(Context.WIFI_SERVICE);

        currentWifi = new WifiInfo(wifiManager.getConnectionInfo());
        info.setText(currentTime() + "\n\n" + currentWifi.toString());
    }

    /**
     * Wi-fi 목록을 TextView에 디스플레이
     */
    public void getWifiList() {
        WifiManager wifiManager = (WifiManager)getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        List<ScanResult> scanResults = wifiManager.getScanResults();

        // 검색된 Wi-fi의 개수 출력
        info.append("\n\n[검색된 Wi-fi 리스트] - " + scanResults.size() + "개\n");

        // 각 Wi-fi의 SSID 출력
        for(ScanResult result : scanResults) {
            String ssid = "\"" + result.SSID + "\"";

            // 현재 연결된 Wi-fi와 같은 BSSID일 경우 보라색으로 강조
            if(result.BSSID.equals(currentWifi.getBSSID())) {
                SpannableStringBuilder builder = new SpannableStringBuilder(ssid);
                builder.setSpan(new ForegroundColorSpan(Color.parseColor("#5F00FF")), 0, ssid.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

                info.append("SSID: ");
                info.append(builder);
                info.append("\n");
            }
            else {
                info.append("SSID: " + ssid + "\n");
            }
        }
    }

    /**
     * 기기에 블루투스로 연결된 장치들에 대한 정보를 TextView에 디스플레이
     */
    private void getBluetoothInformation() {
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        String bluetoothInfo = "";

        if(bluetoothAdapter == null) {
            bluetoothInfo = "블루투스를 지원하지 않는 기기입니다.";
        }
        else if(!bluetoothAdapter.isEnabled()) {
            bluetoothInfo = "블루투스 기능이 꺼져있습니다.";
        }
        else {
            Set<BluetoothDevice> pairDevices = bluetoothAdapter.getBondedDevices();

            if(pairDevices.size() > 0) {
                for(BluetoothDevice device : pairDevices)
                    bluetoothInfo += device.getName().toString() + "\n";
            }
            else {
                bluetoothInfo = "페어링된 블루투스 장치가 없습니다.";
            }
        }

        info.setText(currentTime() + "\n\n" + bluetoothInfo);
    }

    /**
     * 현재 시간 반환
     * @return 현재 시간
     */
    private String currentTime() {
        Calendar calendar = Calendar.getInstance();
        return "요청 시간: " + calendar.getTime().toString();
    }

    /**
     * testGet
     */
    private void testGet() {
        RetrofitConnection retrofitConnection = new RetrofitConnection();
        Call<JsonArray> call = retrofitConnection.server.getData();

        call.enqueue(new Callback<JsonArray>() {
            @Override
            public void onResponse(Call<JsonArray> call, Response<JsonArray> response) {
                if(response.isSuccessful()) {
                    String content = "[Repository 목록]";

                    for(int i = 0; i < response.body().size(); i++) {
                        content += "\n" + (i+1) + ". " + response.body().get(i).getAsJsonObject().get("name").getAsString();
                    }

                    info.setText(currentTime() + "\n\n" + content);
                }
                else {
                    info.setText(currentTime() + "\n\n" + "데이터 전송 오류!");
                }
            }

            @Override
            public void onFailure(Call<JsonArray> call, Throwable t) {
                info.setText(currentTime() + "\n\n" + "서버 연결 오류!!");
            }
        });
    }
}
