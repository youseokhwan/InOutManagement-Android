package com.example.inoutmanagement;

import android.Manifest;
import android.app.Activity;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkRequest;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

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

    WifiInfo currentWifi;
    ConnectivityManager.NetworkCallback networkCallback;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        wifiBtn = findViewById(R.id.wifiBtn);
        bluetoothBtn = findViewById(R.id.bluetoothBtn);
        getBtn = findViewById(R.id.getBtn);
        info = findViewById(R.id.info);

        wifiBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getWifiInformation();
                info.setText(currentTime() + "\n\n" + currentWifi.toString());

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

        // 위치 권한이 있을 경우 네트워크 감지 시작, 없을 경우 권한 요청
        if(hasLocationPermission()) {
            startNetworkDetection();
        }
        else {
            requestLocationPermission();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopNetworkDetection();
    }

    /**
     * 위치 권한 확인
     * @return 권한 여부
     */
    public boolean hasLocationPermission() {
        int permissionCheck = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION);

        // 위치 권한이 있으면 true, 없으면 false를 반환
        if(permissionCheck == PackageManager.PERMISSION_GRANTED)
            return true;
        else
            return false;
    }

    /**
     * 위치 권한 요청
     */
    public void requestLocationPermission() {
        ActivityCompat.requestPermissions(this, new String[] { Manifest.permission.ACCESS_FINE_LOCATION }, 1);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch(requestCode) {
            case 1:
                if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    startNetworkDetection();
                }
                else {
                    info.setText("위치 권한을 허용해주세요.");
                }
        }
    }

    /**
     * 기기에 연결된 Wi-fi에 대한 정보 currentWifi 변수에 저장
     */
    public void getWifiInformation() {
        WifiManager wifiManager = (WifiManager)getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        currentWifi = new WifiInfo(wifiManager.getConnectionInfo());
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
     * Retrofit2 라이브러리를 이용하여 get 호출 예제 적용(GitHub Repository 목록 가져오기)
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

    /**
     * 네트워크 감지 및 Notification 생성
     */
    private void startNetworkDetection() {
        final ConnectivityManager cm = (ConnectivityManager)getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            NetworkRequest.Builder builder = new NetworkRequest.Builder();

            networkCallback = new ConnectivityManager.NetworkCallback() {
                @Override
                public void onAvailable(Network network) {
                    // 디바이스의 Wi-Fi 기능이 어떤 상태인지 확인하기 위한 WifiManager
                    WifiManager wifiManager = (WifiManager)getApplicationContext().getSystemService(Context.WIFI_SERVICE);

                    // 네트워크에 연결됐을 때 Wi-Fi 기능의 On/Off 여부로 네트워크 판단
                    switch(wifiManager.getWifiState()) {
                        case WifiManager.WIFI_STATE_DISABLED:
                        case WifiManager.WIFI_STATE_DISABLING:
                            createNotification("네트워크 알림", "외출: 셀룰러 데이터로 연결되었습니다.");
                            break;
                        case WifiManager.WIFI_STATE_ENABLED:
                            getWifiInformation();
                            createNotification("네트워크 알림", "판단해야함: Wi-fi(" + currentWifi.getSSID() + ")로 연결되었습니다.");
                            break;
                        default:
                            createNotification("네트워크 알림", "오류가 발생하였습니다.");
                    }
                }
            };

            cm.registerNetworkCallback(builder.build(), networkCallback);
        }
        else {
            createNotification("시스템 알림", "지원하지 않는 API 버전입니다.");
        }
    }

    /**
     * 네트워크 감지 종료
     */
    private void stopNetworkDetection() {
        ConnectivityManager cm = (ConnectivityManager)getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            cm.unregisterNetworkCallback(networkCallback);
        }
    }

    /**
     * Notification
     */
    private void createNotification(String title, String text) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext(), "network")
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(title)
                .setContentText(text)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        NotificationManager notificationManager = (NotificationManager)getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationManager.createNotificationChannel(new NotificationChannel("network", "네트워크 알림", NotificationManager.IMPORTANCE_DEFAULT));
        }

        notificationManager.notify(1, builder.build());
    }

}
