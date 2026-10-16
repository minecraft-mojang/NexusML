package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class BannedUserRecord(
    @DocumentId val userId: String = "",
    val email: String = "",
    val reason: String = "",
    val bannedBy: String = "",
    @ServerTimestamp val bannedAt: Timestamp? = null
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "userId" to userId,
        "email" to email,
        "reason" to reason,
        "bannedBy" to bannedBy,
        "bannedAt" to bannedAt
    ).filterValues { it != null }
}

@IgnoreExtraProperties
data class AuditLog(
    @DocumentId val id: String = "",
    val action: String = "",
    val targetUserId: String = "",
    val targetUserEmail: String = "",
    val actorId: String = "",
    val actorEmail: String = "",
    val details: String = "",
    @ServerTimestamp val timestamp: Timestamp? = null
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "id" to id,
        "action" to action,
        "targetUserId" to targetUserId,
        "targetUserEmail" to targetUserEmail,
        "actorId" to actorId,
        "actorEmail" to actorEmail,
        "details" to details,
        "timestamp" to timestamp
    ).filterValues { it != null }
}
