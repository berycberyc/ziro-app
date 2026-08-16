package kz.ziro.app.data

import kotlinx.serialization.Serializable

@Serializable
data class Profile(
    val id: String,
    val full_name: String? = null,
    val role: String = "parent"
)
