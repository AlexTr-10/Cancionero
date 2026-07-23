package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.Song
import kotlinx.coroutines.flow.Flow

@Dao
interface SongDao {
    @Query("SELECT * FROM songs ORDER BY displayOrder ASC, id ASC")
    fun getAllSongs(): Flow<List<Song>>

    @Query("SELECT * FROM songs WHERE isFavorite = 1 ORDER BY displayOrder ASC, id ASC")
    fun getFavoriteSongs(): Flow<List<Song>>

    @Query("SELECT * FROM songs WHERE id = :id")
    suspend fun getSongById(id: Long): Song?

    @Query("SELECT * FROM songs WHERE id = :id")
    fun getSongByIdFlow(id: Long): Flow<Song?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSong(song: Song): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSongs(songs: List<Song>)

    @Update
    suspend fun updateSong(song: Song)

    @Update
    suspend fun updateSongs(songs: List<Song>)

    @Delete
    suspend fun deleteSong(song: Song)

    @Query("UPDATE songs SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun updateFavoriteStatus(id: Long, isFavorite: Boolean)

    @Query("UPDATE songs SET category = :category WHERE id IN (:ids)")
    suspend fun updateBatchCategory(ids: List<Long>, category: String)

    @Query("UPDATE songs SET `key` = :key WHERE id IN (:ids)")
    suspend fun updateBatchKey(ids: List<Long>, key: String)

    @Query("DELETE FROM songs WHERE id IN (:ids)")
    suspend fun deleteBatchSongs(ids: List<Long>)

    @Query("SELECT * FROM songs WHERE title = :title LIMIT 1")
    suspend fun getSongByTitle(title: String): Song?
}
