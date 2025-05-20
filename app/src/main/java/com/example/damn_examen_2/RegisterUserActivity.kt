package com.example.damn_examen_2 // Reemplaza con tu paquete

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.util.Patterns
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.damn_examen_2.databinding.ActivityRegisterUserBinding // Asegúrate que esta importación sea correcta
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.FirebaseDatabase

class RegisterUserActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterUserBinding
    private lateinit var firebaseAuth: FirebaseAuth

    companion object {
        private const val TAG = "RegisterUserActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterUserBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firebaseAuth = FirebaseAuth.getInstance()

        binding.buttonRegisterUser.setOnClickListener {
            performUserRegistration()
        }

        binding.textViewGoToLoginFromRegister.setOnClickListener {
            // Ir a LoginActivity
            val intent = Intent(this, LoginActivity::class.java)
            // Considera limpiar el stack si vienes de un flujo donde no quieres volver a registro
            // intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish() // Opcional, dependiendo si quieres que el usuario pueda volver a esta pantalla desde login
        }
    }

    private fun performUserRegistration() {
        val name = binding.editTextNameRegister.text.toString().trim()
        val email = binding.editTextEmailRegister.text.toString().trim()
        val password = binding.editTextPasswordRegister.text.toString().trim()
        val confirmPassword = binding.editTextConfirmPasswordRegister.text.toString().trim()

        // Validaciones
        if (name.isEmpty()) {
            binding.textFieldNameRegister.error = "El nombre no puede estar vacío"
            binding.editTextNameRegister.requestFocus()
            return
        } else {
            binding.textFieldNameRegister.error = null
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.textFieldEmailRegister.error = "Correo electrónico no válido"
            binding.editTextEmailRegister.requestFocus()
            return
        } else {
            binding.textFieldEmailRegister.error = null
        }

        if (password.length < 6) { // Firebase requiere al menos 6 caracteres
            binding.textFieldPasswordRegister.error = "La contraseña debe tener al menos 6 caracteres"
            binding.editTextPasswordRegister.requestFocus()
            return
        } else {
            binding.textFieldPasswordRegister.error = null
        }

        if (password != confirmPassword) {
            binding.textFieldConfirmPasswordRegister.error = "Las contraseñas no coinciden"
            binding.editTextConfirmPasswordRegister.requestFocus()
            return
        } else {
            binding.textFieldConfirmPasswordRegister.error = null
        }

        // Mostrar ProgressBar y deshabilitar botón
        binding.progressBarRegisterUser.visibility = View.VISIBLE
        binding.buttonRegisterUser.isEnabled = false
        binding.textViewGoToLoginFromRegister.isEnabled = false

        firebaseAuth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                binding.progressBarRegisterUser.visibility = View.GONE
                binding.buttonRegisterUser.isEnabled = true
                binding.textViewGoToLoginFromRegister.isEnabled = true

                if (task.isSuccessful) {
                    Log.d(TAG, "createUserWithEmail:success")
                    val firebaseUser = firebaseAuth.currentUser
                    if (firebaseUser != null) {
                        saveUserDetailsToDatabase(firebaseUser, name, "user") // Guardar detalles adicionales
                    } else {
                        // Esto no debería pasar si task.isSuccessful es true, pero por si acaso
                        Toast.makeText(baseContext, "Error al obtener usuario después del registro.", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Log.w(TAG, "createUserWithEmail:failure", task.exception)
                    Toast.makeText(baseContext, "Error de registro: ${task.exception?.message}",
                        Toast.LENGTH_LONG).show()
                }
            }
    }

    private fun saveUserDetailsToDatabase(firebaseUser: FirebaseUser, userName: String, userRole: String) {
        val userId = firebaseUser.uid
        val email = firebaseUser.email ?: ""

        val userData = HashMap<String, Any>()
        userData["uid"] = userId
        userData["name"] = userName
        userData["email"] = email
        userData["role"] = userRole // "user" para esta activity

        val databaseReference = FirebaseDatabase.getInstance().getReference("Users")

        databaseReference.child(userId).setValue(userData)
            .addOnSuccessListener {
                Log.d(TAG, "Detalles del usuario guardados en la base de datos exitosamente.")
                Toast.makeText(baseContext, "Registro exitoso. Por favor, inicia sesión.", Toast.LENGTH_LONG).show()
                // Navegar a LoginActivity después de guardar los detalles
                val intent = Intent(this, LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK // Limpiar stack para no volver
                startActivity(intent)
                finish()
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error al guardar detalles del usuario: ${e.message}", e)
                // Aunque el Auth fue exitoso, el guardado en DB falló.
                // Es importante manejar este caso. Podrías intentar borrar el usuario de Auth
                // o pedirle que intente loguearse y completar el perfil luego.
                // Por ahora, solo mostramos un error y lo dejamos en la pantalla de registro.
                Toast.makeText(baseContext, "Registro de Auth exitoso, pero falló al guardar detalles: ${e.message}", Toast.LENGTH_LONG).show()
                // Podrías ofrecer reintentar guardar o desloguear al usuario
                // firebaseAuth.currentUser?.delete() // Considerar esto si quieres revertir el registro de Auth
            }
    }
}