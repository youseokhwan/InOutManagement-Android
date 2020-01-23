// 참고: https://hyongdoc.tistory.com/176

package com.example.inoutmanagement;

import com.google.gson.JsonArray;

import java.util.HashMap;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.FieldMap;
import retrofit2.http.POST;

public interface RetrofitInterface {
    // JsonArray는 나중에 실제 사용될 데이터 클래스로 바꾸기

//    @GET("users/youseokhwan/repos")
//    Call<JsonArray> getData();

    @POST("getcheck/wifi")
    Call<HashMap<String, String>> postData(@Body HashMap<String, String> data);

}
