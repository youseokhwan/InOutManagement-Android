package com.example.inoutmanagement;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkRequest;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import static com.example.inoutmanagement.MainActivity.currentWifi;

public class NetworkService extends Service {

    WifiManager wifiManager;
    SharedPreferences appData;
    ConnectivityManager.NetworkCallback networkCallback;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        appData = getSharedPreferences("appData", MODE_PRIVATE);

        createServiceNotificationChannel();
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this,
                0, notificationIntent, 0);
        Notification notification = new NotificationCompat.Builder(this, "NetworkServiceChannel")
                .setContentTitle("Network Service")
                .setContentIntent(pendingIntent)
                .build();
        startForeground(1, notification);

        //do heavy work on a background thread
        final ConnectivityManager cm = (ConnectivityManager)getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);

        // 마시멜로 버전 이상일 경우
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            NetworkRequest.Builder builder = new NetworkRequest.Builder();

            networkCallback = new ConnectivityManager.NetworkCallback() {
                @Override
                public void onAvailable(Network network) {
                    // 디바이스의 Wi-Fi 연결 상태 가져오기
                    getWifiInformation();

                    // 네트워크에 연결됐을 때 Wi-Fi 기능의 On/Off 상태 여부로 네트워크 판단
                    switch(wifiManager.getWifiState()) {
                        // Wi-Fi가 꺼져있거나 꺼지는 중이지만 네트워크가 연결된 경우 셀룰러 데이터로 연결된 경우라고 가정
                        case WifiManager.WIFI_STATE_DISABLED:
                        case WifiManager.WIFI_STATE_DISABLING:
                            createNotification("네트워크 알림", "외출: 셀룰러 데이터로 연결되었습니다.");
                            sendWifiStatus();
                            break;

                        // Wi-Fi가 켜져있는 경우
                        case WifiManager.WIFI_STATE_ENABLED: {
                            // 연결중이던 Wi-Fi가 신호 세기가 약해져서 셀룰러 데이터로 연결된 경우
                            if(currentWifi.getRssi() < -80) {
                                createNotification("네트워크 알림", "외출: 셀룰러 데이터로 연결되었습니다.");
                                sendWifiStatus();
                            }
                            // Wi-Fi 세기가 충분한 경우
                            else {
                                // 연결된 Wi-Fi가 Home Wi-Fi인 경우
                                if(isHomeWifi(currentWifi.getBSSID())) {
                                    createNotification("네트워크 알림", "귀가: Wi-fi(" + currentWifi.getSSID() + ")로 연결되었습니다.");
                                    sendWifiStatus();
                                }
                                // 연결된 Wi-Fi가 Home Wi-Fi가 아닌 경우
                                else {
                                    createNotification("네트워크 알림", "외출: Wi-fi(" + currentWifi.getSSID() + ")로 연결되었습니다.");
                                    sendWifiStatus();
                                }
                            }

                            break;
                        }

                        default:
                            createNotification("네트워크 알림", "오류가 발생하였습니다.");
                    }
                }
            };

            cm.registerNetworkCallback(builder.build(), networkCallback);
        }
        // 마시멜로 버전 이하일 경우
        else {
            createNotification("시스템 알림", "지원하지 않는 API 버전입니다.");
        }
        //stopSelf();

        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        ConnectivityManager cm = (ConnectivityManager)getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);

        // 마시멜로 버전 이상일 경우
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            cm.unregisterNetworkCallback(networkCallback);
        }
        // 마시멜로 버전 이하일 경우
        else { }
    }

    private void createServiceNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    "NetworkServiceChannel",
                    "Network Service Channel",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(serviceChannel);
        }
    }

    /**
     * Notification 생성
     */
    private void createNotification(String title, String text) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext(), "network")
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(title)
                .setContentText(text)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true);

        NotificationManager notificationManager = (NotificationManager)getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
        // 오레오 버전 이상일 경우
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationManager.createNotificationChannel(new NotificationChannel("network", "네트워크 알림", NotificationManager.IMPORTANCE_HIGH));
        }
        // 오레오 버전 이하일 경우 Notification Channel 사용하지 않는 방식으로 구현해야 함
        else { }

        notificationManager.notify(2, builder.build());
    }

    /**
     * 기기에 연결된 Wi-fi에 대한 정보 currentWifi 변수에 저장
     */
    public void getWifiInformation() {
        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        currentWifi = new WifiInfo(wifiManager.getConnectionInfo());
    }

    /**
     * 외출/귀가 시 서버로 SSID, STATE 전송(POST)
     */
    public void sendWifiStatus() {
        JsonObject userinfo = new JsonObject();
        userinfo.addProperty("id", MainActivity.getCurrentId());

        JsonObject wifiinfo = new JsonObject();

        // wifi_home
        JsonArray homeWifiList = new JsonArray();
        String[] data = appData.getString("homeWifiList", "").split(",");
        for(int i = 0; i < data.length; i+=2) {
            homeWifiList.add(data[i].substring(1, data[i].length()-1).trim());
        }
        wifiinfo.add("wifi_home", homeWifiList);

        // wifi_now
        wifiinfo.addProperty("wifi_now", currentWifi.getSSID().substring(1, currentWifi.getSSID().length()-1).trim());

        // wifi_stat
        if(wifiManager.isWifiEnabled())
            wifiinfo.addProperty("wifi_stat", "on");
        else
            wifiinfo.addProperty("wifi_stat", "off");

        // wifi_list
        JsonArray scanWifiList = new JsonArray();
        data = getWifiList().split(",");
        for (String str : data) {
            scanWifiList.add(str.trim());
        }
        wifiinfo.add("wifi_list", scanWifiList);

        JsonObject input = new JsonObject();
        input.add("userinfo", userinfo);
        input.add("wifiinfo", wifiinfo);

        RetrofitConnection retrofitConnection = new RetrofitConnection();
        retrofitConnection.server.changeNetwork(input).enqueue(new Callback<JsonObject>() {

            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                if(response.isSuccessful()) {
                    Log.d("postTest", "onResponse() - isSuccessful() true");
                    Log.d("postTest", "response.body(): " + response.body());
                }
                else {
                    Log.d("postTest", "onResponse() - isSuccessful() false");
                    Log.d("postTest", "response.body(): " + response.body());
                }
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                Log.d("postTest", "onFailure()");
                Log.d("postTest", t.toString());
            }

        });
    }

    /**
     * 연결된 Wi-Fi가 Home Wi-Fi인지 판단
     * @param BSSID 연결된 Wi-Fi의 BSSID 값
     * @return boolean Home Wi-Fi면 true 반환, 아니면 false 반환
     */
    public boolean isHomeWifi(String BSSID) {
        String data = appData.getString("homeWifiList", "");
        String[] wifiList = data.split(",");

        for(int i = 1; i < wifiList.length; i+=2) {
            if(wifiList[i].equals(BSSID)) {
                return true;
            }
        }

        return false;
    }

    /**
     * 주변 Wi-Fi 목록을 String으로 반환
     */
    public String getWifiList() {
        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        List<ScanResult> scanResults = wifiManager.getScanResults();

        // 스캔된 Wi-Fi가 없을 경우
        if(scanResults.size() == 0) {
            return "";
        }
        // 스캔된 Wi-Fi가 있을 경우
        else {
            String list = "";
            for(ScanResult result : scanResults) {
                if(!result.SSID.equals("")) {
                    list += result.SSID + ", ";
                }
            }
            return list.substring(0, list.length()-2);
        }
    }
}
