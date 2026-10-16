package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

const val ADMIN_EMAIL = "joshua270614@gmail.com"

@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val userId: String = "",
    val email: String = "",
    val displayName: String = "",
    val photoUrl: String? = null,
    val role: String = "ML_ENGINEER", // ADMIN, ML_ENGINEER, DATA_SCIENTIST, VIEWER
    val isBanned: Boolean = false,
    val banReason: String? = null,
    @ServerTimestamp val bannedAt: Timestamp? = null,
    val bannedBy: String? = null,
    val pipelineCount: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    @get:Exclude
    val isAdmin: Boolean
        get() = email.equals(ADMIN_EMAIL, ignoreCase = true) || role == "ADMIN"

    fun toMap(): Map<String, Any?> {
        val map = mutableMapOf<String, Any?>(
            "userId" to userId,
            "email" to email,
            "displayName" to displayName,
            "photoUrl" to photoUrl,
            "role" to role,
            "isBanned" to isBanned,
            "banReason" to banReason,
            "bannedAt" to bannedAt,
            "bannedBy" to bannedBy,
            "pipelineCount" to pipelineCount,
            "createdAt" to createdAt,
            "updatedAt" to updatedAt
        )
        return map.filterValues { it != null }
    }
}
