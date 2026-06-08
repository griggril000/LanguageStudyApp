package com.example.languagestudy.data.model

import com.google.firebase.firestore.DocumentId

data class PortfolioItem(
    @DocumentId val id: String = "",
    val title: String = "",
    val link: String = "",
    val isFeatured: Boolean = false,
    val timestamp: Long = 0L,
    val type: String = "youtube" // youtube, soundcloud, etc.
)
