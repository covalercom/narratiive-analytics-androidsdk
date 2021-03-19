package com.narratiive.narratiivesdk


import android.content.Context
import android.webkit.WebView
import okhttp3.OkHttpClient
import retrofit2.Callback
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.HeaderMap

data class TokenInfo (
    val host: String,
    val hostKey: String,
    val aaid: String?
)

data class HitInfo (
    val token: String,
    val host: String,
    val hostKey: String,
    val path: String?
)

data class ResponseData (
    val token: String?,
    val err: String?
)

interface ApiService {
    @POST("tokens")
    fun createToken(@Body data: TokenInfo, @HeaderMap headers: Map<String, String>): Call<ResponseData>


    @POST("hits")
    fun createHit(@Body data: HitInfo,  @HeaderMap headers: Map<String, String>): Call<ResponseData>
}


object ApiServiceBuilder {
    private val client = OkHttpClient.Builder().build()

    fun<T> buildService(service: Class<T>, context: Context): T {
        val baseUrl = context.getString(R.string.narratiive_api_url)
        val retrofit = Retrofit.Builder()
                .baseUrl(baseUrl)
                .addConverterFactory(GsonConverterFactory.create())
                .client(client)
                .build()

        return retrofit.create(service)
    }
}


class NarratiiveApiService(context: Context) {
    private val retrofit = ApiServiceBuilder.buildService(ApiService::class.java, context)
    private val userAgent =  WebView(context).settings.userAgentString
    private val headers = hashMapOf("Content-Type" to "application/json", "User-Agent" to userAgent, "Accept" to "application/json")

    fun createToken(data: TokenInfo, onResult: (ResponseData?) -> Unit){
        retrofit.createToken(data, headers).enqueue(
            object : Callback<ResponseData> {
                override fun onFailure(call: Call<ResponseData>, t: Throwable) {
                    println("NO...")
                    onResult(null)
                }
                override fun onResponse( call: Call<ResponseData>, response: Response<ResponseData>) {
                    onResult(response?.body())
                }
            }
        )
    }

    fun createHit(data: HitInfo, onResult: (ResponseData?) -> Unit){
        retrofit.createHit(data, headers).enqueue(
            object : Callback<ResponseData> {
                override fun onFailure(call: Call<ResponseData>, t: Throwable) {
                    onResult(null)
                }
                override fun onResponse( call: Call<ResponseData>, response: Response<ResponseData>) {
                    onResult(response?.body())
                }
            }
        )
    }
}