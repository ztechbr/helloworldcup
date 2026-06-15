package br.edu.utfpr.helloworldcup.data

import br.edu.utfpr.helloworldcup.presentation.Country
import br.edu.utfpr.helloworldcup.presentation.Match
import retrofit2.http.GET

interface CountryApiService {
    @GET("countries")
    suspend fun getCountries(): List<Country>

    @GET("matches")
    suspend fun getMatches(): List<Match>
}
