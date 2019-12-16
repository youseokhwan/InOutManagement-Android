package com.example.inoutmanagement;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.core.app.ActivityCompat;

public class MainActivity extends Activity {

    Button wifiBtn, bluetoothBtn;
    TextView wifiInfo, bluetoothInfo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        wifiBtn = findViewById(R.id.wifiBtn);
        bluetoothBtn = findViewById(R.id.bluetoothBtn);
        wifiInfo = findViewById(R.id.wifiInfo);
        bluetoothInfo = findViewById(R.id.bluetoothInfo);

        wifiBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                wifiInfo.setVisibility(View.VISIBLE);
                bluetoothInfo.setVisibility(View.INVISIBLE);
            }
        });

        bluetoothBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                wifiInfo.setVisibility(View.INVISIBLE);
                bluetoothInfo.setVisibility(View.VISIBLE);
            }
        });

        // Wi-fi
        if(ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)
            ActivityCompat.requestPermissions(this, new String[] { Manifest.permission.ACCESS_FINE_LOCATION }, 1);

        WifiManager wifiManager = (WifiManager)getApplicationContext().getSystemService(Context.WIFI_SERVICE);

        int ip = wifiManager.getConnectionInfo().getIpAddress();
        String ipAddress = String.format("%d.%d.%d.%d", (ip & 0xff), (ip >> 8&0xff), (ip >> 16&0xff), (ip >> 24&0xff));

        String wifiInfoText = "SSID: " + wifiManager.getConnectionInfo().getSSID()
                + "\nBSSID: " + wifiManager.getConnectionInfo().getBSSID()
                + "\nIpAddress: " + ipAddress
                + "\nLinkSpeed: " + wifiManager.getConnectionInfo().getLinkSpeed()
                + "\nNetworkId: " + wifiManager.getConnectionInfo().getNetworkId()
                + "\nRssi: " + wifiManager.getConnectionInfo().getRssi();

        wifiInfo.setText(wifiInfoText);
    }

}
