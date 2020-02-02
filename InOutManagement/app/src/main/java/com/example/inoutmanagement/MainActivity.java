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
import android.view.inputmethod.InputMethodManager;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TabHost;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.Calendar;
import java.util.List;
import java.util.Set;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * 이탈 관리 및 에너지 정보 관리 App
 * 2019-12-16(월)
 */
public class MainActivity extends Activity {

    // TabView
    TabHost tabHost;

    // LoginView
    EditText idEdt, passwordEdt;
    Button loginBtn;

    // SettingView
    TextView homeWifiList;
    Button regHomeWifiBtn, delHomeWifiBtn, finishBtn;

    // WebView
    WebView webView;
    WebSettings webSettings;

    // 현재 연결된 Wi-Fi 정보 저장, 현재 로그인된 id 정보 저장
    static WifiInfo currentWifi;
    static String currentId = "non-login";

    GpsTracker gpsTracker;
    WifiManager wifiManager;
    SharedPreferences appData;
    ConnectivityManager.NetworkCallback networkCallback;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // TabHost 설정
        setTabHost();

        // WebView 설정
        setWebView();

        // LoginView
        idEdt = findViewById(R.id.idEdt);
        passwordEdt = findViewById(R.id.passwordEdt);
        loginBtn = findViewById(R.id.loginBtn);

        // SettingView
        homeWifiList = findViewById(R.id.homeWifiList);
        regHomeWifiBtn = findViewById(R.id.regHomeWifiBtn);
        delHomeWifiBtn = findViewById(R.id.delHomeWifiBtn);
        finishBtn = findViewById(R.id.finishBtn);

        // 기기에 저장된 로컬 데이터 불러오기
        appData = getSharedPreferences("appData", MODE_PRIVATE);

        // GET getcheck/wifi
//        getBtn.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                getNetworkInfo();
//            }
//        });

        // POST getcheck/wifi
//        postBtn.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                sendWifiStatus();
//            }
//        });

        // 로그인 버튼 클릭 시 서버로 id, password 전송(POST)
        loginBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String id = idEdt.getText().toString().trim();
                String password = passwordEdt.getText().toString().trim();

                // currentId 변수에 저장
                currentId = id;

                // POST 전송
                JsonObject input = new JsonObject();

                input.addProperty("id", id);
                input.addProperty("pwd", password);

