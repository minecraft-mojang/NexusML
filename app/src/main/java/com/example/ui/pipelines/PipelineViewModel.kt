package com.example.ui.pipelines

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.Pipeline
import com.example.data.model.PipelineStatus
import com.example.data.model.StageInfo
import com.example.data.model.UserProfile
import com.example.data.repository.PipelineRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

sealed class PipelineUiState {
    object Loading : PipelineUiState()
    data class Success(val pipelines: List<Pipeline>) : PipelineUiState()
    data class Error(val message: String) : PipelineUiState()
}

class PipelineViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = PipelineRepository(application)

    private val _rawPipelines = MutableStateFlow<List<Pipeline>>(emptyList())
    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow("ALL")
    val selectedCategory = _selectedCategory.asStateFlow()

    private val _selectedStatus = MutableStateFlow("ALL")
    val selectedStatus = _selectedStatus.asStateFlow()

    private val _selectedPipeline = MutableStateFlow<Pipeline?>(null)
    val selectedPipeline = _selectedPipeline.asStateFlow()

    private val _isCreating = MutableStateFlow(false)
    val isCreating = _isCreating.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage = _statusMessage.asStateFlow()

    private var activeSimJob: Job? = null

    val filteredPipelines: StateFlow<List<Pipeline>> = combine(
        _rawPipelines,
        _searchQuery,
        _selectedCategory,
        _selectedStatus
    ) { pipelines, query, category, status ->
        pipelines.filter { pipeline ->
            val matchesQuery = query.isBlank() ||
                    pipeline.name.contains(query, ignoreCase = true) ||
                    pipeline.description.contains(query, ignoreCase = true) ||
                    pipeline.targetCluster.contains(query, ignoreCase = true)
            val matchesCategory = category == "ALL" || pipeline.category.equals(category, ignoreCase = true)
            val matchesStatus = status == "ALL" || pipeline.status.equals(status, ignoreCase = true)
            matchesQuery && matchesCategory && matchesStatus
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    fun initialize(profile: UserProfile) {
        viewModelScope.launch {
            repository.observePipelines(profile.userId, profile.isAdmin)
                .catch { e ->
                    _statusMessage.value = "Failed to load pipelines: ${e.localizedMessage}"
                }
                .collect { list ->
                    _rawPipelines.value = list
                    // If a pipeline is currently selected, update its reference
                    _selectedPipeline.value?.let { current ->
                        val updated = list.find { it.id == current.id }
                        if (updated != null) {
                            _selectedPipeline.value = updated
                        }
                    }
                }
        }
    }

    fun selectPipeline(pipeline: Pipeline?) {
        _selectedPipeline.value = pipeline
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSelectedCategory(category: String) {
        _selectedCategory.value = category
    }

    fun setSelectedStatus(status: String) {
        _selectedStatus.value = status
    }

    fun clearStatusMessage() {
        _statusMessage.value = null
    }

    fun createPipeline(
        name: String,
        description: String,
        category: String,
        targetCluster: String,
        batchSize: Long,
        learningRate: Double,
        optimizer: String,
        epochs: Long
    ) {
        viewModelScope.launch {
            _isCreating.value = true
            val pipeline = Pipeline(
                id = "pipe_${UUID.randomUUID().toString().take(12)}",
                name = name,
                description = description,
                category = category,
                targetCluster = targetCluster,
                batchSize = batchSize,
                learningRate = learningRate,
                optimizer = optimizer,
                totalEpochs = epochs,
                status = "DRAFT",
                loss = 1.65,
                accuracy = 0.0,
                f1Score = 0.0,
                computeCostPerHour = when {
                    targetCluster.contains("H100") -> 28.50
                    targetCluster.contains("TPU") -> 19.80
                    targetCluster.contains("MI300") -> 22.00
                    else -> 8.50
                }
            )

            val result = repository.createPipeline(pipeline)
            _isCreating.value = false
            if (result.isSuccess) {
                _statusMessage.value = "Pipeline '${name}' initialized successfully"
            } else {
                _statusMessage.value = "Creation error: ${result.exceptionOrNull()?.localizedMessage}"
            }
        }
    }

    fun createPresetPipeline(presetName: String) {
        when (presetName) {
            "LLAMA3_QLORA" -> createPipeline(
                name = "Llama-3-70B QLoRA Instruct Tuning",
                description = "4-bit quantized low-rank adaptation on enterprise conversational telemetry",
                category = "LLM Fine-Tuning",
                targetCluster = "NVIDIA H100 SXM5 x8",
                batchSize = 32L,
                learningRate = 0.0002,
                optimizer = "AdamW (Paged 8bit)",
                epochs = 5L
            )
            "VIT_DETECTION" -> createPipeline(
                name = "Vision Transformer (ViT-H) Defect Inspection",
                description = "High-resolution multi-scale visual defect segmentation pipeline",
                category = "Computer Vision",
                targetCluster = "Google TPU v5e-8 Node",
                batchSize = 128L,
                learningRate = 0.0005,
                optimizer = "Lion (Cosine Decay)",
                epochs = 12L
            )
            "TABULAR_FRAUD" -> createPipeline(
                name = "Real-Time Transaction Fraud Risk XGBoost",
                description = "Sub-10ms latency gradient boosting tree pipeline with drift monitoring",
                category = "Tabular",
                targetCluster = "Distributed CPU High-Mem Cluster",
                batchSize = 512L,
                learningRate = 0.01,
                optimizer = "Histogram Gradient Boost",
                epochs = 8L
            )
            "WHISPER_SPEECH" -> createPipeline(
                name = "Whisper Large-v3 Enterprise Multilingual Audio",
                description = "Domain-adapted acoustic feature extraction and transcription alignment",
                category = "Audio",
                targetCluster = "AMD MI300X 4-way",
                batchSize = 64L,
                learningRate = 0.0001,
                optimizer = "AdamW",
                epochs = 6L
            )
        }
    }

    fun deletePipeline(pipeline: Pipeline) {
        viewModelScope.launch {
            if (_selectedPipeline.value?.id == pipeline.id) {
                _selectedPipeline.value = null
            }
            val result = repository.deletePipeline(pipeline.id)
            if (result.isSuccess) {
                _statusMessage.value = "Pipeline '${pipeline.name}' deleted"
            } else {
                _statusMessage.value = "Failed to delete: ${result.exceptionOrNull()?.localizedMessage}"
            }
        }
    }

    fun clonePipeline(pipeline: Pipeline) {
        viewModelScope.launch {
            val cloned = pipeline.copy(
                id = "pipe_${UUID.randomUUID().toString().take(12)}",
                name = "${pipeline.name} (Clone)",
                status = "DRAFT",
                progressPercentage = 0L,
                currentEpoch = 0L,
                accuracy = 0.0,
                loss = 1.5,
                f1Score = 0.0
            )
            val result = repository.createPipeline(cloned)
            if (result.isSuccess) {
                _statusMessage.value = "Cloned as '${cloned.name}'"
            }
        }
    }

    fun pausePipeline(pipeline: Pipeline) {
        activeSimJob?.cancel()
        activeSimJob = null
        viewModelScope.launch {
            val updated = pipeline.copy(status = "PAUSED")
            repository.updatePipeline(updated)
            _selectedPipeline.value = updated
            _statusMessage.value = "Pipeline execution paused"
        }
    }

    fun runPipelineInteractive(pipeline: Pipeline) {
        activeSimJob?.cancel()
        val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.US)

        activeSimJob = viewModelScope.launch {
            _statusMessage.value = "Starting distributed execution on ${pipeline.targetCluster}..."
            var currentP = pipeline.copy(status = "RUNNING")
            _selectedPipeline.value = currentP

            val stages = currentP.getStages().toMutableList()
            val totalEpochs = if (currentP.totalEpochs > 0) currentP.totalEpochs else 10L

            // Stage 1: Ingestion
            stages[0] = stages[0].copy(status = "RUNNING", detail = "Sharding parquet partitions across workers...")
            repository.startExecutionStep(
                pipeline = currentP,
                newStatus = PipelineStatus.RUNNING,
                progress = 10L,
                currentEpoch = 0L,
                loss = 1.65,
                accuracy = 0.12,
                updatedStages = stages,
                newLogLine = "[${timeFormat.format(Date())}] [INGEST] Allocated 24 worker shards • Stream throughput 4.2 GB/s"
            )
            delay(1200)

            stages[0] = stages[0].copy(status = "COMPLETED", detail = "3.2M rows validated • 0 schema violations", durationSec = 14)
            stages[1] = stages[1].copy(status = "RUNNING", detail = "Computing BPE embeddings & vector normalization...")
            repository.startExecutionStep(
                pipeline = currentP,
                newStatus = PipelineStatus.RUNNING,
                progress = 25L,
                currentEpoch = 0L,
                loss = 1.42,
                accuracy = 0.28,
                updatedStages = stages,
                newLogLine = "[${timeFormat.format(Date())}] [TOKENIZE] Cached 128k vocabulary tokens in distributed NVMe cache"
            )
            delay(1200)

            stages[1] = stages[1].copy(status = "COMPLETED", detail = "Feature store synced • Tensor shape (B, 2048)", durationSec = 22)
            stages[2] = stages[2].copy(status = "RUNNING", detail = "Executing distributed forward/backward passes...")

            // Training Epochs
            var currentLoss = 1.25
            var currentAcc = 0.45
            for (epoch in 1..totalEpochs) {
                delay(800)
                currentLoss = (currentLoss * 0.78).coerceAtLeast(0.062)
                currentAcc = (currentAcc + (1.0 - currentAcc) * 0.35).coerceAtMost(0.984)
                val epochProgress = 25L + ((epoch.toDouble() / totalEpochs) * 45).toLong()

                stages[2] = stages[2].copy(
                    status = "RUNNING",
                    detail = "Epoch $epoch/$totalEpochs • Loss: ${String.format(Locale.US, "%.4f", currentLoss)} • Acc: ${String.format(Locale.US, "%.1f", currentAcc * 100)}%"
                )

                repository.startExecutionStep(
                    pipeline = currentP,
                    newStatus = PipelineStatus.RUNNING,
                    progress = epochProgress,
                    currentEpoch = epoch,
                    loss = currentLoss,
                    accuracy = currentAcc,
                    updatedStages = stages,
                    newLogLine = "[${timeFormat.format(Date())}] [TRAIN] Epoch $epoch/$totalEpochs complete • Loss: ${String.format(Locale.US, "%.4f", currentLoss)} • Gradient Norm: 0.84 • GPU Mem: 88%"
                )
            }

            stages[2] = stages[2].copy(status = "COMPLETED", durationSec = (totalEpochs * 18).toInt())

            // Stage 4: Evaluation
            stages[3] = stages[3].copy(status = "RUNNING", detail = "Benchmarking test holdout set & computing ROC-AUC...")
            repository.startExecutionStep(
                pipeline = currentP,
                newStatus = PipelineStatus.RUNNING,
                progress = 78L,
                currentEpoch = totalEpochs,
                loss = currentLoss,
                accuracy = currentAcc,
                updatedStages = stages,
                newLogLine = "[${timeFormat.format(Date())}] [EVAL] Validation Accuracy: ${String.format(Locale.US, "%.2f", currentAcc * 100)}% • F1 Score: 0.962 • P99 Drift: 0.012"
            )
            delay(1200)

            stages[3] = stages[3].copy(status = "COMPLETED", detail = "Test Accuracy ${String.format(Locale.US, "%.1f", currentAcc * 100)}% • F1: 0.962", durationSec = 16)

            // Stage 5: Quantization
            stages[4] = stages[4].copy(status = "RUNNING", detail = "Applying AWQ 4-bit / INT8 quantization & compiling ONNX graph...")
            repository.startExecutionStep(
                pipeline = currentP,
                newStatus = PipelineStatus.RUNNING,
                progress = 88L,
                currentEpoch = totalEpochs,
                loss = currentLoss,
                accuracy = currentAcc,
                updatedStages = stages,
                newLogLine = "[${timeFormat.format(Date())}] [EXPORT] Quantization complete: model size 3.8GB -> 920MB • ONNX runtime target ready"
            )
            delay(1200)

            stages[4] = stages[4].copy(status = "COMPLETED", detail = "INT8 Weights • Compression 4.1x", durationSec = 11)

            // Stage 6: Deployment
            stages[5] = stages[5].copy(status = "RUNNING", detail = "Spawning Triton inference cluster pods...")
            repository.startExecutionStep(
                pipeline = currentP,
                newStatus = PipelineStatus.RUNNING,
                progress = 95L,
                currentEpoch = totalEpochs,
                loss = currentLoss,
                accuracy = currentAcc,
                updatedStages = stages,
                newLogLine = "[${timeFormat.format(Date())}] [DEPLOY] High-Availability cluster online • Latency: 14ms • Throughput: 1650 QPS"
            )
            delay(1200)

            stages[5] = stages[5].copy(status = "COMPLETED", detail = "Live Endpoint: https://inference.enterprise.internal/v2/models", durationSec = 8)

            // Final Completed State
            repository.startExecutionStep(
                pipeline = currentP,
                newStatus = PipelineStatus.COMPLETED,
                progress = 100L,
                currentEpoch = totalEpochs,
                loss = currentLoss,
                accuracy = currentAcc,
                updatedStages = stages,
                newLogLine = "[${timeFormat.format(Date())}] [SUCCESS] Pipeline execution finalized successfully. Artifacts logged to Model Registry."
            )
            _statusMessage.value = "Pipeline '${pipeline.name}' completed with ${String.format(Locale.US, "%.1f", currentAcc * 100)}% accuracy!"
        }
    }
}
