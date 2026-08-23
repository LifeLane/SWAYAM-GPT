package com.example.edgeaicore.core.memory

import androidx.room.*
import com.example.edgeaicore.core.common.PrivacyLevel
import kotlinx.coroutines.flow.Flow

enum class MemoryType {
    NOTE,
    IMAGE,
    DOCUMENT,
    VOICE,
    PERSON,
    PLACE,
    TASK,
    EVENT,
    OBSERVATION,
    PREFERENCE,
    GOAL
}

@Entity(tableName = "memories")
data class MemoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: MemoryType = MemoryType.NOTE,
    val title: String,
    val summary: String,
    val content: String,
    val source: String = "User Input",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val location: String? = null,
    val tags: String = "",
    val privacyLevel: PrivacyLevel = PrivacyLevel.LOCAL_ONLY,
    val confidence: Float = 1.0f,
    val embeddingReference: String? = null, // serialized vector array
    val mediaReference: String? = null,
    val isArchived: Boolean = false,
    val isFavorite: Boolean = false
)

class MemoryTypeConverters {
    @TypeConverter
    fun fromMemoryType(type: MemoryType): String = type.name

    @TypeConverter
    fun toMemoryType(value: String): MemoryType = try {
        MemoryType.valueOf(value)
    } catch (e: Exception) {
        MemoryType.NOTE
    }

    @TypeConverter
    fun fromPrivacyLevel(level: PrivacyLevel): String = level.name

    @TypeConverter
    fun toPrivacyLevel(value: String): PrivacyLevel = try {
        PrivacyLevel.valueOf(value)
    } catch (e: Exception) {
        PrivacyLevel.LOCAL_ONLY
    }
}

@Dao
interface MemoryDao {
    @Query("SELECT * FROM memories WHERE isArchived = 0 ORDER BY createdAt DESC")
    fun getAllActiveMemories(): Flow<List<MemoryEntity>>

    @Query("SELECT * FROM memories WHERE id = :id LIMIT 1")
    suspend fun getMemoryById(id: Long): MemoryEntity?

    @Query("SELECT * FROM memories WHERE isArchived = 0 AND (title LIKE '%' || :query || '%' OR content LIKE '%' || :query || '%' OR tags LIKE '%' || :query || '%') ORDER BY createdAt DESC")
    fun searchMemories(query: String): Flow<List<MemoryEntity>>

    @Query("SELECT * FROM memories WHERE isArchived = 0 AND type = :type ORDER BY createdAt DESC")
    fun getMemoriesByType(type: MemoryType): Flow<List<MemoryEntity>>

    @Query("SELECT * FROM memories WHERE isArchived = 0 AND isFavorite = 1 ORDER BY createdAt DESC")
    fun getFavoriteMemories(): Flow<List<MemoryEntity>>

    @Query("SELECT * FROM memories WHERE isArchived = 0")
    suspend fun getAllActiveMemoriesSync(): List<MemoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMemory(memory: MemoryEntity): Long

    @Update
    suspend fun updateMemory(memory: MemoryEntity)

    @Delete
    suspend fun deleteMemory(memory: MemoryEntity)

    @Query("DELETE FROM memories")
    suspend fun deleteAllMemories()

    @Query("SELECT COUNT(*) FROM memories")
    fun getCount(): Flow<Int>
}

@Database(entities = [MemoryEntity::class], version = 1, exportSchema = false)
@TypeConverters(MemoryTypeConverters::class)
abstract class EdgeMemoryDatabase : RoomDatabase() {
    abstract fun memoryDao(): MemoryDao
}
