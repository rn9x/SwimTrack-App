package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.data.local.entities.CompetitionEntity
import com.example.data.local.entities.CompetitionResultEntity
import com.example.data.local.entities.SwimRecordEntity
import com.example.domain.model.PoolLength
import com.example.domain.model.ResultStatus
import com.example.domain.model.Stroke
import com.example.domain.model.SwimDistance
import kotlinx.coroutines.flow.Flow

@Dao
interface SwimTrackDao {

    // --- Swim Records ---
    @Query("SELECT * FROM swim_records ORDER BY date DESC, createdAt DESC")
    fun observeAllSwimRecords(): Flow<List<SwimRecordEntity>>

    @Query("SELECT * FROM swim_records ORDER BY date DESC, createdAt DESC")
    suspend fun getAllSwimRecordsOnce(): List<SwimRecordEntity>

    @Query("SELECT * FROM swim_records WHERE id = :id LIMIT 1")
    suspend fun getSwimRecordById(id: Long): SwimRecordEntity?

    @Query(
        """
        SELECT * FROM swim_records 
        WHERE distance = :distance AND stroke = :stroke 
        ORDER BY date ASC, createdAt ASC
        """
    )
    fun observeRecordsForEvent(distance: SwimDistance, stroke: Stroke): Flow<List<SwimRecordEntity>>

    @Query(
        """
        SELECT MIN(time) FROM swim_records 
        WHERE distance = :distance 
          AND stroke = :stroke 
          AND status = :finishedStatus 
          AND time > 0
        """
    )
    suspend fun getBestTimeForEvent(
        distance: SwimDistance,
        stroke: Stroke,
        finishedStatus: ResultStatus = ResultStatus.FINISHED
    ): Long?

    @Query(
        """
        SELECT MIN(time) FROM swim_records 
        WHERE distance = :distance 
          AND stroke = :stroke 
          AND poolLength = :poolLength 
          AND status = :finishedStatus 
          AND time > 0
        """
    )
    suspend fun getBestTimeForEventAndPool(
        distance: SwimDistance,
        stroke: Stroke,
        poolLength: PoolLength,
        finishedStatus: ResultStatus = ResultStatus.FINISHED
    ): Long?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSwimRecord(record: SwimRecordEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSwimRecords(records: List<SwimRecordEntity>)

    @Update
    suspend fun updateSwimRecord(record: SwimRecordEntity)

    @Query("DELETE FROM swim_records WHERE id = :id")
    suspend fun deleteSwimRecordById(id: Long)

    // --- Competitions ---
    @Query("SELECT * FROM competitions ORDER BY date DESC, id DESC")
    fun observeAllCompetitions(): Flow<List<CompetitionEntity>>

    @Query("SELECT * FROM competitions ORDER BY date DESC, id DESC")
    suspend fun getAllCompetitionsOnce(): List<CompetitionEntity>

    @Query("SELECT * FROM competitions WHERE id = :id LIMIT 1")
    suspend fun getCompetitionById(id: Long): CompetitionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCompetition(competition: CompetitionEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCompetitions(competitions: List<CompetitionEntity>)

    @Update
    suspend fun updateCompetition(competition: CompetitionEntity)

    @Query("DELETE FROM competitions WHERE id = :id")
    suspend fun deleteCompetitionById(id: Long)

    // --- Competition Results ---
    @Query("SELECT * FROM competition_results ORDER BY id ASC")
    fun observeAllCompetitionResults(): Flow<List<CompetitionResultEntity>>

    @Query("SELECT * FROM competition_results ORDER BY id ASC")
    suspend fun getAllCompetitionResultsOnce(): List<CompetitionResultEntity>

    @Query("SELECT * FROM competition_results WHERE competitionId = :competitionId ORDER BY id ASC")
    fun observeResultsForCompetition(competitionId: Long): Flow<List<CompetitionResultEntity>>

    @Query("SELECT * FROM competition_results WHERE swimRecordId = :swimRecordId LIMIT 1")
    suspend fun getCompetitionResultBySwimRecordId(swimRecordId: Long): CompetitionResultEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCompetitionResult(result: CompetitionResultEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCompetitionResults(results: List<CompetitionResultEntity>)

    @Update
    suspend fun updateCompetitionResult(result: CompetitionResultEntity)

    @Query("DELETE FROM competition_results WHERE id = :id")
    suspend fun deleteCompetitionResultById(id: Long)

    @Query("DELETE FROM competition_results WHERE swimRecordId = :swimRecordId")
    suspend fun deleteCompetitionResultBySwimRecordId(swimRecordId: Long)

    @Query("DELETE FROM competition_results WHERE competitionId = :competitionId")
    suspend fun deleteResultsForCompetition(competitionId: Long)

    @Query("DELETE FROM swim_records WHERE competitionId = :competitionId")
    suspend fun deleteSwimRecordsForCompetition(competitionId: Long)

    // --- Clear All Data ---
    @Query("DELETE FROM competition_results")
    suspend fun clearAllCompetitionResults()

    @Query("DELETE FROM swim_records")
    suspend fun clearAllSwimRecords()

    @Query("DELETE FROM competitions")
    suspend fun clearAllCompetitions()

    @Transaction
    suspend fun clearEntireDatabase() {
        clearAllCompetitionResults()
        clearAllSwimRecords()
        clearAllCompetitions()
    }
}
