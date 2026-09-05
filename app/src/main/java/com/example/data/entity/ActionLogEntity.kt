package com.example.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Rejestr historii działań agentów przy stole Katedry OtakOS.
 */
@Entity(tableName = "stol_action_logs")
data class ActionLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val artifactId: Long? = null,
    val artifactTitle: String? = null,
    val agentName: String,
    val actionType: String, // "POŁOŻENIE_NA_STOLE", "ITERACJA_AGENTÓW", "MODYFIKACJA_KODU", "ZGŁOSZENIE_DO_AKCEPTACJI", "RATYFIKACJA_KATEDRY", "ZWROT_DO_POPRAWY"
    val description: String,
    val timestamp: Long = System.currentTimeMillis()
)
