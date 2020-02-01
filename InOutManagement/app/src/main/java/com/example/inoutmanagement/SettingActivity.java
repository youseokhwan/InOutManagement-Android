package com.example.inoutmanagement;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SettingActivity extends Activity {

    TextView homeWifiList, currentWifiInfo;
    Button refreshBtn, registerBtn, clearBtn;

    SharedPreferences appData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);

        clearBtn = findViewById(R.id.clearBtn);
        refreshBtn = findViewById(R.id.refreshBtn);
        registerBtn = findViewById(R.id.registerBtn);
        homeWifiList = findViewById(R.id.homeWifiList);
        currentWifiInfo = findViewById(R.id.currentWifiInfo);
        appData = getSharedPreferences("appData", MODE_PRIVATE);

        refreshBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updateCurrentWifiInfo();
            }
        });

        registerBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addHomeWifi();
                printHomeWifiList();
            }
        });

        clearBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clearHomeWifiList();
                printHomeWifiList();
            }
        });

        // 현재 등록된 Home Wi-Fi 목록을 표시
        printHomeWifiList();

        // 현재 연결된 Wi-Fi의 정보를 TextView에 업데이트
        updateCurrentWifiInfo();
    }

    /**
     * 현재 등록된 Home Wi-Fi 목록을 표시
     */
    public void printHomeWifiList() {
        String data = appData.getString("homeWifiList", "");
        String[] wifiList = data.split(",");

        // 등록된 Wi-Fi가 없을 경우
        if(data.equals(""))
            homeWifiList.setText("등록된 Home Wi-Fi가 없습니다.\n");
        // 등록된 Wi-Fi가 1개 이상일 경우
        else {
            homeWifiList.setText("");
            for(int i = 0; i < wifiList.length; i++) {
                homeWifiList.append((i+1)/2+1 + ". ");
                homeWifiList.append(wifiList[i++] + " ");
                homeWifiList.append("(" + wifiList[i] + ")\n");
            }
        }
    }

    /**
     * 현재 연결된 Wi-Fi를 Home Wi-Fi로 추가 + POST
     */
    public void addHomeWifi() {
        String data = appData.getString("homeWifiList", "");
        String[] wifiList = data.split(",");

        // Wi-Fi에 연결된 상태가 아닐 경우 Toast 띄우기
        if(MainActivity.getCurrentWifiSSID().equals("<unknown ssid>")) {
            Toast.makeText(getApplicationContext(), "Wi-Fi에 연결되어있지 않습니다.", Toast.LENGTH_SHORT).show();
            return;
        }

        // BSSID를 비교하여 이미 등록된 Wi-Fi일 경우 Toast 띄우고 중복 등록 방지
        for(int i = 1; i < wifiList.length; i+=2) {
            if(wifiList[i].equals(MainActivity.getCurrentWifiBSSID())) {
                Toast.makeText(getApplicationContext(), "이미 등록된 Wi-Fi입니다.", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        data += MainActivity.getCurrentWifiSSID() + ",";
        data += MainActivity.getCurrentWifiBSSID() + ",";

        SharedPreferences.Editor editor = appData.edit();
        editor.putString("homeWifiList", data);
        editor.apply();

        // POST(getcheck/wifi/reg-home)
        JsonObject userinfo = new JsonObject();
        userinfo.addProperty("id", MainActivity.getCurrentId());

        JsonObject wifiinfo = new JsonObject();
        JsonArray homeWifiList = new JsonArray();

        String[] wifiData = appData.getString("homeWifiList", "no home-wifi").split(",");
        for(int i = 0; i < wifiData.length; i+=2) {
            homeWifiList.add(wifiData[i].substring(1, wifiData[i].length()-1).trim());
        }
        wifiinfo.add("wifi_home", homeWifiList);

        JsonObject input = new JsonObject();
        input.add("userinfo", userinfo);
        input.add("wifiinfo", wifiinfo);

        RetrofitConnection retrofitConnection = new RetrofitConnection();
        retrofitConnection.server.regHomeWifi(input).enqueue(new Callback<JsonObject>() {

            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                if(response.isSuccessful()) {
                    Toast.makeText(getApplicationContext(), "등록 성공", Toast.LENGTH_SHORT).show();
                }
                else {
                    Toast.makeText(getApplicationContext(), "오류: " + response.code(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                Toast.makeText(getApplicationContext(), t.toString(), Toast.LENGTH_SHORT).show();
            }

        });
    }

    /**
     * 등록된 Home Wi-Fi 모두 삭제 + POST
     */
    public void clearHomeWifiList() {
        SharedPreferences.Editor editor = appData.edit();
        editor.putString("homeWifiList", "");
        editor.apply();

        // POST(getcheck/wifi/reg-home)
        JsonObject userinfo = new JsonObject();
        userinfo.addProperty("id", MainActivity.getCurrentId());

        JsonObject wifiinfo = new JsonObject();
        wifiinfo.addProperty("wifi_home", "[]");

        JsonObject input = new JsonObject();
        input.add("userinfo", userinfo);
        input.add("wifiinfo", wifiinfo);

        RetrofitConnection retrofitConnection = new RetrofitConnection();
        retrofitConnection.server.regHomeWifi(input).enqueue(new Callback<JsonObject>() {

            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                if(response.isSuccessful()) {
                    Toast.makeText(getApplicationContext(), "등록 성공", Toast.LENGTH_SHORT).show();
                }
                else {
                    Toast.makeText(getApplicationContext(), "오류: " + response.code(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                Toast.makeText(getApplicationContext(), t.toString(), Toast.LENGTH_SHORT).show();
            }

        });
    }

    /**
     * currentWifiInfo TextView에 현재 연결된 Wi-Fi 정보를 표시
     */
    public void updateCurrentWifiInfo() {
        currentWifiInfo.setText("SSID: " + MainActivity.getCurrentWifiSSID());
        currentWifiInfo.append("\nBSSID: " + MainActivity.getCurrentWifiBSSID());
    }

}
