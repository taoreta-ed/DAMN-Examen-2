package com.example.damn_examen_2 // Reemplaza con tu paquete

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.damn_examen_2.databinding.ItemUserAdminViewBinding // Importa tu ViewBinding

class UserAdapter(
    private var userList: List<AdminViewUser>,
    private val currentAdminUid: String,
    private val onOptionSelected: (user: AdminViewUser, option: UserAction) -> Unit
) : RecyclerView.Adapter<UserAdapter.UserViewHolder>() {

    enum class UserAction {
        CHANGE_ROLE,
        DELETE_USER
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val binding = ItemUserAdminViewBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return UserViewHolder(binding)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        val user = userList[position]
        holder.bind(user)
    }

    override fun getItemCount(): Int = userList.size

    fun updateUsers(newUsers: List<AdminViewUser>) {
        userList = newUsers
        notifyDataSetChanged() // Podrías usar DiffUtil para mejor rendimiento
    }

    inner class UserViewHolder(private val binding: ItemUserAdminViewBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(user: AdminViewUser) {
            binding.textViewItemUserName.text = user.name
            binding.textViewItemUserEmail.text = user.email
            binding.textViewItemUserRole.text = user.role.replaceFirstChar { it.titlecase() } // ej. "User", "Admin"

            // Cambiar ícono o estilo basado en el rol si quieres
            if (user.role == "admin") {
                binding.imageViewUserIcon.setImageResource(R.drawable.ic_admin_placeholder) // Necesitarás este drawable
                binding.textViewItemUserRole.setBackgroundResource(R.drawable.bg_role_admin_chip) // Un fondo para el chip de admin
            } else {
                binding.imageViewUserIcon.setImageResource(R.drawable.ic_person_placeholder) // Drawable para usuario normal
                binding.textViewItemUserRole.setBackgroundResource(R.drawable.bg_role_user_chip) // Un fondo para el chip de user
            }

            // El administrador actual no puede modificar su propio rol o eliminarse a sí mismo desde la lista
            if (user.uid == currentAdminUid) {
                binding.imageButtonUserOptions.visibility = View.GONE
            } else {
                binding.imageButtonUserOptions.visibility = View.VISIBLE
                binding.imageButtonUserOptions.setOnClickListener { view ->
                    showPopupMenu(view, user)
                }
            }

            // Click listener para todo el item si quieres hacer algo más (ej. ver perfil detallado)
            // itemView.setOnClickListener { /* ... */ }
        }

        private fun showPopupMenu(view: View, user: AdminViewUser) {
            val popup = PopupMenu(view.context, view)
            popup.menuInflater.inflate(R.menu.menu_user_item_options, popup.menu) // Necesitarás crear este menú

            // Ajustar las opciones del menú según el rol actual del usuario
            val changeRoleItem = popup.menu.findItem(R.id.action_change_role)
            if (user.role == "admin") {
                changeRoleItem.title = "Degradar a Usuario"
            } else {
                changeRoleItem.title = "Promover a Administrador"
            }

            popup.setOnMenuItemClickListener { menuItem ->
                when (menuItem.itemId) {
                    R.id.action_change_role -> {
                        onOptionSelected(user, UserAction.CHANGE_ROLE)
                        true
                    }
                    R.id.action_delete_user -> {
                        onOptionSelected(user, UserAction.DELETE_USER)
                        true
                    }
                    else -> false
                }
            }
            popup.show()
        }
    }
}