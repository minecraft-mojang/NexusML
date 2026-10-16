package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp
import org.json.JSONArray
import org.json.JSONObject

enum class PipelineStatus(val label: String) {
    DRAFT("Draft"),
    QUEUED("Queued"),
    RUNNING("Running"),
    COMPLETED("Completed"),
    FAILED("Failed"),
    PAUSED("Paused")
}

data class StageInfo(
    val name: String = "",
    val description: String = "",
    val status: String = "PENDING", // PENDING, RUNNING, COMPLETED, FAILED
    val durationSec: Int = 0,
    val detail: String = ""
)

@IgnoreExtraProperties
data class Pipeline(
    @DocumentId val id: String = "",
    val name: String = "",
    val description: String = "",
    val category: String = "LLM Fine-Tuning", // LLM Fine-Tuning, Computer Vision, Tabular, NLP, Audio
    val status: String = "DRAFT",
    val userId: String = "",
    val userEmail: String = "",
    val userName: String = "",
    val targetCluster: String = "NVIDIA H100 SXM5 x8", // Google TPU v5e-8, AMD MI300X, Distributed CPU
    val computeCostPerHour: Double = 24.50,
    val currentEpoch: Long = 0L,
    val totalEpochs: Long = 10L,
    val progressPercentage: Long = 0L,
    val batchSize: Long = 64L,
    val learningRate: Double = 0.0003,
    val optimizer: String = "AdamW (Cosine Decay)",
    val accuracy: Double = 0.0,
    val loss: Double = 1.0,
    val f1Score: Double = 0.0,
    val latencyMs: Long = 0L,
    val throughputQps: Long = 0L,
    val stagesJson: String = "",
    val metricsJson: String = "",
    val logsJson: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    @get:Exclude
    val statusEnum: PipelineStatus
        get() = try {
            PipelineStatus.valueOf(status)
        } catch (_: Exception) {
            PipelineStatus.DRAFT
        }

    @Exclude
    fun getStages(): List<StageInfo> {
        if (stagesJson.isBlank()) {
            return defaultStages()
        }
        return try {
            val jsonArray = JSONArray(stagesJson)
            val list = mutableListOf<StageInfo>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                list.add(
                    StageInfo(
                        name = obj.optString("name", "Stage ${i + 1}"),
                        description = obj.optString("description", ""),
                        status = obj.optString("status", "PENDING"),
                        durationSec = obj.optInt("durationSec", 0),
                        detail = obj.optString("detail", "")
                    )
                )
            }
            list
        } catch (_: Exception) {
            defaultStages()
        }
    }

    @Exclude
    fun getRecentLogs(): List<String> {
        if (logsJson.isBlank()) {
            return listOf(
                "SYSTEM: Initialized pipeline configuration",
                "CLUSTER: Allocated target nodes on $targetCluster",
                "SECURITY: Identity verified for $userEmail"
            )
        }
        return try {
            val arr = JSONArray(logsJson)
            val list = mutableListOf<String>()
            for (i in 0 until arr.length()) {
                list.add(arr.getString(i))
            }
            list
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun toMap(): Map<String, Any?> {
        val map = mutableMapOf<String, Any?>(
            "id" to id,
            "name" to name,
            "description" to description,
            "category" to category,
            "status" to status,
            "userId" to userId,
            "userEmail" to userEmail,
            "userName" to userName,
            "targetCluster" to targetCluster,
            "computeCostPerHour" to computeCostPerHour,
            "currentEpoch" to currentEpoch,
            "totalEpochs" to totalEpochs,
            "progressPercentage" to progressPercentage,
            "batchSize" to batchSize,
            "learningRate" to learningRate,
            "optimizer" to optimizer,
            "accuracy" to accuracy,
            "loss" to loss,
            "f1Score" to f1Score,
            "latencyMs" to latencyMs,
            "throughputQps" to throughputQps,
            "stagesJson" to stagesJson,
            "metricsJson" to metricsJson,
            "logsJson" to logsJson,
            "createdAt" to createdAt,
            "updatedAt" to updatedAt
        )
        return map.filterValues { it != null }
    }

    companion object {
        fun defaultStages(): List<StageInfo> = listOf(
            StageInfo(
                name = "Data Ingestion & Sharding",
                description = "Ingest multi-terabyte parquet datasets from object store",
                status = "PENDING",
                detail = "3.2M rows validated • 0 Schema Drift"
            ),
            StageInfo(
                name = "Feature Engineering & Tokenization",
                description = "BPE Tokenizer (128k vocab) & Embedding Cache",
                status = "PENDING",
                detail = "Zero-padding • Tensor dimension (B, 2048)"
            ),
            StageInfo(
                name = "Distributed Training",
                description = "FSDP ZeRO-3 sharded gradient execution",
                status = "PENDING",
                detail = "Mixed Precision BF16 • AdamW Cosine"
            ),
            StageInfo(
                name = "Evaluation & Benchmarks",
                description = "Validation loss convergence & MMLU / GSM8k tests",
                status = "PENDING",
                detail = "Target Accuracy > 94.2% • ROC-AUC 0.98"
            ),
            StageInfo(
                name = "Quantization & ONNX Export",
                description = "INT8 / FP8 tensor quantization for low latency",
                status = "PENDING",
                detail = "Weights compressed 4.2x • 1.8GB bundle"
            ),
            StageInfo(
                name = "Endpoint Deployment",
                description = "High-availability Triton inference microservice",
                status = "PENDING",
                detail = "vLLM Engine • P99 < 18ms • 1400 QPS"
            )
        )

        fun stagesToJson(stages: List<StageInfo>): String {
            val arr = JSONArray()
            stages.forEach { stage ->
                val obj = JSONObject().apply {
                    put("name", stage.name)
                    put("description", stage.description)
                    put("status", stage.status)
                    put("durationSec", stage.durationSec)
                    put("detail", stage.detail)
                }
                arr.put(obj)
            }
            return arr.toString()
        }
    }
}
