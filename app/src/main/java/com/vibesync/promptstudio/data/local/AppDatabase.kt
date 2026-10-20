package com.vibesync.promptstudio.data.local

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "prompt_templates")
data class PromptTemplateRow(
    @PrimaryKey val id: String,
    val title: String,
    val systemPrompt: String,
    val userPrompt: String,
    val variablesJson: String,
    val updatedAt: Long
)

@Entity(tableName = "run_history")
data class RunHistoryRow(
    @PrimaryKey val id: String,
    val provider: String,
    val modelId: String,
    val promptTitle: String,
    val inputText: String,
    val responseText: String,
    val createdAt: Long
)

@Entity(tableName = "variable_sets")
data class VariableSetRow(
    @PrimaryKey val id: String,
    val name: String,
    val valuesJson: String,
    val updatedAt: Long
)

@Dao
interface PromptDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPrompt(row: PromptTemplateRow)

    @Query("SELECT * FROM prompt_templates ORDER BY updatedAt DESC")
    fun observePrompts(): Flow<List<PromptTemplateRow>>

    @Query("SELECT * FROM prompt_templates WHERE id = :id LIMIT 1")
    suspend fun findPrompt(id: String): PromptTemplateRow?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRun(row: RunHistoryRow)

    @Query("SELECT * FROM run_history ORDER BY createdAt DESC LIMIT 50")
    fun observeRecentRuns(): Flow<List<RunHistoryRow>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertVariableSet(row: VariableSetRow)

    @Query("SELECT * FROM variable_sets ORDER BY updatedAt DESC")
    fun observeVariableSets(): Flow<List<VariableSetRow>>
}

@Database(
    entities = [PromptTemplateRow::class, RunHistoryRow::class, VariableSetRow::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun promptDao(): PromptDao
}
