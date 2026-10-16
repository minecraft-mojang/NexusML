package com.example.ui.admin

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.AuditLog
import com.example.data.model.UserProfile
import com.example.data.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AdminViewModel(application: Application) : AndroidViewModel(application) {

    private val userRepository = UserRepository(application)

    private val _rawUsers = MutableStateFlow<List<UserProfile>>(emptyList())
    private val _userSearchQuery = MutableStateFlow("")
    val userSearchQuery = _userSearchQuery.asStateFlow()

    private val _filterTab = MutableStateFlow("ALL") // ALL, ACTIVE, BANNED
    val filterTab = _filterTab.asStateFlow()

    private val _auditLogs = MutableStateFlow<List<AuditLog>>(emptyList())
    val auditLogs = _auditLogs.asStateFlow()

    private val _adminMessage = MutableStateFlow<String?>(null)
    val adminMessage = _adminMessage.asStateFlow()

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing = _isProcessing.asStateFlow()

    val filteredUsers: StateFlow<List<UserProfile>> = combine(
        _rawUsers,
        _userSearchQuery,
        _filterTab
    ) { users, query, tab ->
        users.filter { user ->
            val matchesQuery = query.isBlank() ||
                    user.displayName.contains(query, ignoreCase = true) ||
                    user.email.contains(query, ignoreCase = true) ||
                    user.role.contains(query, ignoreCase = true)
            val matchesTab = when (tab) {
                "ACTIVE" -> !user.isBanned
                "BANNED" -> user.isBanned
                else -> true
            }
            matchesQuery && matchesTab
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    init {
        observeUsers()
        observeLogs()
    }

    private fun observeUsers() {
        viewModelScope.launch {
            userRepository.observeAllUsers()
                .catch { e ->
                    _adminMessage.value = "Failed to load users: ${e.localizedMessage}"
                }
                .collect { list ->
                    _rawUsers.value = list
                }
        }
    }

    private fun observeLogs() {
        viewModelScope.launch {
            userRepository.observeAuditLogs()
                .catch { /* ignore */ }
                .collect { list ->
                    _auditLogs.value = list
                }
        }
    }

    fun setSearchQuery(query: String) {
        _userSearchQuery.value = query
    }

    fun setFilterTab(tab: String) {
        _filterTab.value = tab
    }

    fun clearAdminMessage() {
        _adminMessage.value = null
    }

    fun banUser(user: UserProfile, reason: String) {
        if (reason.isBlank()) {
            _adminMessage.value = "Please enter a reason for suspending this user."
            return
        }
        viewModelScope.launch {
            _isProcessing.value = true
            val result = userRepository.banUser(user.userId, user.email, reason)
            _isProcessing.value = false
            if (result.isSuccess) {
                _adminMessage.value = "User ${user.email} banned for: $reason"
            } else {
                _adminMessage.value = "Failed to ban user: ${result.exceptionOrNull()?.localizedMessage}"
            }
        }
    }

    fun unbanUser(user: UserProfile) {
        viewModelScope.launch {
            _isProcessing.value = true
            val result = userRepository.unbanUser(user.userId, user.email)
            _isProcessing.value = false
            if (result.isSuccess) {
                _adminMessage.value = "Reinstated access for ${user.email}"
            } else {
                _adminMessage.value = "Failed to reinstate: ${result.exceptionOrNull()?.localizedMessage}"
            }
        }
    }

    fun updateRole(user: UserProfile, newRole: String) {
        viewModelScope.launch {
            _isProcessing.value = true
            val result = userRepository.updateUserRole(user.userId, newRole)
            _isProcessing.value = false
            if (result.isSuccess) {
                _adminMessage.value = "Updated ${user.displayName}'s role to $newRole"
            } else {
                _adminMessage.value = "Failed to update role: ${result.exceptionOrNull()?.localizedMessage}"
            }
        }
    }
}
