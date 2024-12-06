package com.example.carteira

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.ListView
import androidx.appcompat.app.AppCompatActivity

class ListarRecursosActivity : AppCompatActivity() {
    private val moedas = listOf("R$", "USD", "EUR", "BTC", "ETH")
    private val sharedPreferences by lazy { getSharedPreferences("app_prefs", MODE_PRIVATE) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_listar_recursos)

        val lvRecursos = findViewById<ListView>(R.id.lvRecursos)

        // Chama a função para listar as moedas com saldo positivo
        val moedasComSaldoPositivo = listarMoedasComSaldoPositivo()

        // Se houver moedas com saldo positivo, exibe-as, caso contrário, exibe uma mensagem
        val recursos = if (moedasComSaldoPositivo.isNotEmpty()) {
            moedasComSaldoPositivo.map { moeda ->
                val chave = if (moeda == "R$") "saldo_real" else "saldo_$moeda"
                val saldo = sharedPreferences.getFloat(chave, 0f)
                "$moeda: %.2f".format(saldo)
            }
        } else {
            listOf("Nenhum saldo disponível.")
        }

        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, recursos)
        lvRecursos.adapter = adapter
    }
    private fun listarMoedasComSaldoPositivo(): List<String> {
        return moedas.filter { moeda ->
            val chave = if (moeda == "R$") "saldo_real" else "saldo_$moeda"
            sharedPreferences.getFloat(chave, 0f) > 0
        }
    }



}
