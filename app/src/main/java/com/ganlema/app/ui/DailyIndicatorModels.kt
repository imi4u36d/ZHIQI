package com.ganlema.app.ui

import androidx.compose.ui.graphics.Color

data class IndicatorOption(
    val value: String,
    val label: String
)

fun metricTitle(metricKey: String): String = when (metricKey) {
    "爱爱" -> "爱爱"
    "症状" -> "症状"
    "心情" -> "心情"
    "白带" -> "白带"
    "体温" -> "体温"
    "体重" -> "体重"
    "日记" -> "日记"
    "好习惯" -> "好习惯"
    "便便" -> "便便"
    "计划" -> "计划"
    else -> metricKey
}

fun metricAccent(metricKey: String): Color = when (metricKey) {
    "爱爱" -> Color(0xFFFF6EA8)
    "症状" -> Color(0xFF6BCBFF)
    "心情" -> Color(0xFFF6C453)
    "白带" -> Color(0xFFB56AF4)
    "体温" -> Color(0xFFA26BFF)
    "体重" -> Color(0xFFC27BFF)
    "日记" -> Color(0xFFF2BD4B)
    "好习惯" -> Color(0xFF6BCBFF)
    "便便" -> Color(0xFFF5C75D)
    "计划" -> Color(0xFF66C8F4)
    else -> Color(0xFFD66A9A)
}

fun metricOptions(metricKey: String): List<IndicatorOption> = when (metricKey) {
    "症状" -> listOf(
        IndicatorOption("none", "无"),
        IndicatorOption("bloating", "腹胀"),
        IndicatorOption("fatigue", "乏力"),
        IndicatorOption("backache", "腰酸"),
        IndicatorOption("breast", "胸胀"),
        IndicatorOption("acne", "痘痘")
    )
    "心情" -> listOf(
        IndicatorOption("happy", "开心"),
        IndicatorOption("calm", "平静"),
        IndicatorOption("sensitive", "敏感"),
        IndicatorOption("irritable", "烦躁"),
        IndicatorOption("sad", "难过")
    )
    "白带" -> listOf(
        IndicatorOption("clear", "透明"),
        IndicatorOption("stretchy", "拉丝"),
        IndicatorOption("milky", "乳白"),
        IndicatorOption("yellow", "偏黄"),
        IndicatorOption("odor", "异味")
    )
    "体温" -> listOf(
        IndicatorOption("low", "偏低"),
        IndicatorOption("normal", "正常"),
        IndicatorOption("high", "偏高")
    )
    "体重" -> listOf(
        IndicatorOption("stable", "稳定"),
        IndicatorOption("up", "略增"),
        IndicatorOption("down", "略降"),
        IndicatorOption("fluctuate", "波动")
    )
    "日记" -> listOf(
        IndicatorOption("body", "身体"),
        IndicatorOption("work", "工作"),
        IndicatorOption("emotion", "情绪"),
        IndicatorOption("relation", "关系"),
        IndicatorOption("other", "其他")
    )
    "好习惯" -> listOf(
        IndicatorOption("sleep", "早睡"),
        IndicatorOption("water", "喝水"),
        IndicatorOption("sport", "运动"),
        IndicatorOption("stretch", "拉伸"),
        IndicatorOption("vitamin", "维生素")
    )
    "便便" -> listOf(
        IndicatorOption("normal", "正常"),
        IndicatorOption("dry", "偏干"),
        IndicatorOption("loose", "偏稀"),
        IndicatorOption("hard", "困难"),
        IndicatorOption("many", "多次")
    )
    "计划" -> listOf(
        IndicatorOption("rest", "休息"),
        IndicatorOption("exercise", "运动"),
        IndicatorOption("date", "约会"),
        IndicatorOption("check", "检查"),
        IndicatorOption("medicine", "买药")
    )
    else -> emptyList()
}
