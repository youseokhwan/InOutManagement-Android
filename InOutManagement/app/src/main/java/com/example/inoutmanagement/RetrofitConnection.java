// 참고: https://hyongdoc.tistory.com/176

package com.example.inoutmanagement;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitConnection {

    // Server URL(내부)
//    String URL = "https://192.9.45.226:40006/";

    // Server URL(외부)
    String URL = "https://210.102.181.156:40006/";

    Retrofit retrofit = new Retrofit.Builder()
            .baseUrl(URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build();

    RetrofitInterface server = retrofit.create(RetrofitInterface.class);

}

