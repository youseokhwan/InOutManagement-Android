// 참고: https://hyongdoc.tistory.com/176

package com.example.inoutmanagement;

import com.google.gson.JsonArray;

import retrofit2.Call;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;

public interface RetrofitInterface {
    // JsonArray는 나중에 실제 사용될 데이터 클래스로 치환하기

    @GET("users/youseokhwan/repos")
    Call<JsonArray> getData();

    @POST("ccl/")
    void postData(Call<JsonArray> data); // sdfsdfsdf

    @PUT("ccl/")
    void putData(Call<JsonArray> data);

    @DELETE("ccl/")
    void deleteData(Call<JsonArray> data);

}
