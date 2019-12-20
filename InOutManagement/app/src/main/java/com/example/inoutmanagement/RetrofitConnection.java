// 참고: https://hyongdoc.tistory.com/176

package com.example.inoutmanagement;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitConnection {
    // Server URL
    String URL = "https://api.github.com/";

    Retrofit retrofit = new Retrofit.Builder()
            .baseUrl(URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build();

    RetrofitInterface server = retrofit.create(RetrofitInterface.class);
}

