package com.example.scrapsetu.data.repo

import com.example.scrapsetu.data.local.SessionDao
import com.example.scrapsetu.data.local.SessionEntity
import com.example.scrapsetu.data.model.User
import com.example.scrapsetu.data.remote.SupabaseClientProvider

import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.postgrest.postgrest

class AuthRepository(
    private val sessionDao: SessionDao
) {

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

        val role = getCurrentUser()?.role
        sessionDao.upsertSession(
            SessionEntity(
                email = email,
                password = password,
                role = role,
                loggedInAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun signOut() {
        client.auth.signOut()
        sessionDao.clearSession()
    }

    suspend fun restoreSession(): User? {
        val session = sessionDao.getSession() ?: return null

        return runCatching {
            signIn(session.email, session.password)
            getCurrentUser()
        }.getOrElse {
            sessionDao.clearSession()
            null
        }
    }

    suspend fun cachedRole(): String? = sessionDao.getSession()?.role

    fun currentUserId(): String? = client.auth.currentUserOrNull()?.id

    suspend fun getCurrentUser(): User? {
        val id = currentUserId() ?: return null
        return client.postgrest["users"]
            .select { filter { eq("id", id) } }
            .decodeSingleOrNull<User>()
    }

    suspend fun getUsersByIds(ids: List<String>): Map<String, User> {
        if (ids.isEmpty()) return emptyMap()

        val distinctIds = ids.distinct().toSet()
        return client.postgrest["users"]
            .select()
            .decodeList<User>()
            .filter { it.id in distinctIds }
            .associateBy { it.id }
    }
}