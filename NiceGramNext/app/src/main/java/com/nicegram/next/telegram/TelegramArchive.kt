package com.nicegram.next.telegram

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Entity(
    tableName = "archived_messages",
    primaryKeys = ["chatId", "messageId"]
)
data class ArchivedMessageEntity(
    val chatId: Long,
    val messageId: Long,
    val chatTitle: String,
    val sender: String,
    val encryptedText: String,
    val timestamp: Long,
    val deleted: Boolean
)

data class ArchivedMessage(
    val chatId: Long,
    val messageId: Long,
    val chatTitle: String,
    val sender: String,
    val text: String,
    val timestamp: Long
)

@Dao
interface TelegramArchiveDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(message: ArchivedMessageEntity)

    @Query("UPDATE archived_messages SET deleted = 1 WHERE chatId = :chatId AND messageId IN (:messageIds)")
    suspend fun markDeleted(chatId: Long, messageIds: List<Long>)

    @Query("SELECT * FROM archived_messages WHERE deleted = 1 ORDER BY timestamp DESC")
    fun observeDeleted(): Flow<List<ArchivedMessageEntity>>
}

@Database(entities = [ArchivedMessageEntity::class], version = 1, exportSchema = false)
abstract class TelegramArchiveDatabase : RoomDatabase() {
    abstract fun archiveDao(): TelegramArchiveDao
}
