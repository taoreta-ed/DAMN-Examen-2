package com.example.damn_examen_2 // Reemplaza con tu paquete

data class AdminViewUser(
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    var role: String = "user" // 'user' o 'admin'
)