package kz.ziro.app.data

import kotlinx.serialization.Serializable

@Serializable
data class Stage(
    val subject: String,
    val questions: Int,
    val minutes: Int,
    val format: String, // "abcd" or "number"
    val newPage: Boolean = false // admin marks this subject as starting a new sheet page
)

@Serializable
data class TestType(
    val id: String? = null,
    val code: String,
    val name_kk: String,
    val name_ru: String,
    val stages: List<Stage>,
    val scoring_scheme: String
)
