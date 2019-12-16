package com.example.inoutmanagement;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

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
    }

}
