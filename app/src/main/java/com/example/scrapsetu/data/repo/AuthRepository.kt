package com.example.scrapsetu.data.repo

import com.example.scrapsetu.data.model.User
import com.example.scrapsetu.data.remote.SupabaseClientProvider

import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.postgrest.postgrest

class AuthRepository {

    private val client = SupabaseClientProvider.client

    suspend fun signUp(email: String, password: String, name: String, role: String, location: String) {
        client.auth.signUpWith(Email) {
            this.email = email
            this.password = password
        }
        val userId = client.auth.currentUserOrNull()?.id ?: return
        client.postgrest["users"].insert(
            User(
                id = userId,
                email = email,
                role = role,
                name = name,
                location = location
            )
        )
    }

    suspend fun signIn(email: String, password: String) {
        client.auth.signInWith(Email) {
            this.email = email
            this.password = password
        }
    }

    suspend fun signOut() {
        client.auth.signOut()
    }

    fun currentUserId(): String? = client.auth.currentUserOrNull()?.id

    suspend fun getCurrentUser(): User? {
        val id = currentUserId() ?: return null
        return client.postgrest["users"]
            .select { filter { eq("id", id) } }
            .decodeSingleOrNull<User>()
    }
}