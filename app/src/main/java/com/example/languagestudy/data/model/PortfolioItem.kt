package com.example.languagestudy.data.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.PropertyName

data class PortfolioItem(
    @DocumentId 
    var id: String = "",
    
    val title: String = "",
    
    val link: String = "",
    
    @get:PropertyName("isTop")
    @set:PropertyName("isTop")
    @get:JvmName("isTop")
    var isTop: Boolean = false,
    
    @get:PropertyName("isPrivate")
    @set:PropertyName("isPrivate")
    @get:JvmName("isPrivate")
    var isPrivate: Boolean = false,
    
    val type: String = "youtube",
    
    val videoId: String? = null,
    
    val language: String = "",

    val dateAdded: com.google.firebase.Timestamp? = null
)
