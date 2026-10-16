package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.ADMIN_EMAIL
import com.example.data.model.AuditLog
import com.example.data.model.BannedUserRecord
import com.example.data.model.UserProfile
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.UUID

class UserRepository(private val db: FirebaseFirestore) {

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private val auth = FirebaseAuth.getInstance()

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in before accessing user repository.")
    }

    suspend fun syncUserProfile(firebaseUser: FirebaseUser): UserProfile {
        val uid = firebaseUser.uid
        val email = firebaseUser.email ?: "engineer@enterprise.internal"
        val displayName = firebaseUser.displayName?.ifBlank { null } ?: email.substringBefore("@")
        val photoUrl = firebaseUser.photoUrl?.toString()
        val docRef = db.collection("users").document(uid)

        val isMasterAdmin = email.equals(ADMIN_EMAIL, ignoreCase = true)
        val defaultRole = if (isMasterAdmin) "ADMIN" else "ML_ENGINEER"

        return try {
            val snapshot = docRef.get().await()
            if (snapshot.exists()) {
                val profile = snapshot.toObject(UserProfile::class.java) ?: UserProfile(
                    userId = uid,
                    email = email,
                    displayName = displayName,
                    role = defaultRole
                )
                // If it is the designated admin email, ensure role is ADMIN
                if (isMasterAdmin && profile.role != "ADMIN") {
                    docRef.update(
                        mapOf(
                            "role" to "ADMIN",
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
                    profile.copy(role = "ADMIN")
                } else {
                    docRef.update("updatedAt", FieldValue.serverTimestamp()).await()
                    profile
                }
            } else {
                val newProfile = UserProfile(
                    userId = uid,
                    email = email,
                    displayName = displayName,
                    photoUrl = photoUrl,
                    role = defaultRole,
                    isBanned = false,
                    pipelineCount = 0L
                )
                val payload = mutableMapOf<String, Any?>(
                    "userId" to uid,
                    "email" to email,
                    "displayName" to displayName,
                    "photoUrl" to photoUrl,
                    "role" to defaultRole,
                    "isBanned" to false,
                    "pipelineCount" to 0L,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                ).filterValues { it != null }

                docRef.set(payload).await()
                newProfile
            }
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            UserProfile(
                userId = uid,
                email = email,
                displayName = displayName,
                role = defaultRole
            )
        }
    }

    fun observeUserProfile(userId: String): Flow<UserProfile?> = callbackFlow {
        val docRef = db.collection("users").document(userId)
        val registration = docRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                handleFirestoreError(error, OperationType.GET, docRef.path)
                close(error)
                return@addSnapshotListener
            }
            if (snapshot != null && snapshot.exists()) {
                trySend(snapshot.toObject(UserProfile::class.java))
            } else {
                trySend(null)
            }
        }
        awaitClose { registration.remove() }
    }

    fun observeAllUsers(): Flow<List<UserProfile>> = callbackFlow {
        val colRef = db.collection("users")
        val registration = colRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                handleFirestoreError(error, OperationType.LIST, colRef.path)
                close(error)
                return@addSnapshotListener
            }
            val list = snapshot?.documents?.mapNotNull { it.toObject(UserProfile::class.java) } ?: emptyList()
            trySend(list)
        }
        awaitClose { registration.remove() }
    }

    suspend fun banUser(targetUserId: String, targetEmail: String, reason: String): Result<Unit> {
        val adminUid = requireUserId()
        val adminEmail = auth.currentUser?.email ?: ADMIN_EMAIL
        val userDoc = db.collection("users").document(targetUserId)
        val banDoc = db.collection("banned_users").document(targetUserId)
        val logDoc = db.collection("audit_logs").document(UUID.randomUUID().toString())

        return try {
            val userUpdates = mapOf(
                "isBanned" to true,
                "banReason" to reason,
                "bannedBy" to adminUid,
                "bannedAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            userDoc.update(userUpdates).await()

            val banPayload = mapOf(
                "userId" to targetUserId,
                "email" to targetEmail,
                "reason" to reason,
                "bannedBy" to adminUid,
                "bannedAt" to FieldValue.serverTimestamp()
            )
            banDoc.set(banPayload).await()

            val auditPayload = mapOf(
                "id" to logDoc.id,
                "action" to "BAN_USER",
                "targetUserId" to targetUserId,
                "targetUserEmail" to targetEmail,
                "actorId" to adminUid,
                "actorEmail" to adminEmail,
                "details" to "Banned for toxic conduct: $reason",
                "timestamp" to FieldValue.serverTimestamp()
            )
            logDoc.set(auditPayload).await()

            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, userDoc.path)
            Result.failure(e)
        }
    }

    suspend fun unbanUser(targetUserId: String, targetEmail: String): Result<Unit> {
        val adminUid = requireUserId()
        val adminEmail = auth.currentUser?.email ?: ADMIN_EMAIL
        val userDoc = db.collection("users").document(targetUserId)
        val banDoc = db.collection("banned_users").document(targetUserId)
        val logDoc = db.collection("audit_logs").document(UUID.randomUUID().toString())

        return try {
            val userUpdates = mapOf(
                "isBanned" to false,
                "banReason" to null,
                "bannedBy" to null,
                "bannedAt" to null,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            userDoc.update(userUpdates).await()
            banDoc.delete().await()

            val auditPayload = mapOf(
                "id" to logDoc.id,
                "action" to "UNBAN_USER",
                "targetUserId" to targetUserId,
                "targetUserEmail" to targetEmail,
                "actorId" to adminUid,
                "actorEmail" to adminEmail,
                "details" to "Account reinstated by admin",
                "timestamp" to FieldValue.serverTimestamp()
            )
            logDoc.set(auditPayload).await()

            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, userDoc.path)
            Result.failure(e)
        }
    }

    suspend fun updateUserRole(targetUserId: String, newRole: String): Result<Unit> {
        val userDoc = db.collection("users").document(targetUserId)
        return try {
            userDoc.update(
                mapOf(
                    "role" to newRole,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, userDoc.path)
            Result.failure(e)
        }
    }

    fun observeAuditLogs(): Flow<List<AuditLog>> = callbackFlow {
        val colRef = db.collection("audit_logs").orderBy("timestamp", Query.Direction.DESCENDING).limit(50)
        val registration = colRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                handleFirestoreError(error, OperationType.LIST, "audit_logs")
                close(error)
                return@addSnapshotListener
            }
            val list = snapshot?.documents?.mapNotNull { it.toObject(AuditLog::class.java) } ?: emptyList()
            trySend(list)
        }
        awaitClose { registration.remove() }
    }
}
