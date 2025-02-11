//object RetrofitClientObject {
//}
//package com.ehome.enpartesapp
//
//import okhttp3.OkHttpClient
//import okhttp3.logging.HttpLoggingInterceptor
//import retrofit2.Retrofit
//import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClientObject {
    private const val BASE_URL = "http://local.enpartes.com2/" // Your base URL
    rivate const val BASE_URL = "http://192.168.1.127/" // Your base URL

    val instance: ApiService by lazy {
        //Create a logging interceptor
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY // Set the logging level
        }

        // Create an OkHttpClient and add the logging interceptor
        val client = OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor) //Add the interceptor here
            .build()

        // Create the Retrofit instance
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create()) // Add Gson converter
            .client(client) // Set the OkHttpClient
            .build()

        // Create the ApiService
        retrofit.create(ApiService::class.java)
    }
}