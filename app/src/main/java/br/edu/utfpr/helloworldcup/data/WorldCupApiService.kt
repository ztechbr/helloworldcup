package br.edu.utfpr.helloworldcup.data

import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET

data class SelectionsResponse(
    val selecoes: List<String>,
    val total: Int
)

data class MatchDto(
    val id: Int,
    val date: String,
    val time: String,
    val group: String,
    val stadium: String,
    val city: String,
    val country: String,
    val home_team: String,
    val away_team: String,
    val score: String?,
    val status: String
)

interface WorldCupApiService {
    @GET("matches")
    suspend fun getMatches(): List<MatchDto>

    @GET("matches/dia/hoje")
    suspend fun getMatchesToday(): List<MatchDto>

    @GET("matches/selecoes")
    suspend fun getSelections(): SelectionsResponse
}

object RetrofitClient {
    private const val BASE_URL = "https://ai-worldcup26.jd0rwz.easypanel.host/"

    val instance: WorldCupApiService by lazy {
        val authInterceptor = Interceptor { chain ->
            val request = chain.request().newBuilder()
                .addHeader("X-Token", "COPA26!")
                .build()
            chain.proceed(request)
        }

        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(logging)
            .build()

        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(WorldCupApiService::class.java)
    }
}
