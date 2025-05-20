package com.example.damn_examen_2 //

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.damn_examen_2.databinding.ActivityMainBinding // Asegúrate que esta importación sea correcta
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlin.collections.getValue
import kotlin.io.path.exists

@SuppressLint("CustomSplashScreen")
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var firebaseAuth: FirebaseAuth

    companion object {
        private const val SPLASH_DELAY = 2000L // 2 segundos de retraso
        private const val TAG = "MainActivitySplash"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firebaseAuth = FirebaseAuth.getInstance()

        // Usar un Handler para retrasar la navegación
        Handler(Looper.getMainLooper()).postDelayed({
            checkUserStatus()
        }, SPLASH_DELAY)
    }

    private fun checkUserStatus() {
        val currentUser = firebaseAuth.currentUser
        if (currentUser == null) {
            // Usuario no logueado, ir a LoginActivity
            Log.d(TAG, "Usuario no logueado. Redirigiendo a LoginActivity.")
            navigateToLogin()
        } else {
            // Usuario logueado, verificar su rol y redirigir
            Log.d(TAG, "Usuario logueado: ${currentUser.uid}. Verificando rol...")
            fetchUserRoleAndNavigate(currentUser)
        }
    }

    private fun fetchUserRoleAndNavigate(firebaseUser: FirebaseUser) {
        // Asumimos que guardas la información del usuario, incluido el rol,
        // en Firebase Realtime Database bajo un nodo "Users/{userId}"
        val userId = firebaseUser.uid
        val databaseReference = FirebaseDatabase.getInstance().getReference("Users").child(userId)

        databaseReference.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val userRole = snapshot.child("role").getValue(String::class.java)
                    Log.d(TAG, "Rol del usuario obtenido: $userRole")
                    if (userRole == "admin") {
                        navigateToAdminDashboard()
                    } else {
                        navigateToUserDashboard()
                    }
                } else {
                    // No se encontró información del usuario en la base de datos,
                    // podría ser un estado inconsistente. Por seguridad, desloguear y ir a Login.
                    Log.w(TAG, "No se encontró información para el usuario UID: $userId. Deslogueando.")
                    firebaseAuth.signOut()
                    navigateToLogin()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                // Error al leer de la base de datos.
                // Por seguridad, o para reintentar, podrías ir a Login.
                Log.e(TAG, "Error al obtener rol del usuario: ${error.message}", error.toException())
                Toast.makeText(applicationContext, "Error al verificar usuario. Intenta de nuevo.", Toast.LENGTH_LONG).show()
                firebaseAuth.signOut() // Opcional, pero más seguro
                navigateToLogin()
            }
        })
    }

    private fun navigateToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
        finish() // Finaliza MainActivity para que no se pueda volver con el botón "atrás"
    }

    private fun navigateToUserDashboard() {
        // Reemplaza MainUserActivity::class.java con tu Activity del dashboard de usuario normal
        Log.d(TAG, "Redirigiendo a User Dashboard.")
        val intent = Intent(this, MainUserActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun navigateToAdminDashboard() {
        // Reemplaza MainAdminActivity::class.java con tu Activity del dashboard de administrador
        Log.d(TAG, "Redirigiendo a Admin Dashboard.")
        val intent = Intent(this, MainAdminActivity::class.java)
        startActivity(intent)
        finish()
    }
}