package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.entity.ActionLogEntity
import com.example.data.entity.ArtifactEntity
import com.example.data.entity.TaskCommentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StolDao {
    @Query("SELECT * FROM stol_artifacts ORDER BY updatedTimestamp DESC")
    fun getAllArtifacts(): Flow<List<ArtifactEntity>>

    @Query("SELECT * FROM stol_artifacts WHERE status = :status ORDER BY updatedTimestamp DESC")
    fun getArtifactsByStatus(status: String): Flow<List<ArtifactEntity>>

    @Query("SELECT * FROM stol_artifacts WHERE id = :id")
    fun getArtifactById(id: Long): Flow<ArtifactEntity?>

    @Query("SELECT COUNT(*) FROM stol_artifacts")
    suspend fun getArtifactsCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertArtifact(artifact: ArtifactEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertArtifacts(artifacts: List<ArtifactEntity>)

    @Update
    suspend fun updateArtifact(artifact: ArtifactEntity)

    @Query("DELETE FROM stol_artifacts WHERE id = :id")
    suspend fun deleteArtifactById(id: Long)

    @Query("SELECT * FROM stol_action_logs ORDER BY timestamp DESC")
    fun getAllActionLogs(): Flow<List<ActionLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertActionLog(log: ActionLogEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertActionLogs(logs: List<ActionLogEntity>)

    @Query("SELECT * FROM stol_task_comments ORDER BY timestamp ASC")
    fun getAllComments(): Flow<List<TaskCommentEntity>>

    @Query("SELECT * FROM stol_task_comments WHERE artifactId = :artifactId ORDER BY timestamp ASC")
    fun getCommentsForArtifact(artifactId: Long): Flow<List<TaskCommentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertComment(comment: TaskCommentEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertComments(comments: List<TaskCommentEntity>)

    @Query("DELETE FROM stol_task_comments WHERE id = :id")
    suspend fun deleteCommentById(id: Long)

    @Query("DELETE FROM stol_task_comments WHERE artifactId = :artifactId")
    suspend fun deleteCommentsByArtifactId(artifactId: Long)
}
