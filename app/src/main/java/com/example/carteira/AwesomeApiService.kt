package com.example.carteira

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Path

interface AwesomeApiService {

    // Endpoint para cotações de moedas específicas
    @GET("last/{currencyPair}")
    fun getCurrencyQuote(@Path("currencyPair") currencyPair: String): Call<Map<String, CurrencyQuote>>
}
