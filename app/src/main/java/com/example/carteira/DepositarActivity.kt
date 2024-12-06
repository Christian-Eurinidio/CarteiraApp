package com.example.carteira

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class DepositarActivity : AppCompatActivity() {
    private val sharedPreferences by lazy { getSharedPreferences("app_prefs", MODE_PRIVATE) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_depositar)

        val etValorDeposito = findViewById<EditText>(R.id.etValorDeposito)
        val btnConfirmarDeposito = findViewById<Button>(R.id.btnConfirmarDeposito)


        btnConfirmarDeposito.setOnClickListener {
            val valor = etValorDeposito.text.toString().toFloatOrNull()
            if (valor != null && valor > 0) {
                val saldoAtual = sharedPreferences.getFloat("saldo_real", 0f)
                sharedPreferences.edit().putFloat("saldo_real", saldoAtual + valor).apply()
                Toast.makeText(this, "Depósito realizado!", Toast.LENGTH_SHORT).show()
                finish()
            } else {
                Toast.makeText(this, "Digite um valor válido.", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