                RetrofitConnection retrofitConnection = new RetrofitConnection();
                retrofitConnection.server.login(input).enqueue(new Callback<JsonObject>() {

                    @Override
                    public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                        if(response.isSuccessful()) {
                            if(response.code() == 200) {
                                Toast.makeText(getApplicationContext(), "로그인 성공! Home Wi-Fi를 설정해주세요", Toast.LENGTH_SHORT).show();

                                // 로그인 성공하면 안드로이드 키보드 숨김
                                InputMethodManager inputMethodManager = (InputMethodManager)getSystemService(INPUT_METHOD_SERVICE);
                                inputMethodManager.hideSoftInputFromWindow(passwordEdt.getWindowToken(), 0);

                                // SettingView에 등록된 Home Wi-Fi 목록 표시
                                printHomeWifiList();

                                // SettingView 탭으로 이동
                                tabHost.setCurrentTab(1);
                            } else {
                                Toast.makeText(getApplicationContext(), "로그인 실패", Toast.LENGTH_SHORT).show();
                                currentId = "non-login";
                            }
                        }
                        else {
                            Log.d("loginTest", "onResponse - isSuccessful() false");
                            Log.d("loginTest", "status: " + response.code());
                            currentId = "non-login";
                        }
                    }

                    @Override
                    public void onFailure(Call<JsonObject> call, Throwable t) {
                        Log.d("loginTest", "onFailure");
                        Log.d("loginTest", t.toString());
                    }

                });
            }
        });

        // SettingView의 regHomeWifiBtn - 현재 연결된 Wi-Fi를 Home Wi-Fi로 등록
        regHomeWifiBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addHomeWifi();
                printHomeWifiList();
            }
        });

        // SettingView의 delHomeWifiBtn - Home Wi-Fi 모두 지우기
        delHomeWifiBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clearHomeWifiList();
                printHomeWifiList();
            }
        });

        // SettingView의 finishBtn - Home Wi-Fi 설정 완료하고 WebView로 진입
        finishBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // isFirstRun 변수를 false로 변경하여 다음 실행부터 바로 WebView로 진입하도록 설정
                SharedPreferences.Editor editor = appData.edit();
                editor.putBoolean("isFirstRun", false);
                editor.apply();

                Toast.makeText(getApplicationContext(), "Home Wi-Fi 설정 완료!", Toast.LENGTH_SHORT).show();

                // WebView 진입
                tabHost.setCurrentTab(2);

                // 네트워크 감지 시작
                startNetworkDetection();
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

        // GPS 감지 종료
        gpsTracker.stopGps();
    }

    /**
     * TabHost 설정
     */
    public void setTabHost() {
        // TabHost 설정
        tabHost = findViewById(R.id.tabHost);
        tabHost.setup();

        // Login 탭
        TabHost.TabSpec ts1 = tabHost.newTabSpec("Tab Spec 1");
        ts1.setContent(R.id.loginView);
        ts1.setIndicator("Login");
        tabHost.addTab(ts1);

        // Setting 탭
        TabHost.TabSpec ts2 = tabHost.newTabSpec("Tab Spec 2");
        ts2.setContent(R.id.settingView);
        ts2.setIndicator("Setting");
        tabHost.addTab(ts2);

        // WebView 탭
        TabHost.TabSpec ts3 = tabHost.newTabSpec("Tab Spec 3");
        ts3.setContent(R.id.webView);
        ts3.setIndicator("Web");
        tabHost.addTab(ts3);
    }

    /**
     * WebView 설정
     */
    public void setWebView() {
        // 웹뷰 설정
        webView = findViewById(R.id.webView);

        webView.setWebViewClient(new WebViewClient()); // 클릭 시 새창 안뜨게
        webSettings = webView.getSettings(); //세부 세팅 등록
        webSettings.setJavaScriptEnabled(true); // 웹페이지 자바스클비트 허용 여부
        webSettings.setSupportMultipleWindows(false); // 새창 띄우기 허용 여부
        webSettings.setJavaScriptCanOpenWindowsAutomatically(false); // 자바스크립트 새창 띄우기(멀티뷰) 허용 여부
        webSettings.setLoadWithOverviewMode(true); // 메타태그 허용 여부
        webSettings.setUseWideViewPort(true); // 화면 사이즈 맞추기 허용 여부
        webSettings.setSupportZoom(true); // 화면 줌 허용 여부
        webSettings.setBuiltInZoomControls(true); // 화면 확대 축소 허용 여부
        webSettings.setLayoutAlgorithm(WebSettings.LayoutAlgorithm.SINGLE_COLUMN); // 컨텐츠 사이즈 맞추기
        webSettings.setCacheMode(WebSettings.LOAD_NO_CACHE); // 브라우저 캐시 허용 여부
        webSettings.setDomStorageEnabled(true); // 로컬저장소 허용 여부

        webView.loadUrl("http://xiplug.keico.co.kr/"); // 웹뷰에 표시할 웹사이트 주소, 웹뷰 시작
    }

    /**
     * ACCESS_FINE_LOCATION 권한 확인
     * 안드로이드 Q 이상인 경우 ACCESS_BACKGROUND_LOCATION 권한도 필요함
     */
    public void checkLocationPermission() {
        int foregroundPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION);

        // FINE 권한이 있는 경우 안드로이드 버전에 따라 분기
        if (foregroundPermission == PackageManager.PERMISSION_GRANTED) {
            // 안드로이드 10 이상인 경우 BACKGROUND 권한도 필요
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                int backgroundPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION);

                // BACKGROUND 권한이 있으면 이후 로직 진행
                if (backgroundPermission == PackageManager.PERMISSION_GRANTED) {
                    checkFirstRun();
                }
                // BACKGROUND 권한이 없으면 요청
                else
                    ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_BACKGROUND_LOCATION}, 2);
            }
            // 안드로이드 10 미만인 경우 이후 로직 진행
            else {
                checkFirstRun();
            }
        }
        // FINE 권한이 없는 경우 요청
        else
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
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
        switch (requestCode) {
            case 1:
                // FINE 권한이 허용된 경우 안드로이드 버전에 따라 분기
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // 안드로이드 10 이상인 경우 BACKGROUND 권한도 필요
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_BACKGROUND_LOCATION}, 2);
                        // 안드로이드 10 미만인 경우 이후 로직 진행
                    else {
                        checkFirstRun();
                    }
                }
                // FINE 권한이 거부된 경우 앱 종료
                else {
                    ActivityCompat.finishAffinity(this);
                }
                break;

            case 2:
                // BACKGROUND 권한이 허용된 경우 이후 로직 진행
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    checkFirstRun();
                }
                // BACKGROUND 권한이 거부된 경우 앱 종료
                else {
                    ActivityCompat.finishAffinity(this);
                }
                break;
        }
    }

    /**
     * 권한 설정, 로그인, Home Wi-Fi가 모두 설정돼있으면 다음 실행부터 바로 WebView로 실행되도록 설정
     */
    public void checkFirstRun() {
        boolean isFirstRun = appData.getBoolean("isFirstRun", true);

        // 첫 실행이면 로그인 창으로 이동
        if(isFirstRun) {
            tabHost.setCurrentTab(0);
        }
        // 첫 실행이 아니면 WebView로 이동하고 네트워크 감지 시작
        else {
            tabHost.setCurrentTab(2);
            startNetworkDetection();
        }
    }

    /**
     * 기기에 연결된 Wi-fi에 대한 정보 currentWifi 변수에 저장
     */
    public void getWifiInformation() {
        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        currentWifi = new WifiInfo(wifiManager.getConnectionInfo());
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

    /**
     * 위도, 경도 출력 -> return 로직이 없으므로 사용하기 전에 메소드 수정하시면 됩니다.
     */
    private void getLocation() {
        gpsTracker = new GpsTracker(MainActivity.this);

        double latitude = gpsTracker.getLatitude();
        double longitude = gpsTracker.getLongitude();
    }

    /**
     * GET getcheck/wifi -> 현재 선언만 돼있고 호출하는 부분은 없습니다.
     */
    private void getNetworkInfo() {

        RetrofitConnection retrofitConnection = new RetrofitConnection();
        retrofitConnection.server.getNetwork().enqueue(new Callback<JsonObject>() {

            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                if(response.isSuccessful()) {
                    Log.d("getTest", "onResponse() - isSuccessful() true");
                    Log.d("getTest", "response.body(): " + response.body());
                }
                else {
                    Log.d("getTest", "onResponse() - isSuccessful() false");
                    Log.d("getTest", "response.body(): " + response.body());
                }
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                Log.d("getTest", "onFailure()");
                Log.d("getTest", t.toString());
            }

        });

    }

    /**
     * 외출/귀가 시 서버로 SSID, STATE 전송(POST)
     */
    private void sendWifiStatus() {
        JsonObject userinfo = new JsonObject();
        userinfo.addProperty("id", getCurrentId());

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
     * 현재 로그인 된 User ID 반환
     */
    public static String getCurrentId() {
        return currentId;
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

        getWifiInformation();

        // Wi-Fi에 연결된 상태가 아닐 경우 Toast 띄우기
        if(currentWifi.getSSID().equals("<unknown ssid>")) {
            Toast.makeText(getApplicationContext(), "Wi-Fi에 연결되어있지 않습니다.", Toast.LENGTH_SHORT).show();
            return;
        }

        // BSSID를 비교하여 이미 등록된 Wi-Fi일 경우 Toast 띄우고 중복 등록 방지
        for(int i = 1; i < wifiList.length; i+=2) {
            if(wifiList[i].equals(currentWifi.getBSSID())) {
                Toast.makeText(getApplicationContext(), "이미 등록된 Wi-Fi입니다.", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        data += currentWifi.getSSID() + ",";
        data += currentWifi.getBSSID() + ",";

        SharedPreferences.Editor editor = appData.edit();
        editor.putString("homeWifiList", data);
        editor.apply();

        // POST(getcheck/wifi/reg-home)
        JsonObject userinfo = new JsonObject();
        userinfo.addProperty("id", MainActivity.getCurrentId());

        JsonObject wifiinfo = new JsonObject();
        JsonArray homeWifiList = new JsonArray();

        String[] wifiData = appData.getString("homeWifiList", "").split(",");
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
     * 등록된 Home Wi-Fi 모두 삭제
     */
    public void clearHomeWifiList() {
        SharedPreferences.Editor editor = appData.edit();
        editor.putString("homeWifiList", "");
        editor.apply();

        // POST(getcheck/wifi/reg-home)
        JsonObject userinfo = new JsonObject();
        userinfo.addProperty("id", MainActivity.getCurrentId());

        JsonObject wifiinfo = new JsonObject();
        wifiinfo.addProperty("wifi_home", "");

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
}
