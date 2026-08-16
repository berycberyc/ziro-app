package kz.ziro.app.data

import kotlinx.serialization.Serializable

@Serializable
data class StudentRef(
    val full_name: String,
    val language: String? = null
)

@Serializable
data class Registration(
    val id: String,
    val test_type_id: String,
    val classroom: String? = null,
    val test_variant: String? = null,
    val payment_status: String,
    val students: StudentRef? = null
)
