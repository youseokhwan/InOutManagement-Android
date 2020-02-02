package com.example.inoutmanagement;

public class WifiInfo {
    private String ssid;
    public String getSSID() { return ssid; }
    public void setSSID(String ssid) { this.ssid = ssid; }

    private String bssid;
    public String getBSSID() { return bssid; }
    public void setBSSID(String bssid) {
        this.bssid = bssid;
    }

//    private int ipAddress;
//    public int getIpAddress() {
//        return ipAddress;
//    }
//    public void setIpAddress(int ipAddress) { this.ipAddress = ipAddress; }
//
//    private String ipv4;
//    public String getIPv4() {
//        return ipv4;
//    }
//    public void setIPv4(String ipv4) {
//        this.ipv4 = ipv4;
//    }

    private int rssi;
    public int getRssi() {
        return rssi;
    }
    public void setRssi(int rssi) {
        this.rssi = rssi;
    }

    public WifiInfo(android.net.wifi.WifiInfo wifiInfo) {
        ssid = wifiInfo.getSSID();
        bssid = wifiInfo.getBSSID();
//        ipAddress = wifiInfo.getIpAddress();
        rssi = wifiInfo.getRssi();
//        ipv4 = String.format("%d.%d.%d.%d", (ipAddress & 0xff), (ipAddress >> 8&0xff), (ipAddress >> 16&0xff), (ipAddress >> 24&0xff));
    }

    @Override
    public String toString() {
        String str = "[현재 연결된 Wi-fi 정보]"
                + "\nSSID: " + ssid
                + "\nBSSID: " + bssid
//                + "\nIpAddress: " + ipv4
                + "\nRssi: " + rssi;

        return str;
    }
}