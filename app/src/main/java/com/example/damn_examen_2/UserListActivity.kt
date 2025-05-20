package com.example.damn_examen_2 // Reemplaza con tu paquete

import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.damn_examen_2.databinding.ActivityUserListBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class UserListActivity : AppCompatActivity() {

    private lateinit var binding: ActivityUserListBinding
    private lateinit var databaseReference: DatabaseReference
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var userAdapter: UserAdapter
    private val userList = mutableListOf<AdminViewUser>()
    private var currentAdminUid: String? = null

    companion object {
        private const val TAG = "UserListActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUserListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firebaseAuth = FirebaseAuth.getInstance()
        currentAdminUid = firebaseAuth.currentUser?.uid
        databaseReference = FirebaseDatabase.getInstance().getReference("Users")

        setupToolbar()
        setupRecyclerView()
        loadUsers()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbarUserList)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Manejar el botón de retroceso de la toolbar
        if (item.itemId == android.R.id.home) {
            finish() // Cierra esta actividad y vuelve a la anterior (MainAdminActivity)
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun setupRecyclerView() {
        userAdapter = UserAdapter(userList, currentAdminUid ?: "") { user, action ->
            when (action) {
                UserAdapter.UserAction.CHANGE_ROLE -> handleChangeRole(user)
                UserAdapter.UserAction.DELETE_USER -> handleDeleteUser(user)
            }
        }
        binding.recyclerViewUsers.layoutManager = LinearLayoutManager(this)
        binding.recyclerViewUsers.adapter = userAdapter
    }

    private fun loadUsers() {
        binding.progressBarUserList.visibility = View.VISIBLE
        binding.textViewNoUsers.visibility = View.GONE
        binding.recyclerViewUsers.visibility = View.GONE

        databaseReference.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                userList.clear()
                if (snapshot.exists()) {
                    for (userSnapshot in snapshot.children) {
                        val user = userSnapshot.getValue(AdminViewUser::class.java)
                        user?.let { userList.add(it) }
                    }
                }

                if (userList.isEmpty()) {
                    binding.textViewNoUsers.visibility = View.VISIBLE
                    binding.recyclerViewUsers.visibility = View.GONE
                } else {
                    binding.textViewNoUsers.visibility = View.GONE
                    binding.recyclerViewUsers.visibility = View.VISIBLE
                    userAdapter.updateUsers(userList)
                }
                binding.progressBarUserList.visibility = View.GONE
            }

            override fun onCancelled(error: DatabaseError) {
                binding.progressBarUserList.visibility = View.GONE
                binding.textViewNoUsers.visibility = View.VISIBLE
                binding.textViewNoUsers.text = "Error al cargar usuarios: ${error.message}"
                Log.e(TAG, "Error al cargar usuarios: ${error.message}", error.toException())
                Toast.makeText(this@UserListActivity, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun handleChangeRole(user: AdminViewUser) {
        val newRole = if (user.role == "user") "admin" else "user"
        val actionMessage = if (newRole == "admin") "promover a Administrador" else "degradar a Usuario"

        AlertDialog.Builder(this)
            .setTitle("Confirmar Cambio de Rol")
            .setMessage("¿Estás seguro de que quieres $actionMessage a ${user.name} (${user.email})?")
            .setPositiveButton("Sí, Cambiar Rol") { dialog, _ ->
                updateUserRoleInDatabase(user.uid, newRole)
                dialog.dismiss()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun updateUserRoleInDatabase(userId: String, newRole: String) {
        databaseReference.child(userId).child("role").setValue(newRole)
            .addOnSuccessListener {
                Toast.makeText(this, "Rol actualizado a $newRole.", Toast.LENGTH_SHORT).show()
                // La lista se actualizará automáticamente gracias al ValueEventListener
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error al actualizar rol: ${e.message}", Toast.LENGTH_SHORT).show()
                Log.e(TAG, "Error al actualizar rol para UID: $userId", e)
            }
    }

    private fun handleDeleteUser(user: AdminViewUser) {
        AlertDialog.Builder(this)
            .setTitle("Confirmar Eliminación")
            .setMessage("¿Estás seguro de que quieres eliminar a ${user.name} (${user.email})? Esta acción no se puede deshacer y también eliminará su cuenta de autenticación.")
            .setPositiveButton("Sí, Eliminar") { dialog, _ ->
                // Paso 1: Eliminar de Realtime Database
                databaseReference.child(user.uid).removeValue()
                    .addOnSuccessListener {
                        // Paso 2: Eliminar de Firebase Authentication
                        // ¡IMPORTANTE! Esta es una operación sensible.
                        // Idealmente, esto se haría desde un backend (Cloud Functions)
                        // para asegurar que el admin tiene permisos para hacerlo.
                        // Eliminar directamente desde el cliente puede ser un riesgo si no se maneja con cuidado.
                        // Firebase Auth no proporciona una API directa para que un usuario elimine a otro
                        // sin privilegios de administrador del proyecto (que no se deben tener en el cliente).
                        // Por ahora, solo eliminaremos de la base de datos y mostraremos un mensaje.
                        // Para una eliminación completa, necesitarías el Firebase Admin SDK en un backend.

                        Toast.makeText(this, "Usuario eliminado de la base de datos. La cuenta de Auth debe eliminarse manualmente o vía backend.", Toast.LENGTH_LONG).show()
                        Log.d(TAG, "Usuario ${user.uid} eliminado de Realtime Database.")
                        // La lista se actualizará automáticamente.
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Error al eliminar usuario de la base de datos: ${e.message}", Toast.LENGTH_SHORT).show()
                        Log.e(TAG, "Error al eliminar de DB UID: ${user.uid}", e)
                    }
                dialog.dismiss()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    override fun onDestroy() {
        super.onDestroy()
        // Es buena práctica remover los listeners de Firebase cuando la Activity ya no es necesaria,
        // especialmente si usas addValueEventListener para evitar memory leaks.
        // Sin embargo, si la actividad se destruye y recrea (ej. por rotación), este listener se
        // volverá a añadir en onCreate. Si este es el único listener global a "Users",
        // puede ser aceptable no removerlo aquí para que siga actualizando en segundo plano,
        // pero para listeners más específicos o de vida corta, siempre remuévelos.
        // databaseReference.removeEventListener(/* el ValueEventListener específico si lo guardaste */);
    }
}