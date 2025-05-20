package com.example.damn_examen_2

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.damn_examen_2.databinding.ActivityLoginBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser

class LoginActivity : AppCompatActivity() {

    // Declarar ViewBinding
    private lateinit var binding: ActivityLoginBinding
    private lateinit var firebaseAuth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Inflar el layout usando ViewBinding
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firebaseAuth = FirebaseAuth.getInstance()

        // Listener para el botón de inicio de sesión
        binding.buttonLogin.setOnClickListener {
            performLogin()
        }

        // Listener para ir a la pantalla de registro de usuario normal
        binding.textViewGoToRegister.setOnClickListener {
            // Reemplaza RegisterUserActivity::class.java con tu Activity de registro
            val intent = Intent(this, RegisterUserActivity::class.java)
            startActivity(intent)
        }

        // Listener para ir a la pantalla de registro de administrador
        binding.textViewGoToAdminRegister.setOnClickListener {
            // Reemplaza RegisterAdminActivity::class.java con tu Activity/Dialog de registro de admin
            val intent = Intent(this, RegisterAdminActivity::class.java)
            startActivity(intent)
        }
    }

    private fun performLogin() {
        val email = binding.editTextEmail.text.toString().trim()
        val password = binding.editTextPassword.text.toString().trim()

        if (!isValidEmail(email)) {
            binding.textFieldEmail.error = "Correo electrónico no válido"
            return
        } else {
            binding.textFieldEmail.error = null // Limpiar error
        }

        if (password.isEmpty()) {
            binding.textFieldPassword.error = "La contraseña no puede estar vacía"
            return
        } else {
            binding.textFieldPassword.error = null // Limpiar error
        }

        // Mostrar ProgressBar
        binding.progressBarLogin.visibility = View.VISIBLE
        binding.buttonLogin.isEnabled = false // Deshabilitar botón durante la carga

        firebaseAuth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                binding.progressBarLogin.visibility = View.GONE
                binding.buttonLogin.isEnabled = true

                if (task.isSuccessful) {
                    // Inicio de sesión exitoso, actualizar UI con la información del usuario
                    val user = firebaseAuth.currentUser
                    Toast.makeText(baseContext, "Inicio de sesión exitoso.", Toast.LENGTH_SHORT).show()
                    updateUI(user)
                } else {
                    // Si el inicio de sesión falla, mostrar un mensaje al usuario.
                    Toast.makeText(baseContext, "Error de autenticación: ${task.exception?.message}",
                        Toast.LENGTH_LONG).show()
                    updateUI(null)
                }
            }
    }

    private fun isValidEmail(email: String): Boolean {
        return Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    // Esta función se llamará cuando el usuario ya esté logueado al iniciar la app
    // o después de un inicio de sesión/registro exitoso.
    override fun onStart() {
        super.onStart()
        // Comprobar si el usuario ya está logueado (sesión persistente)
        val currentUser = firebaseAuth.currentUser
        if (currentUser != null) {
            // Aquí deberías obtener el rol del usuario desde Firebase Realtime Database/Firestore
            // y redirigir a la pantalla correspondiente (User o Admin).
            // Por ahora, solo lo mandamos a una MainActivity genérica.
            Toast.makeText(this, "Usuario ya logueado: ${currentUser.email}", Toast.LENGTH_SHORT).show()
            updateUI(currentUser)
        }
    }

    private fun updateUI(user: FirebaseUser?) {
        if (user != null) {
            // Aquí es donde, una vez que sabes el rol, decidirías a qué Activity ir.
            // Por ejemplo:
            // if (userEsAdmin) {
            //    startActivity(Intent(this, MainAdminActivity::class.java))
            // } else {
            //    startActivity(Intent(this, MainUserActivity::class.java))
            // }
            // Por ahora, solo como ejemplo, vamos a una MainActivity (deberás crearla)
            val intent = Intent(this, MainActivity::class.java) // Reemplaza MainActivity con tu destino
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK // Para no volver aquí con el botón atrás
            startActivity(intent)
            finish() // Finaliza LoginActivity
        } else {
            // El usuario no está logueado, permanece en la pantalla de login o muéstrala.
        }
    }
}