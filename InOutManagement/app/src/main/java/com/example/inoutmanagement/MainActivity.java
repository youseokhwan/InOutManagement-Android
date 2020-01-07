package com.example.inoutmanagement;

import android.Manifest;
import android.app.Activity;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
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
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

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

    TextView info;
    Button wifiBtn, bluetoothBtn, getBtn, settingBtn;

    // 현재 연결된 Wi-Fi 정보 저장
    static WifiInfo currentWifi;

    WifiManager wifiManager;
    SharedPreferences appData;
    ConnectivityManager.NetworkCallback networkCallback;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        info = findViewById(R.id.info);
//        getBtn = findViewById(R.id.getBtn);
        wifiBtn = findViewById(R.id.wifiBtn);
        settingBtn = findViewById(R.id.settingBtn);
        bluetoothBtn = findViewById(R.id.bluetoothBtn);
        appData = getSharedPreferences("appData", MODE_PRIVATE);

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

//        getBtn.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                testGet();
//            }
//        });

        settingBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), SettingActivity.class);
                startActivity(intent);
            }
        });

        // 위치 권한 확인
        checkLocationPermission();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // 네트워크 감지 중단
        stopNetworkDetection();
    }

    /**
     * ACCESS_FINE_LOCATION 권한 확인
     * 안드로이드 Q 이상인 경우 ACCESS_BACKGROUND_LOCATION 권한도 필요함
     */
    public void checkLocationPermission() {
        int foregroundPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION);

        // FINE 권한이 있는 경우 안드로이드 버전에 따라 분기
        if(foregroundPermission == PackageManager.PERMISSION_GRANTED) {
            // 안드로이드 10 이상인 경우 BACKGROUND 권한도 필요
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                int backgroundPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION);

                // BACKGROUND 권한이 있으면 네트워크 감지 시작하고 최초 실행이면 Home Wi-Fi 설정 액티비티로 이동
                if(backgroundPermission == PackageManager.PERMISSION_GRANTED) {
                    startNetworkDetection();
                    checkFirstRun();
                }
                // BACKGROUND 권한이 없으면 요청
                else
                    ActivityCompat.requestPermissions(this, new String[] { Manifest.permission.ACCESS_BACKGROUND_LOCATION }, 2);
            }
            // 안드로이드 10 미만인 경우 네트워크 감지 시작하고 최초 실행이면 Home Wi-Fi 설정 액티비티로 이동
            else {
                startNetworkDetection();
                checkFirstRun();
            }
        }
        // FINE 권한이 없는 경우 요청
        else
            ActivityCompat.requestPermissions(this, new String[] { Manifest.permission.ACCESS_FINE_LOCATION }, 1);
    }

    /**
     * 위치 권한 요청에 대한 사용자 응답 처리
     * 안드로이드 Q 이상인 경우 ACCESS_BACKGROUND_LOCATION 권한도 필요함
     * @param requestCode
     * @param permissions
     * @param grantResults
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch(requestCode) {
            case 1:
                // FINE 권한이 허용된 경우 안드로이드 버전에 따라 분기
                if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // 안드로이드 10 이상인 경우 BACKGROUND 권한도 필요
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_BACKGROUND_LOCATION}, 2);
                    // 안드로이드 10 미만인 경우 네트워크 감지 시작하고 최초 실행이면 Home Wi-Fi 설정 액티비티로 이동
                    else {
                        startNetworkDetection();
                        checkFirstRun();
                    }
                }
                // FINE 권한이 거부된 경우 TextView 수정
                else
                    info.setText("위치 권한을 허용해주세요.");
                break;

            case 2:
                // BACKGROUND 권한이 허용된 경우 네트워크 감지 시작하고 최초 실행이면 Home Wi-Fi 설정 액티비티로 이동
                if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    startNetworkDetection();
                    checkFirstRun();
                }
                // BACKGROUND 권한이 거부된 경우 TextView 수정
                else
                    info.setText("위치 권한을 '앱 사용 중에만 허용'에서 '항상 허용'으로 바꿔주세요.");
                break;
        }
    }

    /**
     * 최초 실행인지 판단하고 최초 실행이면 Home Wi-Fi 설정 액티비티로 이동
     */
    public void checkFirstRun() {
        // 최초 실행일 경우
        if(appData.getBoolean("isFirstRun", true)) {
            // isFirstRun 변수를 false로 변경
            SharedPreferences.Editor editor = appData.edit();
            editor.putBoolean("isFirstRun", false);
            editor.apply();

            // 최초 실행 시 Toast 띄우기
            Toast.makeText(getApplicationContext(), "최초 실행: Home Wi-Fi를 설정해주세요.", Toast.LENGTH_SHORT).show();

            // Home Wi-Fi를 설정하는 화면으로 유도
            Intent intent = new Intent(getApplicationContext(), SettingActivity.class);
            startActivity(intent);
        }
    }

    /**
     * 기기에 연결된 Wi-fi에 대한 정보 currentWifi 변수에 저장
     */
    public void getWifiInformation() {
        wifiManager = (WifiManager)getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        currentWifi = new WifiInfo(wifiManager.getConnectionInfo());
    }

    /**
     * Wi-fi 목록을 TextView에 디스플레이
     */
    public void getWifiList() {
        wifiManager = (WifiManager)getApplicationContext().getSystemService(Context.WIFI_SERVICE);
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

        info.setText(currentTime() + "\n\n[페어링된 블루투스 기기 목록]\n" + bluetoothInfo);
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
//    private void testGet() {
//        RetrofitConnection retrofitConnection = new RetrofitConnection();
//        Call<JsonArray> call = retrofitConnection.server.getData();
//
//        call.enqueue(new Callback<JsonArray>() {
//            @Override
//            public void onResponse(Call<JsonArray> call, Response<JsonArray> response) {
//                if(response.isSuccessful()) {
//                    String content = "[Retrofit2 RestAPI GET 예제입니다]";
//
//                    for(int i = 0; i < response.body().size(); i++) {
//                        content += "\n" + (i+1) + ". " + response.body().get(i).getAsJsonObject().get("name").getAsString();
//                    }
//
//                    info.setText(currentTime() + "\n\n" + content);
//                }
//                else {
//                    info.setText(currentTime() + "\n\n" + "데이터 전송 오류!");
//                }
//            }
//
//            @Override
//            public void onFailure(Call<JsonArray> call, Throwable t) {
//                info.setText(currentTime() + "\n\n" + "서버 연결 오류!!");
//            }
//        });
//    }

    /**
     * 네트워크 감지 및 Notification 생성
     */
    private void startNetworkDetection() {
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
                            break;

                        // Wi-Fi가 켜져있는 경우
                        case WifiManager.WIFI_STATE_ENABLED: {
                            // 연결중이던 Wi-Fi가 신호 세기가 약해져서 셀룰러 데이터로 연결된 경우
                            if(currentWifi.getRssi() < -80) {
                                createNotification("네트워크 알림", "외출: 셀룰러 데이터로 연결되었습니다.");
                            }
                            // 다른 Wi-Fi가 연결된 경우
                            else {
                                // 연결된 Wi-Fi가 Home Wi-Fi인 경우
                                if(isHomeWifi(currentWifi.getBSSID()))
                                    createNotification("네트워크 알림", "귀가: Wi-fi(" + currentWifi.getSSID() + ")로 연결되었습니다.");
                                // 연결된 Wi-Fi가 Home Wi-Fi가 아닌 경우
                                else
                                    createNotification("네트워크 알림", "외출: Wi-fi(" + currentWifi.getSSID() + ")로 연결되었습니다.");
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
    }

    /**
     * 네트워크 감지 종료
     */
    private void stopNetworkDetection() {
        ConnectivityManager cm = (ConnectivityManager)getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);

        // 마시멜로 버전 이상일 경우
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            cm.unregisterNetworkCallback(networkCallback);
        }
        // 마시멜로 버전 이하일 경우
        else { }
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

        notificationManager.notify(1, builder.build());
    }

    /**
     * SettingActivity에서 currentWifi의 SSID를 얻기 위한 메소드
     */
    public static String getCurrentWifiSSID() {
        return currentWifi.getSSID();
    }

    /**
     * SettingActivity에서 currentWifi의 BSSID를 얻기 위한 메소드
     */
    public static String getCurrentWifiBSSID() {
        return currentWifi.getBSSID();
    }

    /**
     * 연결된 Wi-Fi가 Home Wi-Fi인지 판단
     * @param BSSID 연결된 Wi-Fi의 BSSID 값
     * @return boolean Home Wi-Fi면 true 반환, 아니면 false 반환
     */
    public boolean isHomeWifi(String BSSID) {
        String data = appData.getString("homeWifiList", "");
        String[] wifiList = data.split(";");

        for(int i = 1; i < wifiList.length; i+=2) {
            if(wifiList[i].equals(BSSID)) {
                return true;
            }
        }

        return false;
    }
}
