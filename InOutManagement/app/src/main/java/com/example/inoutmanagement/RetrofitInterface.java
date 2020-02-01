package com.example.inoutmanagement;

import com.google.gson.JsonObject;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Headers;
import retrofit2.http.POST;

public interface RetrofitInterface {

    @Headers({"Content-Type: application/json", "Accept: application/json"})
    @POST("rest/login")
    Call<JsonObject> login(@Body JsonObject data);

    @Headers({"Content-Type: application/json", "Accept: application/json"})
    @GET("getcheck/wifi")
    Call<JsonObject> getNetwork();

    @Headers({"Content-Type: application/json", "Accept: application/json"})
    @POST("getcheck/wifi")
    Call<JsonObject> changeNetwork(@Body JsonObject data);

    @Headers({"Content-Type: application/json", "Accept: application/json"})
    @POST("getcheck/wifi/reg-home")
    Call<JsonObject> regHomeWifi(@Body JsonObject data);

}
