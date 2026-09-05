package com.example.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Reprezentuje komentarz lub wpis w dyskusji agentów nad danym zadaniem / artefaktem na stole.
 */
@Entity(
    tableName = "stol_task_comments",
    indices = [Index(value = ["artifactId"])]
)
data class TaskCommentEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val artifactId: Long,
    val authorName: String,
    val authorRole: String = "Agent Katedry",
    val commentType: String = "DISCUSSION", // "DISCUSSION", "FEEDBACK", "SUGGESTION", "CRITIQUE", "APPROVAL"
    val content: String,
    val timestamp: Long = System.currentTimeMillis()
)
