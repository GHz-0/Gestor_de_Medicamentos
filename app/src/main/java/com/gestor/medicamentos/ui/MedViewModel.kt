package com.gestor.medicamentos.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.gestor.medicamentos.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class MedViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = MedRepository(app)

    /** null = cargando, vacío = sin perfil (se muestra la pantalla de personalización). */
    val profiles: StateFlow<List<Profile>?> = repo.profiles.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val allMeds = repo.meds.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    /** Activos ordenados por la siguiente toma (la más próxima primero). */
    val active = allMeds.map { l -> l.filter { it.status == Status.ACTIVE }.sortedBy { it.nextDoseMillis ?: Long.MAX_VALUE } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val past = allMeds.map { l -> l.filter { it.status != Status.ACTIVE }.sortedByDescending { it.id } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val medFilter = MutableStateFlow<Long?>(null)
    val daysFilter = MutableStateFlow(0) // 0 = todo

    val logs = combine(repo.logs, medFilter, daysFilter) { list, med, days ->
        val from = if (days == 0) 0L else System.currentTimeMillis() - days * 86_400_000L
        list.filter { (med == null || it.medId == med) && it.timeMillis >= from }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun saveProfile(p: Profile) = viewModelScope.launch { repo.saveProfile(p) }
    fun saveMed(m: Medication) = viewModelScope.launch { repo.save(m) }
    fun take(id: Long) = viewModelScope.launch { repo.take(id) }
    fun delete(id: Long) = viewModelScope.launch { repo.delete(id) }
    fun restore(id: Long) = viewModelScope.launch { repo.restore(id) }
    fun refill(id: Long, n: Int) = viewModelScope.launch { repo.refill(id, n) }
}
