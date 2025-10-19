package com.example.proyectomilsabores

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class MainLogin : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_login)

        val btnLogin: Button = findViewById(R.id.btn_login)
        val etx_paswd: EditText = findViewById(R.id.etx_pass)
        val etx_user: EditText = findViewById(R.id.etx_user)
        val tx_conf: TextView = findViewById(R.id.tx_conf)

        var defUser = "admin@duocuc.cl"
        var defPass = "admin"

        btnLogin.setOnClickListener {
        if (etx_user.text.toString() == defUser &&
            etx_paswd.text.toString() == defPass){
            tx_conf.text = "Bienvenido ${etx_user.text}"

            val nvaVentana = Intent(this, MainFeed::class.java)
            startActivity(nvaVentana)
            }
            else{
                tx_conf.text = "Usuario o contraseña incorrectos"
            }
        }


        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }
}