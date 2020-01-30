package com.example.inoutmanagement;

import org.json.JSONObject;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Headers;
import retrofit2.http.POST;

public interface RetrofitInterface {

    @Headers({"Content-Type: application/json", "Accept: application/json"})
    @POST("getcheck/wifi")
    Call<JSONObject> postData(@Body JSONObject data);

}
