package com.example.menstruation.data.model

import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

enum class Symptom(val label: String, val category: SymptomCategory) {
    // 全身
    FATIGUE("疲劳", SymptomCategory.BODY_GENERAL),
    FEVER("发热", SymptomCategory.BODY_GENERAL),
    CHILLS("发冷", SymptomCategory.BODY_GENERAL),
    INSOMNIA("失眠", SymptomCategory.BODY_GENERAL),
    SLEEPINESS("嗜睡", SymptomCategory.BODY_GENERAL),

    // 头部
    HEADACHE("头痛", SymptomCategory.HEAD),
    DIZZINESS("头晕", SymptomCategory.HEAD),

    // 腹部
    BLOATING("腹胀", SymptomCategory.ABDOMEN),
    DIARRHEA("腹泻", SymptomCategory.ABDOMEN),
    CONSTIPATION("便秘", SymptomCategory.ABDOMEN),
    STOMACHACHE("胃疼", SymptomCategory.ABDOMEN),
    NAUSEA("恶心", SymptomCategory.ABDOMEN),

    // 皮肤
    ACNE("痤疮", SymptomCategory.SKIN),
    RASH("皮疹", SymptomCategory.SKIN),
    DRY_SKIN("干燥", SymptomCategory.SKIN),

    // 私处分泌物
    DISCHARGE_WHITE("白色分泌物", SymptomCategory.DISCHARGE),
    DISCHARGE_YELLOW("黄色分泌物", SymptomCategory.DISCHARGE),
    DISCHARGE_BROWN("褐色分泌物", SymptomCategory.DISCHARGE),
    DISCHARGE_CLEAR("透明分泌物", SymptomCategory.DISCHARGE),

    // 其他
    BREAST_TENDERNESS("乳房胀痛", SymptomCategory.OTHER),
    BACK_PAIN("腰痛", SymptomCategory.OTHER),
    JOINT_PAIN("关节痛", SymptomCategory.OTHER),
    CRAMPS("痛经", SymptomCategory.OTHER);

    companion object {
        private val json = Json {
            ignoreUnknownKeys = true
        }

        fun toJson(symptoms: List<Symptom>): String {
            return json.encodeToString(
                ListSerializer(String.serializer()),
                symptoms.map { it.name }
            )
        }

        fun fromJson(jsonString: String): List<Symptom> {
            return try {
                val names = json.decodeFromString(
                    ListSerializer(String.serializer()),
                    jsonString
                )
                names.mapNotNull { name ->
                    try {
                        valueOf(name)
                    } catch (e: IllegalArgumentException) {
                        null // Skip unknown symptoms
                    }
                }
            } catch (e: Exception) {
                emptyList()
            }
        }
    }
}

enum class SymptomCategory(val label: String) {
    BODY_GENERAL("全身"),
    HEAD("头部"),
    ABDOMEN("腹部"),
    SKIN("皮肤"),
    DISCHARGE("私处分泌物"),
    OTHER("其他")
}
