package io.github.langstudy.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import io.github.langstudy.data.local.entity.VocabEntity
import io.github.langstudy.data.repository.VocabRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.math.ceil

class FlashcardViewModel(
    private val repository: VocabRepository
) : ViewModel() {
    private var userId: String? = null
    private val FLASHCARD_TARGET_COUNT = 10

    private val _reviewList = MutableStateFlow<List<VocabEntity>>(emptyList())
    val reviewList: StateFlow<List<VocabEntity>> = _reviewList.asStateFlow()

    private val _currentIndex = MutableStateFlow(0)
    val currentIndex: StateFlow<Int> = _currentIndex.asStateFlow()

    private val _isFlipped = MutableStateFlow(false)
    val isFlipped: StateFlow<Boolean> = _isFlipped.asStateFlow()

    private val _isSessionFinished = MutableStateFlow(false)
    val isSessionFinished: StateFlow<Boolean> = _isSessionFinished.asStateFlow()

    private val _sessionStats = MutableStateFlow(SessionStats())
    val sessionStats: StateFlow<SessionStats> = _sessionStats.asStateFlow()

    data class SessionStats(
        val totalReviewed: Int = 0,
        val notStartedCount: Int = 0,
        val inProgressCount: Int = 0,
        val proficientCount: Int = 0
    )

    fun init(
        userId: String,
        allVocab: List<VocabEntity>? = null,
        categoryFilter: String? = null,
        languageFilter: String? = null
    ) {
        this.userId = userId
        
        if (allVocab != null) {
            setupReview(allVocab, categoryFilter, languageFilter)
        } else {
            viewModelScope.launch {
                val vocab = repository.allVocab.first()
                setupReview(vocab, categoryFilter, languageFilter)
            }
        }
    }

    private fun setupReview(
        vocab: List<VocabEntity>,
        categoryFilter: String?,
        languageFilter: String?
    ) {
        val filtered = vocab.filter {
            (categoryFilter == null || it.category == categoryFilter) &&
                    (languageFilter == null || it.language == languageFilter)
        }
        _reviewList.value = buildReviewList(filtered, FLASHCARD_TARGET_COUNT)
        _currentIndex.value = 0
        _isFlipped.value = false
        _isSessionFinished.value = false
        _sessionStats.value = SessionStats()
    }

    private fun buildReviewList(items: List<VocabEntity>, targetCount: Int): List<VocabEntity> {
        if (items.isEmpty()) return emptyList()

        val selected = mutableListOf<VocabEntity>()
        val selectedIds = mutableSetOf<String>()

        // Fallback logic from flashcard.js
        val remaining = items.filter { !selectedIds.contains(it.id) }
        val notStarted = remaining.filter { it.status == "NOT_STARTED" }.shuffled()
        val inProgress = remaining.filter { it.status == "IN_PROGRESS" }.shuffled()
        val proficient = remaining.filter { it.status == "PROFICIENT" }.shuffled()

        val slotsLeft = targetCount - selected.size
        val targetNotStarted = ceil(slotsLeft * 0.5).toInt()
        val targetInProgress = ceil(slotsLeft * 0.35).toInt()

        fun pick(source: List<VocabEntity>, count: Int) {
            source.take(count).forEach {
                if (!selectedIds.contains(it.id)) {
                    selected.add(it)
                    selectedIds.add(it.id)
                }
            }
        }

        pick(notStarted, targetNotStarted)
        pick(inProgress, targetInProgress)
        pick(proficient, targetCount - selected.size)

        if (selected.size < targetCount) {
            val fallback = items.filter { !selectedIds.contains(it.id) }.shuffled()
            pick(fallback, targetCount - selected.size)
        }

        return selected.shuffled()
    }

    fun nextCard() {
        if (_currentIndex.value < _reviewList.value.size - 1) {
            _currentIndex.value++
            _isFlipped.value = false
        }
    }

    fun prevCard() {
        if (_currentIndex.value > 0) {
            _currentIndex.value--
            _isFlipped.value = false
        }
    }

    fun flipCard() {
        _isFlipped.value = !_isFlipped.value
    }

    fun updateStatus(newStatus: String) {
        val currentItem = _reviewList.value.getOrNull(_currentIndex.value) ?: return
        viewModelScope.launch {
            repository.update(currentItem.copy(status = newStatus), userId)

            // Update local review list item status
            val newList = _reviewList.value.toMutableList()
            newList[_currentIndex.value] = currentItem.copy(status = newStatus)
            _reviewList.value = newList

            // Update stats
            _sessionStats.value = _sessionStats.value.let { stats ->
                stats.copy(
                    totalReviewed = stats.totalReviewed + 1,
                    notStartedCount = stats.notStartedCount + if (newStatus == "NOT_STARTED") 1 else 0,
                    inProgressCount = stats.inProgressCount + if (newStatus == "IN_PROGRESS") 1 else 0,
                    proficientCount = stats.proficientCount + if (newStatus == "PROFICIENT") 1 else 0
                )
            }

            // Automatically move to next card or finish session
            if (_currentIndex.value < _reviewList.value.size - 1) {
                nextCard()
            } else {
                _isSessionFinished.value = true
            }
        }
    }
}

class FlashcardViewModelFactory(
    private val repository: VocabRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(FlashcardViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return FlashcardViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
