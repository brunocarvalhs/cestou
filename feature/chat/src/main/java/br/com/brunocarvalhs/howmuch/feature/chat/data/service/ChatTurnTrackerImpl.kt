package br.com.brunocarvalhs.howmuch.feature.chat.data.service

import br.com.brunocarvalhs.howmuch.feature.chat.domain.service.ChatTurnTracker
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class ChatTurnTrackerImpl @Inject constructor() : ChatTurnTracker {

    private val jobs = ConcurrentHashMap<String, Job>()
    private val _inFlight = MutableStateFlow<Set<String>>(emptySet())
    override val inFlight: StateFlow<Set<String>> = _inFlight.asStateFlow()

    override fun begin(key: String, job: Job) {
        jobs[key] = job
        _inFlight.update { it + key }
    }

    override fun end(key: String) {
        jobs.remove(key)
        _inFlight.update { it - key }
    }

    override fun cancelAll() {
        val cancelled = jobs.values.toList()
        jobs.clear()
        _inFlight.value = emptySet()
        cancelled.forEach { it.cancel() }
    }
}
