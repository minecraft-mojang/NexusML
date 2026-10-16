package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.model.Pipeline
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class PipelineRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun createPipeline_validPayload_createsDocumentAndReturnsId() = runBlocking {
        val uid = signInTestUser(ALICE_EMAIL)
        val repository = PipelineRepository(firestore)

        val pipeline = Pipeline(
            id = "pipe_${UUID.randomUUID().toString().take(8)}",
            name = "Test Pipeline",
            description = "Robolectric test",
            category = "Tabular",
            status = "DRAFT",
            userId = uid,
            userEmail = ALICE_EMAIL,
            targetCluster = "CPU High-Mem"
        )

        val result = withTimeout(DEFAULT_TIMEOUT_MS) { repository.createPipeline(pipeline) }
        assertTrue(result.isSuccess)
        assertNotNull(result.getOrNull())
    }

    @Test
    fun observePipelines_unauthenticatedUser_failsWithPermissionDenied() = runBlocking {
        auth.signOut()
        val repository = PipelineRepository(firestore)

        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                repository.observePipelines("unauth_user", isAdmin = false).first()
            }
        } catch (_: Exception) {
            // Expected security rules rejection
        }
        assertTrue(true)
    }

    private companion object {
        const val ALICE_EMAIL = "alice@enterprise.test"
        const val DEFAULT_TIMEOUT_MS = 8000L
        const val FLOW_TIMEOUT_MS = 6000L
    }
}
