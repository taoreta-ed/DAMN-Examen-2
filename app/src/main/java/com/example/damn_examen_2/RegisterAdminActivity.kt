package com.example.damn_examen_2

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.util.Patterns
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.damn_examen_2.databinding.ActivityRegisterAdminBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.FirebaseDatabase

class RegisterAdminActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterAdminBinding
    private lateinit var firebaseAuth: FirebaseAuth

    // IMPORTANTE: Define tu contraseña maestra aquí.
    // En una aplicación real, NUNCA la quemes directamente en el código.
    // Deberías obtenerla de una configuración segura o un backend.
    // Para este ejercicio, la definimos aquí por simplicidad.
    private val MASTER_PASSWORD = "Vignette" //

    companion object {
        private const val TAG = "RegisterAdminActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterAdminBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firebaseAuth = FirebaseAuth.getInstance()

        setupPhaseOneListeners() // Configurar listeners para la fase 1
        setupPhaseTwoListeners() // Configurar listeners para la fase 2 (aunque el grupo esté oculto)
    }

    private fun setupPhaseOneListeners() {
        binding.buttonVerifyMasterPassword.setOnClickListener {
            verifyMasterPassword()
        }
    }

    private fun verifyMasterPassword() {
        val enteredPassword = binding.editTextMasterPassword.text.toString()
        if (enteredPassword.isEmpty()) {
            binding.textFieldMasterPassword.error = "La contraseña maestra no puede estar vacía"
            binding.editTextMasterPassword.requestFocus()
            return
        } else {
            binding.textFieldMasterPassword.error = null
        }

        if (enteredPassword == MASTER_PASSWORD) {
            Toast.makeText(this, "Contraseña maestra verificada.", Toast.LENGTH_SHORT).show()
            // Ocultar Fase 1, Mostrar Fase 2
            binding.groupMasterPassword.visibility = View.GONE
            binding.groupAdminRegistration.visibility = View.VISIBLE
        } else {
            Toast.makeText(this, "Contraseña maestra incorrecta.", Toast.LENGTH_LONG).show()
            binding.textFieldMasterPassword.error = "Incorrecta"
            binding.editTextMasterPassword.requestFocus()
        }
    }

    private fun setupPhaseTwoListeners() {
        binding.buttonRegisterAdmin.setOnClickListener {
            performAdminRegistration()
        }
    }

    private fun performAdminRegistration() {
        val adminName = binding.editTextAdminName.text.toString().trim()
        val adminEmail = binding.editTextAdminEmail.text.toString().trim()
        val adminPassword = binding.editTextAdminPassword.text.toString().trim()
        val adminConfirmPassword = binding.editTextAdminConfirmPassword.text.toString().trim()

        // Validaciones
        if (adminName.isEmpty()) {
            binding.textFieldAdminName.error = "El nombre no puede estar vacío"
            binding.editTextAdminName.requestFocus()
            return
        } else {
            binding.textFieldAdminName.error = null
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(adminEmail).matches()) {
            binding.textFieldAdminEmail.error = "Correo electrónico no válido"
            binding.editTextAdminEmail.requestFocus()
            return
        } else {
            binding.textFieldAdminEmail.error = null
        }

        if (adminPassword.length < 6) {
            binding.textFieldAdminPassword.error = "La contraseña debe tener al menos 6 caracteres"
            binding.editTextAdminPassword.requestFocus()
            return
        } else {
            binding.textFieldAdminPassword.error = null
        }

        if (adminPassword != adminConfirmPassword) {
            binding.textFieldAdminConfirmPassword.error = "Las contraseñas no coinciden"
            binding.editTextAdminConfirmPassword.requestFocus()
            return
        } else {
            binding.textFieldAdminConfirmPassword.error = null
        }

        binding.progressBarRegisterAdmin.visibility = View.VISIBLE
        binding.buttonRegisterAdmin.isEnabled = false // Deshabilitar botón durante el proceso

        firebaseAuth.createUserWithEmailAndPassword(adminEmail, adminPassword)
            .addOnCompleteListener(this) { task ->
                binding.progressBarRegisterAdmin.visibility = View.GONE
                binding.buttonRegisterAdmin.isEnabled = true

                if (task.isSuccessful) {
                    Log.d(TAG, "createAdminUserWithEmail:success")
                    val firebaseUser = firebaseAuth.currentUser
                    if (firebaseUser != null) {
                        saveAdminDetailsToDatabase(firebaseUser, adminName, "admin")
                    } else {
                        Toast.makeText(baseContext, "Error al obtener administrador después del registro.", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Log.w(TAG, "createAdminUserWithEmail:failure", task.exception)
                    Toast.makeText(baseContext, "Error de registro de administrador: ${task.exception?.message}",
                        Toast.LENGTH_LONG).show()
                }
            }
    }

    private fun saveAdminDetailsToDatabase(firebaseUser: FirebaseUser, adminName: String, userRole: String) {
        val userId = firebaseUser.uid
        val email = firebaseUser.email ?: ""

        val adminData = HashMap<String, Any>()
        adminData["uid"] = userId
        adminData["name"] = adminName
        adminData["email"] = email
        adminData["role"] = userRole // "admin" para esta activity

        val databaseReference = FirebaseDatabase.getInstance().getReference("Users")

        databaseReference.child(userId).setValue(adminData)
            .addOnSuccessListener {
                Log.d(TAG, "Detalles del administrador guardados en la base de datos exitosamente.")
                Toast.makeText(baseContext, "Administrador registrado exitosamente. Por favor, inicia sesión.", Toast.LENGTH_LONG).show()

                // Navegar a LoginActivity después de guardar los detalles
                val intent = Intent(this, LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error al guardar detalles del administrador: ${e.message}", e)
                Toast.makeText(baseContext, "Registro Auth de admin exitoso, pero falló al guardar detalles: ${e.message}", Toast.LENGTH_LONG).show()
                // Considerar manejo de errores aquí, como borrar el usuario de Auth
            }
    }
}