package com.example.data.db

import androidx.room.*
import com.example.data.models.ChatMessageEntity
import com.example.data.models.ConnaissancePersonnelleEntity
import com.example.data.models.DiagnosticEntity
import com.example.data.models.ProblemeSuiviEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DiagnosticDao {
    @Query("SELECT * FROM diagnostics ORDER BY timestamp DESC")
    fun getAllDiagnostics(): Flow<List<DiagnosticEntity>>

    @Query("SELECT * FROM diagnostics WHERE id = :id")
    fun getDiagnosticById(id: Long): Flow<DiagnosticEntity?>

    @Query("SELECT * FROM diagnostics WHERE id = :id")
    suspend fun getDiagnosticByIdSync(id: Long): DiagnosticEntity?

    @Query("SELECT * FROM diagnostics WHERE problemeSuiviId = :suiviId ORDER BY timestamp ASC")
    fun getDiagnosticsForSuivi(suiviId: Long): Flow<List<DiagnosticEntity>>

    @Query("SELECT * FROM diagnostics WHERE isPendingAnalysis = 1 ORDER BY timestamp ASC")
    suspend fun getPendingDiagnosticsSync(): List<DiagnosticEntity>

    @Query("SELECT * FROM diagnostics WHERE isResolved = 0 AND urgence IN ('moyen', 'eleve', 'critique') ORDER BY timestamp DESC")
    fun getActiveEmergencies(): Flow<List<DiagnosticEntity>>

    @Query("SELECT * FROM diagnostics ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentDiagnostics(limit: Int = 5): Flow<List<DiagnosticEntity>>

    @Query("SELECT COUNT(*) FROM diagnostics")
    fun getDiagnosticCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDiagnostic(entity: DiagnosticEntity): Long

    @Update
    suspend fun updateDiagnostic(entity: DiagnosticEntity)

    @Query("DELETE FROM diagnostics WHERE id = :id")
    suspend fun deleteDiagnosticById(id: Long)

    @Query("DELETE FROM diagnostics")
    suspend fun clearAllDiagnostics()

    // --- Probleme Suivi ---
    @Query("SELECT * FROM problemes_suivis ORDER BY derniereMiseAJour DESC")
    fun getAllProblemesSuivis(): Flow<List<ProblemeSuiviEntity>>

    @Query("SELECT * FROM problemes_suivis WHERE statut != 'RESOLU' ORDER BY derniereMiseAJour DESC")
    fun getActiveProblemesSuivis(): Flow<List<ProblemeSuiviEntity>>

    @Query("SELECT * FROM problemes_suivis WHERE id = :id")
    fun getProblemeSuiviById(id: Long): Flow<ProblemeSuiviEntity?>

    @Query("SELECT * FROM problemes_suivis WHERE id = :id")
    suspend fun getProblemeSuiviByIdSync(id: Long): ProblemeSuiviEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProblemeSuivi(entity: ProblemeSuiviEntity): Long

    @Update
    suspend fun updateProblemeSuivi(entity: ProblemeSuiviEntity)

    @Query("DELETE FROM problemes_suivis WHERE id = :id")
    suspend fun deleteProblemeSuivi(id: Long)

    // --- Chat Messages ---
    @Query("SELECT * FROM chat_messages WHERE diagnosticId = :diagnosticId ORDER BY timestamp ASC")
    fun getChatMessagesForDiagnostic(diagnosticId: Long): Flow<List<ChatMessageEntity>>

    @Query("SELECT * FROM chat_messages WHERE diagnosticId = :diagnosticId ORDER BY timestamp ASC")
    suspend fun getChatMessagesForDiagnosticSync(diagnosticId: Long): List<ChatMessageEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChatMessage(message: ChatMessageEntity): Long

    // --- Connaissances Personnelles ---
    @Query("SELECT * FROM connaissances_personnelles ORDER BY timestamp DESC")
    fun getAllConnaissances(): Flow<List<ConnaissancePersonnelleEntity>>

    @Query("SELECT * FROM connaissances_personnelles ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentConnaissancesSync(limit: Int = 5): List<ConnaissancePersonnelleEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConnaissance(connaissance: ConnaissancePersonnelleEntity): Long
}

