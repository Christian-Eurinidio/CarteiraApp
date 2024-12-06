package com.example.carteira

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.IOException
import org.json.JSONException



class ConverterRecursosActivity : AppCompatActivity() {
    private val moedas = listOf("R$", "USD", "EUR", "BTC", "ETH")
    private val sharedPreferences by lazy { getSharedPreferences("app_prefs", MODE_PRIVATE) }

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_converter_recursos)

        val spinnerOrigem = findViewById<Spinner>(R.id.spinnerOrigem)
        val spinnerDestino = findViewById<Spinner>(R.id.spinnerDestino)
        val etValorConverter = findViewById<EditText>(R.id.etValorConverter)
        val btnConverter = findViewById<Button>(R.id.btnConverter)
        val progressBar = findViewById<ProgressBar>(R.id.progressBar)
        val tvResultado = findViewById<TextView>(R.id.tvResultado)

        // Filtra moedas com saldo > 0
        val moedasComSaldo = moedas.filter { moeda ->
            val chave = if (moeda == "R$") "saldo_real" else "saldo_$moeda"
            sharedPreferences.getFloat(chave, 0f) > 0
        }
        spinnerOrigem.setSelection(moedasComSaldo.indexOf("R$"))

        val adapterOrigem = ArrayAdapter(this, android.R.layout.simple_spinner_item, moedasComSaldo)
        adapterOrigem.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerOrigem.adapter = adapterOrigem

        val adapterDestino = ArrayAdapter(this, android.R.layout.simple_spinner_item, moedas)
        adapterDestino.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerDestino.adapter = adapterDestino

        val tvSaldo = findViewById<TextView>(R.id.tvSaldoOrigem)
        spinnerOrigem.setOnItemSelectedListener(object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                val moedaOrigem = parent.getItemAtPosition(position).toString()
                val chaveSaldo = if (moedaOrigem == "R$") "saldo_real" else "saldo_$moedaOrigem"
                val saldoOrigem = sharedPreferences.getFloat(chaveSaldo, 0f)

                tvSaldo.text = "Saldo: %.2f $moedaOrigem".format(saldoOrigem)
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                tvSaldo.text = "Saldo: 0.00"
            }
        })


        btnConverter.setOnClickListener {
            val moedaOrigem = spinnerOrigem.selectedItem?.toString() ?: return@setOnClickListener
            val moedaDestino = spinnerDestino.selectedItem?.toString() ?: return@setOnClickListener
            val valor = etValorConverter.text.toString().toFloatOrNull()

            if (moedaOrigem == moedaDestino) {
                Toast.makeText(this, "Selecione moedas diferentes.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (valor == null || valor <= 0) {
                Toast.makeText(this, "Digite um valor válido.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Verificar saldo da moeda de origem
            val chaveSaldo = if (moedaOrigem == "R$") "saldo_real" else "saldo_$moedaOrigem"
            val saldoOrigem = sharedPreferences.getFloat(chaveSaldo, 0f)

            if (saldoOrigem < valor) {
                Toast.makeText(this, "Saldo insuficiente na moeda de origem.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Inicia conversão
            progressBar.visibility = View.VISIBLE
            tvResultado.text = "Processando..."

            realizarConversao(moedaOrigem, moedaDestino, valor) { sucesso, valorConvertido ->
                runOnUiThread {
                    progressBar.visibility = View.GONE
                    if (sucesso) {
                        atualizarSaldos(moedaOrigem, moedaDestino, valor, valorConvertido)
                        tvResultado.text =
                            "Conversão realizada! $valor $moedaOrigem = $valorConvertido $moedaDestino"
                    } else {
                        tvResultado.text = "Erro ao realizar conversão."
                        Toast.makeText(this, "Erro ao realizar conversão.", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }


    }

    private fun realizarConversao(
        moedaOrigem: String,
        moedaDestino: String,
        valor: Float,
        callback: (Boolean, Float) -> Unit
    ) {
        val mapaMoedas = mapOf(
            "R$" to "BRL",
            "USD" to "USD",
            "EUR" to "EUR",
            "BTC" to "BTC",
            "ETH" to "ETH"
        )
        val codigoOrigem = mapaMoedas[moedaOrigem] ?: return
        val codigoDestino = mapaMoedas[moedaDestino] ?: return

        // Ajuste na URL com BTC ou ETH
        val url = if (codigoDestino == "BTC" || codigoDestino == "ETH") {
            "https://economia.awesomeapi.com.br/last/${codigoDestino}-${codigoOrigem}"
        } else {
            "https://economia.awesomeapi.com.br/last/${codigoOrigem}-${codigoDestino}"
        }
        Log.d("ConverterRecursosActivity", "URL gerada: $url")

        val client = OkHttpClient()
        val request = Request.Builder().url(url).build()

        client.newCall(request).enqueue(object : okhttp3.Callback {
            override fun onFailure(call: okhttp3.Call, e: IOException) {
                Log.e("ConverterRecursosActivity", "Falha na requisição: ${e.message}")
                callback(false, 0f)
            }

            override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                try {
                    response.use {
                        val responseBody = response.body?.string() ?: throw IOException("Corpo da resposta vazio")
                        Log.d("ConverterRecursosActivity", "Resposta da API: $responseBody")

                        val json = JSONObject(responseBody)

                        // Busca chave baseada na ordem BTC ou ETH
                        val key = if (codigoDestino == "BTC" || codigoDestino == "ETH") {
                            "${codigoDestino}${codigoOrigem}"
                        } else {
                            "${codigoOrigem}${codigoDestino}"
                        }

                        if (json.has(key)) {
                            val cotacao = json.getJSONObject(key).getString("bid").toFloatOrNull()
                            if (cotacao != null) {
                                val valorConvertido = if (moedaOrigem == "R$" && (moedaDestino == "BTC" || moedaDestino == "ETH")) {
                                    valor / cotacao // Inverte a cotação para calcular corretamente de BRL para BTC ou ETH
                                } else {
                                    valor * cotacao // Caso contrário, é uma conversão direta
                                }
                                callback(true, valorConvertido)
                            } else {
                                Log.e("ConverterRecursosActivity", "Cotação inválida para $key")
                                callback(false, 0f)
                            }
                        } else {
                            Log.e("ConverterRecursosActivity", "Chave $key não encontrada no JSON")
                            callback(false, 0f)
                        }
                    }
                } catch (e: JSONException) {
                    Log.e("ConverterRecursosActivity", "Erro ao processar JSON: ${e.message}")
                    callback(false, 0f)
                } catch (e: Exception) {
                    Log.e("ConverterRecursosActivity", "Erro inesperado: ${e.message}")
                    callback(false, 0f)
                }
            }
        })
    }

    private fun atualizarSaldos(
        moedaOrigem: String,
        moedaDestino: String,
        valorOrigem: Float,
        valorDestino: Float
    ) {
        val chaveOrigem = if (moedaOrigem == "R$") "saldo_real" else "saldo_$moedaOrigem"
        val chaveDestino = if (moedaDestino == "R$") "saldo_real" else "saldo_$moedaDestino"

        val saldoOrigem = sharedPreferences.getFloat(chaveOrigem, 0f)
        val saldoDestino = sharedPreferences.getFloat(chaveDestino, 0f)

        sharedPreferences.edit()
            .putFloat(chaveOrigem, saldoOrigem - valorOrigem)
            .putFloat(chaveDestino, saldoDestino + valorDestino)
            .apply()
    }


}
