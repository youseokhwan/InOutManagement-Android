// 참고: https://hyongdoc.tistory.com/176

package com.example.inoutmanagement;

import com.google.gson.JsonArray;

import java.util.HashMap;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.FieldMap;
import retrofit2.http.Headers;
import retrofit2.http.POST;

public interface RetrofitInterface {

    @Headers("Content-Type: application/json")
    @POST("getcheck/wifi")
    Call<String> postData(@Body String data);

}
