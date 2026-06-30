package io.github.langstudy.data.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.PropertyName

data class PortfolioItem(
    @DocumentId
    var id: String = "",

    var title: String = "",

    var link: String = "",

    @get:PropertyName("isTop")
    @set:PropertyName("isTop")
    var isTop: Boolean = false,

    @get:PropertyName("isPrivate")
    @set:PropertyName("isPrivate")
    var isPrivate: Boolean = false,

    var type: String = "youtube",

    var videoId: String? = null,

    var language: String = "",

    var dateAdded: com.google.firebase.Timestamp? = null
)
