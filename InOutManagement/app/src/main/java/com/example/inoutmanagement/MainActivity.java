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
import android.content.pm.PackageManager;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;

import java.util.Calendar;
import java.util.Set;

/**
 * 이탈 관리 및 에너지 정보 관리 App
 * 2019-12-16(월)
 *
 * @author youseokhwan
 */
public class MainActivity extends Activity {

    Button wifiBtn, bluetoothBtn;
    TextView info;

    Intent detectWifiIntent;
    ComponentName service;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        wifiBtn = findViewById(R.id.wifiBtn);
        bluetoothBtn = findViewById(R.id.bluetoothBtn);
        info = findViewById(R.id.info);
        detectWifiIntent = new Intent(MainActivity.this, DetectWifi.class);

        checkWifiPermission();

        wifiBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getWifiInformation();
            }
        });

        bluetoothBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getBluetoothInformation();
            }
        });
    }

    @Override
    protected void onPause() {
        service = startService(detectWifiIntent);
        super.onPause();
    }

    @Override
    protected void onRestart() {
        stopService(new Intent(this, service.getClass()));
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
     * 기기에 연결된 Wi-fi에 대한 정보를 TextView에 디스플레이
     */
    public void getWifiInformation() {
        WifiManager wifiManager = (WifiManager)getApplicationContext().getSystemService(Context.WIFI_SERVICE);

        // int형 ip값을 IPv4 형태로 변경
        int ip = wifiManager.getConnectionInfo().getIpAddress();
        String ipAddress = String.format("%d.%d.%d.%d", (ip & 0xff), (ip >> 8&0xff), (ip >> 16&0xff), (ip >> 24&0xff));

        String wifiInfo = "SSID: " + wifiManager.getConnectionInfo().getSSID()
                + "\nBSSID: " + wifiManager.getConnectionInfo().getBSSID()
                + "\nIpAddress: " + ipAddress
                + "\nLinkSpeed: " + wifiManager.getConnectionInfo().getLinkSpeed()
                + "\nNetworkId: " + wifiManager.getConnectionInfo().getNetworkId()
                + "\nRssi: " + wifiManager.getConnectionInfo().getRssi();

        info.setText(currentTime() + "\n\n" + wifiInfo);
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
}
