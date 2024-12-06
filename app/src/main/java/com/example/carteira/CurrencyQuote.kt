package com.example.carteira

data class CurrencyQuote(
    val bid: String, // Preço de compra (importante para conversões)
    val ask: String,
    val high: String,
    val low: String,
    val varBid: String,
    val pctChange: String,
    val timestamp: String,
    val create_date: String
)
