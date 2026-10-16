package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.Pipeline
import com.example.data.model.PipelineStatus
import com.example.data.model.StageInfo
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import org.json.JSONArray
import java.util.UUID

class PipelineRepository(private val db: FirebaseFirestore) {

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private val auth = FirebaseAuth.getInstance()

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in before modifying pipelines.")
    }

    suspend fun createPipeline(pipeline: Pipeline): Result<String> {
        val uid = requireUserId()
        val userEmail = auth.currentUser?.email ?: ""
        val userName = auth.currentUser?.displayName ?: userEmail.substringBefore("@")
        val pipelineId = if (pipeline.id.isNotBlank()) pipeline.id else "pipe_${UUID.randomUUID().toString().take(12)}"

        val initialStages = if (pipeline.stagesJson.isBlank()) {
            Pipeline.stagesToJson(Pipeline.defaultStages())
        } else {
            pipeline.stagesJson
        }

        val initialLogs = if (pipeline.logsJson.isBlank()) {
            JSONArray(listOf(
                "SYSTEM: Initialized ML Pipeline runtime configuration",
                "SECURITY: User session authenticated for $userEmail",
                "CLUSTER: Resource allocated on target ${pipeline.targetCluster}"
            )).toString()
        } else {
            pipeline.logsJson
        }

        val docRef = db.collection("pipelines").document(pipelineId)
        val payload = pipeline.toMap().toMutableMap().apply {
            put("id", pipelineId)
            put("userId", uid)
            put("userEmail", userEmail)
            put("userName", userName)
            put("stagesJson", initialStages)
            put("logsJson", initialLogs)
            put("createdAt", FieldValue.serverTimestamp())
            put("updatedAt", FieldValue.serverTimestamp())
        }.filterValues { it != null }

        return try {
            docRef.set(payload).await()
            try {
                db.collection("users").document(uid).update(
                    mapOf(
                        "pipelineCount" to FieldValue.increment(1),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } catch (_: Exception) {
                // Ignore if user doc not yet initialized
            }
            Result.success(pipelineId)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    fun observePipelines(userId: String, isAdmin: Boolean): Flow<List<Pipeline>> = callbackFlow {
        val query: Query = if (isAdmin) {
            db.collection("pipelines")
        } else {
            db.collection("pipelines").whereEqualTo("userId", userId)
        }

        val registration = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                handleFirestoreError(error, OperationType.LIST, "pipelines")
                close(error)
                return@addSnapshotListener
            }
            val items = snapshot?.documents?.mapNotNull { it.toObject(Pipeline::class.java) } ?: emptyList()
            trySend(items.sortedByDescending { it.updatedAt })
        }
        awaitClose { registration.remove() }
    }

    fun observePipeline(pipelineId: String): Flow<Pipeline?> = callbackFlow {
        val docRef = db.collection("pipelines").document(pipelineId)
        val registration = docRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                handleFirestoreError(error, OperationType.GET, docRef.path)
                close(error)
                return@addSnapshotListener
            }
            if (snapshot != null && snapshot.exists()) {
                trySend(snapshot.toObject(Pipeline::class.java))
            } else {
                trySend(null)
            }
        }
        awaitClose { registration.remove() }
    }

    suspend fun updatePipeline(pipeline: Pipeline): Result<Unit> {
        val docRef = db.collection("pipelines").document(pipeline.id)
        val updates = pipeline.toMap().toMutableMap().apply {
            put("updatedAt", FieldValue.serverTimestamp())
        }.filterValues { it != null }

        return try {
            docRef.update(updates).await()
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun deletePipeline(pipelineId: String): Result<Unit> {
        val uid = requireUserId()
        val docRef = db.collection("pipelines").document(pipelineId)
        return try {
            docRef.delete().await()
            db.collection("users").document(uid).update(
                mapOf(
                    "pipelineCount" to FieldValue.increment(-1),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun startExecutionStep(
        pipeline: Pipeline,
        newStatus: PipelineStatus,
        progress: Long,
        currentEpoch: Long,
        loss: Double,
        accuracy: Double,
        updatedStages: List<StageInfo>,
        newLogLine: String
    ): Result<Unit> {
        val docRef = db.collection("pipelines").document(pipeline.id)
        val existingLogs = pipeline.getRecentLogs().toMutableList()
        existingLogs.add(newLogLine)
        if (existingLogs.size > 50) {
            existingLogs.removeAt(0)
        }

        val updates = mapOf(
            "status" to newStatus.name,
            "progressPercentage" to progress,
            "currentEpoch" to currentEpoch,
            "loss" to loss,
            "accuracy" to accuracy,
            "stagesJson" to Pipeline.stagesToJson(updatedStages),
            "logsJson" to JSONArray(existingLogs).toString(),
            "updatedAt" to FieldValue.serverTimestamp()
        )

        return try {
            docRef.update(updates).await()
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }
}
