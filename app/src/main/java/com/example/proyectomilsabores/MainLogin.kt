package com.example.proyectomilsabores

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.proyectomilsabores.DbLog.DbHelper
import com.example.proyectomilsabores.DbLog.Usuarios
import com.example.proyectomilsabores.ui.activities.MainFeed

class MainLogin : AppCompatActivity() {

    private lateinit var databaseHelper: DbHelper

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_login)

        // Inicializar DatabaseHelper
        databaseHelper = DbHelper(this)

        val btnLogin: Button = findViewById(R.id.btn_login)
        val etxPasswd: EditText = findViewById(R.id.etx_pass)
        val etxUser: EditText = findViewById(R.id.etx_user)
        val etxName: EditText = findViewById(R.id.etx_name) // Necesitarás agregar este EditText en tu layout
        val txConf: TextView = findViewById(R.id.tx_conf)
        val tvToggle: TextView = findViewById(R.id.tv_toggle) // Para cambiar entre login/registro

        var isRegisterMode = false

        // Función para cambiar entre modos
        fun toggleMode() {
            isRegisterMode = !isRegisterMode
            if (isRegisterMode) {
                etxName.visibility = android.view.View.VISIBLE
                btnLogin.text = "Registrarse"
                tvToggle.text = "¿Ya tienes cuenta? Inicia sesión"
                txConf.text = "Completa el formulario de registro"
            } else {
                etxName.visibility = android.view.View.GONE
                btnLogin.text = "Iniciar Sesión"
                tvToggle.text = "¿No tienes cuenta? Regístrate"
                txConf.text = "Ingresa tus credenciales"
            }
            // Limpiar campos
            etxUser.text.clear()
            etxPasswd.text.clear()
            etxName.text.clear()
        }

        // Configurar el toggle
        tvToggle.setOnClickListener {
            toggleMode()
        }

        btnLogin.setOnClickListener {
            val email = etxUser.text.toString().trim()
            val password = etxPasswd.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                txConf.text = "Por favor, completa todos los campos"
                return@setOnClickListener
            }

            if (isRegisterMode) {
                // MODO REGISTRO
                val nombre = etxName.text.toString().trim()

                if (nombre.isEmpty()) {
                    txConf.text = "Por favor, ingresa tu nombre"
                    return@setOnClickListener
                }

                // Verificar si el usuario ya existe
                if (databaseHelper.checkUserExists(email)) {
                    txConf.text = "Este email ya está registrado"
                    return@setOnClickListener
                }

                // Crear nuevo usuario
                val newUser = Usuarios(nombre = nombre, email = email, password = password)
                val result = databaseHelper.addUser(newUser)

                if (result != -1L) {
                    Toast.makeText(this, "Registro exitoso", Toast.LENGTH_SHORT).show()
                    txConf.text = "Registro exitoso. Ahora puedes iniciar sesión"
                    // Cambiar a modo login
                    toggleMode()
                } else {
                    txConf.text = "Error en el registro"
                }

            } else {
                // MODO LOGIN
                val user = databaseHelper.loginUser(email, password)

                if (user != null) {
                    txConf.text = "Bienvenido ${user.nombre}"
                    Toast.makeText(this, "Inicio de sesión exitoso", Toast.LENGTH_SHORT).show()

                    val nvaVentana = Intent(this, MainFeed::class.java)
                    // Puedes pasar datos del usuario si es necesario
                    nvaVentana.putExtra("USER_NAME", user.nombre)
                    nvaVentana.putExtra("USER_EMAIL", user.email)
                    startActivity(nvaVentana)
                    finish() // Opcional: cerrar la actividad de login
                } else {
                    txConf.text = "Usuario o contraseña incorrectos!"
                }
            }
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }
}