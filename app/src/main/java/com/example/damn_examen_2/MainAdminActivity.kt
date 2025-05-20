package com.example.damn_examen_2 // Reemplaza con tu paquete

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.damn_examen_2.databinding.ActivityMainAdminBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import java.util.Date
import java.text.SimpleDateFormat
import java.util.Locale

class MainAdminActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainAdminBinding
    private lateinit var firebaseAuth: FirebaseAuth
    private var currentUserUid: String? = null
    private lateinit var adminUserDbRef: DatabaseReference
    private lateinit var allUsersDbRef: DatabaseReference
    private lateinit var notificationsDbRef: DatabaseReference


    companion object {
        private const val TAG = "MainAdminActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainAdminBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firebaseAuth = FirebaseAuth.getInstance()
        currentUserUid = firebaseAuth.currentUser?.uid

        if (currentUserUid == null) {
            Toast.makeText(this, "Administrador no autenticado.", Toast.LENGTH_SHORT).show()
            goToLogin()
            return
        }

        adminUserDbRef = FirebaseDatabase.getInstance().getReference("Users").child(currentUserUid!!)
        allUsersDbRef = FirebaseDatabase.getInstance().getReference("Users") // Para listar usuarios
        notificationsDbRef = FirebaseDatabase.getInstance().getReference("Notifications") // Para guardar notificaciones


        loadAdminProfile()
        setupUIListeners()
    }

    private fun loadAdminProfile() {
        adminUserDbRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val adminName = snapshot.child("name").getValue(String::class.java)
                    val adminEmail = snapshot.child("email").getValue(String::class.java)
                    binding.textViewAdminInfo.text = "Admin: ${adminName ?: "N/A"}, Email: ${adminEmail ?: "N/A"}"
                } else {
                    binding.textViewAdminInfo.text = "Admin: Datos no encontrados"
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Error al cargar perfil del admin: ${error.message}", error.toException())
                Toast.makeText(this@MainAdminActivity, "Error al cargar perfil.", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun setupUIListeners() {
        // Dentro de MainAdminActivity.kt, en setupUIListeners()

        binding.buttonListAllUsers.setOnClickListener {
            val intent = Intent(this, UserListActivity::class.java)
            startActivity(intent)
        }

        binding.buttonSendGlobalNotification.setOnClickListener {
            val title = binding.editTextNotificationTitle.text.toString().trim()
            val message = binding.editTextNotificationMessage.text.toString().trim()
            sendGlobalNotification(title, message)
        }

        binding.buttonLogoutAdmin.setOnClickListener {
            firebaseAuth.signOut()
            Toast.makeText(this, "Sesión de administrador cerrada.", Toast.LENGTH_SHORT).show()
            goToLogin()
        }
    }

    private fun sendGlobalNotification(title: String, message: String) {
        if (title.isEmpty() || message.isEmpty()) {
            Toast.makeText(this, "El título y el mensaje no pueden estar vacíos.", Toast.LENGTH_SHORT).show()
            return
        }

        // 1. Guardar la notificación en la base de datos (para historial)
        val notificationId = notificationsDbRef.push().key ?: return // Generar un ID único
        val timestamp = System.currentTimeMillis()
        val formattedTimestamp = SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.getDefault()).format(Date(timestamp))

        val notificationData = hashMapOf(
            "id" to notificationId,
            "title" to title,
            "message" to message,
            "timestamp" to timestamp, // Para ordenar o filtrar
            "formattedTimestamp" to formattedTimestamp, // Para mostrar
            "senderId" to (currentUserUid ?: "admin_system"),
            "target" to "all" // Indica que es para todos
        )

        notificationsDbRef.child(notificationId).setValue(notificationData)
            .addOnSuccessListener {
                Toast.makeText(this, "Notificación guardada y enviada (simulado).", Toast.LENGTH_LONG).show()
                binding.editTextNotificationTitle.text?.clear()
                binding.editTextNotificationMessage.text?.clear()

                // 2. TODO: Implementar el envío real de la notificación PUSH
                // Esto requeriría Firebase Cloud Messaging (FCM).
                // Por ahora, solo la guardamos en la DB.
                // Los clientes (MainUserActivity) necesitarían escuchar cambios en "Notifications"
                // o, idealmente, recibir un mensaje FCM.

            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error al guardar notificación: ${e.message}", Toast.LENGTH_SHORT).show()
                Log.e(TAG, "Error al guardar notificación", e)
            }
    }


    private fun goToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}