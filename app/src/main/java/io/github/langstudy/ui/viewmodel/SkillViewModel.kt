package io.github.langstudy.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import io.github.langstudy.data.local.entity.SkillEntity
import io.github.langstudy.data.local.entity.Subtask
import io.github.langstudy.data.repository.SettingsRepository
import io.github.langstudy.data.repository.SkillRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SkillViewModel(
    private val repository: SkillRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {
    private var userId: String? = null

    val allSkills: StateFlow<List<SkillEntity>> = repository.allSkills
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedLanguage = MutableStateFlow<String?>(null)
    val selectedLanguage: StateFlow<String?> = _selectedLanguage.asStateFlow()

    private val _learnedLanguages = MutableStateFlow<List<String>>(emptyList())
    val learnedLanguages: StateFlow<List<String>> = _learnedLanguages.asStateFlow()

    val filteredSkills: StateFlow<List<SkillEntity>> =
        combine(allSkills, _searchQuery, _selectedLanguage) { skills, query, lang ->
            skills.filter { skill ->
                (query.isBlank() || skill.name.contains(query, ignoreCase = true) ||
                        skill.level.contains(query, ignoreCase = true) ||
                        skill.status.contains(query, ignoreCase = true) ||
                        skill.language.contains(query, ignoreCase = true)) &&
                        (lang == null || lang.isBlank() || skill.language == lang)
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val availableLanguages: StateFlow<List<String>> = allSkills.map { skills ->
        skills.map { it.language }.filter { it.isNotBlank() }.distinct().sorted()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _error = MutableSharedFlow<String>()
    val error: SharedFlow<String> = _error.asSharedFlow()

    fun initUserId(id: String) {
        if (userId == id) return
        userId = id
        viewModelScope.launch {
            repository.startSync(id)
                .catch { e -> _error.emit("Sync failed: ${e.message}") }
                .collect()
        }
        viewModelScope.launch {
            settingsRepository.getUserSettings(id).collect { settings ->
                _learnedLanguages.value = settings.learnedLanguages
                if (_selectedLanguage.value == null) {
                    _selectedLanguage.value = settings.languageLearning
                }
            }
        }
    }

    fun addSkill(input: String, language: String = "") {
        if (input.isBlank()) {
            viewModelScope.launch { _error.emit("Skill name cannot be empty") }
            return
        }

        val skillNames = input.split("\n").filter { it.trim().isNotBlank() }
        val currentMaxPriority = allSkills.value.maxOfOrNull { it.priority } ?: -1

        viewModelScope.launch {
            try {
                skillNames.forEachIndexed { index, name ->
                    repository.insert(
                        SkillEntity(
                            name = name.trim(),
                            language = language,
                            priority = currentMaxPriority + 1 + index
                        ), userId
                    )
                }
            } catch (e: Exception) {
                _error.emit("Failed to add skill: ${e.message}")
            }
        }
    }

    fun moveSkill(fromIndex: Int, toIndex: Int) {
        val list = filteredSkills.value.toMutableList()
        if (fromIndex !in list.indices || toIndex !in list.indices) return

        val item = list.removeAt(fromIndex)
        list.add(toIndex, item)
        updateSkillOrder(list)
    }

    fun updateSkillOrder(newList: List<SkillEntity>) {
        viewModelScope.launch {
            newList.forEachIndexed { index, skill ->
                if (skill.priority != index) {
                    repository.update(skill.copy(priority = index), userId)
                }
            }
        }
    }

    fun updateSkill(skill: SkillEntity) {
        val (calculatedProgress, calculatedStatus) = if (skill.subtasks.isEmpty()) {
            when (skill.status) {
                "PROFICIENT" -> 100 to "PROFICIENT"
                "IN_PROGRESS" -> 100 to "IN_PROGRESS"
                else -> 0 to "NOT_STARTED"
            }
        } else {
            val total = skill.subtasks.size
            val proficientCount = skill.subtasks.count { it.status == "PROFICIENT" }
            val inProgressCount = skill.subtasks.count { it.status == "IN_PROGRESS" }

            val progress = (proficientCount * 100) / total
            val status = when {
                proficientCount == total -> "PROFICIENT"
                proficientCount > 0 || inProgressCount > 0 -> "IN_PROGRESS"
                else -> "NOT_STARTED"
            }
            progress to status
        }

        viewModelScope.launch {
            repository.update(
                skill.copy(
                    progress = calculatedProgress,
                    status = calculatedStatus,
                    lastUpdated = System.currentTimeMillis()
                ), userId
            )
        }
    }

    fun deleteSkill(skill: SkillEntity) {
        viewModelScope.launch {
            try {
                repository.delete(skill, userId)
            } catch (e: Exception) {
                _error.emit("Failed to delete skill: ${e.message}")
            }
        }
    }

    fun cycleSkillStatus(skill: SkillEntity) {
        val nextStatus = when (skill.status) {
            "NOT_STARTED" -> "IN_PROGRESS"
            "IN_PROGRESS" -> "PROFICIENT"
            "PROFICIENT" -> "NOT_STARTED"
            else -> "NOT_STARTED"
        }

        val updatedSubtasks = if (skill.subtasks.isNotEmpty()) {
            when (nextStatus) {
                "PROFICIENT" -> skill.subtasks.map { it.copy(status = "PROFICIENT") }
                "NOT_STARTED" -> skill.subtasks.map { it.copy(status = "NOT_STARTED") }
                else -> skill.subtasks
            }
        } else {
            skill.subtasks
        }

        updateSkill(skill.copy(status = nextStatus, subtasks = updatedSubtasks))
    }

    fun addSubtask(skill: SkillEntity, text: String) {
        if (text.isBlank()) return
        val newSubtasks = skill.subtasks + Subtask(text = text.trim())
        updateSkill(skill.copy(subtasks = newSubtasks))
    }

    fun updateSubtaskStatus(skill: SkillEntity, subtaskId: String) {
        val newSubtasks = skill.subtasks.map {
            if (it.id == subtaskId) {
                val nextStatus = when (it.status) {
                    "NOT_STARTED" -> "IN_PROGRESS"
                    "IN_PROGRESS" -> "PROFICIENT"
                    "PROFICIENT" -> "NOT_STARTED"
                    else -> "NOT_STARTED"
                }
                it.copy(status = nextStatus)
            } else it
        }
        updateSkill(skill.copy(subtasks = newSubtasks))
    }

    fun deleteSubtask(skill: SkillEntity, subtaskId: String) {
        val newSubtasks = skill.subtasks.filter { it.id != subtaskId }
        updateSkill(skill.copy(subtasks = newSubtasks))
    }

    fun updateSubtaskText(skill: SkillEntity, subtaskId: String, newText: String) {
        if (newText.isBlank()) return
        val newSubtasks = skill.subtasks.map {
            if (it.id == subtaskId) it.copy(text = newText.trim()) else it
        }
        updateSkill(skill.copy(subtasks = newSubtasks))
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSelectedLanguage(language: String?) {
        _selectedLanguage.value = language
    }
}

class SkillViewModelFactory(
    private val repository: SkillRepository,
    private val settingsRepository: SettingsRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SkillViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SkillViewModel(repository, settingsRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
