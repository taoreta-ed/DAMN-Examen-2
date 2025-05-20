package com.example.damn_examen_2 // Reemplaza con tu paquete

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.damn_examen_2.databinding.ActivityMainUserBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class MainUserActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainUserBinding
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var databaseReference: DatabaseReference
    private var currentUserUid: String? = null

    companion object {
        private const val TAG = "MainUserActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainUserBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firebaseAuth = FirebaseAuth.getInstance()
        currentUserUid = firebaseAuth.currentUser?.uid

        if (currentUserUid == null) {
            // Si no hay usuario logueado, no debería estar aquí. Redirigir a Login.
            Toast.makeText(this, "Usuario no autenticado.", Toast.LENGTH_SHORT).show()
            goToLogin()
            return
        }

        // Referencia al nodo del usuario actual en Realtime Database
        databaseReference = FirebaseDatabase.getInstance().getReference("Users").child(currentUserUid!!)

        loadUserProfile()
        setupUIListeners()
    }

    private fun loadUserProfile() {
        databaseReference.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val userName = snapshot.child("name").getValue(String::class.java)
                    val userEmail = snapshot.child("email").getValue(String::class.java)

                    binding.textViewUserWelcome.text = "Bienvenido, ${userName ?: "Usuario"}"
                    binding.textViewUserEmail.text = "Email: ${userEmail ?: "No disponible"}"
                } else {
                    Log.w(TAG, "No se encontraron datos del perfil para el usuario: $currentUserUid")
                    Toast.makeText(this@MainUserActivity, "No se pudo cargar el perfil.", Toast.LENGTH_SHORT).show()
                    // Podría ser un usuario de Auth sin entrada en Realtime DB por algún error previo.
                    // Considerar un mensaje más específico o acción.
                    binding.textViewUserWelcome.text = "Bienvenido, Usuario"
                    binding.textViewUserEmail.text = "Email: ${firebaseAuth.currentUser?.email ?: "No disponible"}"
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Error al cargar el perfil del usuario: ${error.message}", error.toException())
                Toast.makeText(this@MainUserActivity, "Error al cargar perfil: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun setupUIListeners() {
        binding.buttonEditProfileUser.setOnClickListener {
            // TODO: Navegar a ProfileActivity cuando la creemos
            Toast.makeText(this, "Funcionalidad 'Editar Perfil' pendiente.", Toast.LENGTH_SHORT).show()
            // val intent = Intent(this, ProfileActivity::class.java)
            // startActivity(intent)
        }

        binding.buttonViewNotificationHistoryUser.setOnClickListener {
            // TODO: Navegar a NotificationHistoryActivity cuando la creemos
            Toast.makeText(this, "Funcionalidad 'Ver Historial' pendiente.", Toast.LENGTH_SHORT).show()
            // val intent = Intent(this, NotificationHistoryActivity::class.java)
            // startActivity(intent)
        }

        binding.buttonLogoutUser.setOnClickListener {
            firebaseAuth.signOut()
            Toast.makeText(this, "Sesión cerrada.", Toast.LENGTH_SHORT).show()
            goToLogin()
        }
    }

    private fun goToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    // Opcional: Si quieres actualizar la UI si el perfil cambia en la base de datos en tiempo real
    // (por ejemplo, si un admin cambia el nombre del usuario remotamente)
    // Deberías usar addValueEventListener en lugar de addListenerForSingleValueEvent
    // y manejar el removeEventListener en onStop() o onDestroy().
    // Por ahora, con addListenerForSingleValueEvent es suficiente para cargar una vez.
}