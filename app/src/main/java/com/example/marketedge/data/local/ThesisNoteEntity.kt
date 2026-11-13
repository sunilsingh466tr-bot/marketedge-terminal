package com.example.marketedge.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "thesis_notes")
data class ThesisNoteEntity(
    @PrimaryKey val symbol: String,
    val companyName: String,
    val thesisText: String,
    val catalysts: String = "",
    val targetValuation: String = "",
    val riskFactors: String = "",
    val updatedAt: Long = System.currentTimeMillis()
)
