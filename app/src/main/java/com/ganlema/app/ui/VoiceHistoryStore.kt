package com.ganlema.app.ui

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

private const val VOICE_HISTORY_FILE = "voice_history.json"

data class VoiceHistoryItem(
    val id: Long,
    val savedAtMillis: Long,
    val startMillis: Long,
    val endMillis: Long,
    val locationLabel: String,
    val amplitudes: List<Float>,
    val climaxSegments: List<Pair<Int, Int>>,
    val peakPercent: Int,
    val peakSecond: String,
    val activePercent: Int,
    val climaxCount: Int,
    val climaxTotalSecond: String,
    val longestClimaxSecond: String,
    val summary: String,
    val maleSummary: String,
    val femaleSummary: String
)

class VoiceHistoryStore(private val context: Context) {
    fun load(): List<VoiceHistoryItem> {
        val file = context.getFileStreamPath(VOICE_HISTORY_FILE)
        if (!file.exists()) return emptyList()
        val text = runCatching { file.readText() }.getOrNull().orEmpty()
        if (text.isBlank()) return emptyList()

        return runCatching {
            val array = JSONArray(text)
            (0 until array.length()).mapNotNull { index ->
                parse(array.optJSONObject(index))
            }
        }.getOrElse { emptyList() }
    }

    fun append(item: VoiceHistoryItem) {
        val updated = (load() + item).sortedByDescending { it.savedAtMillis }.take(120)
        saveAll(updated)
    }

    fun delete(id: Long) {
        saveAll(load().filterNot { it.id == id })
    }

    private fun saveAll(items: List<VoiceHistoryItem>) {
        val array = JSONArray()
        items.forEach { array.put(encode(it)) }
        context.openFileOutput(VOICE_HISTORY_FILE, Context.MODE_PRIVATE).use { out ->
            out.write(array.toString().toByteArray())
        }
    }

    private fun encode(item: VoiceHistoryItem): JSONObject {
        val obj = JSONObject()
        obj.put("id", item.id)
        obj.put("savedAtMillis", item.savedAtMillis)
        obj.put("startMillis", item.startMillis)
        obj.put("endMillis", item.endMillis)
        obj.put("locationLabel", item.locationLabel)
        obj.put("peakPercent", item.peakPercent)
        obj.put("peakSecond", item.peakSecond)
        obj.put("activePercent", item.activePercent)
        obj.put("climaxCount", item.climaxCount)
        obj.put("climaxTotalSecond", item.climaxTotalSecond)
        obj.put("longestClimaxSecond", item.longestClimaxSecond)
        obj.put("summary", item.summary)
        obj.put("maleSummary", item.maleSummary)
        obj.put("femaleSummary", item.femaleSummary)

        val points = JSONArray()
        item.amplitudes.forEach { points.put(it.toDouble()) }
        obj.put("amplitudes", points)

        val segments = JSONArray()
        item.climaxSegments.forEach { (start, end) ->
            val s = JSONObject()
            s.put("start", start)
            s.put("end", end)
            segments.put(s)
        }
        obj.put("climaxSegments", segments)

        return obj
    }

    private fun parse(obj: JSONObject?): VoiceHistoryItem? {
        if (obj == null) return null
        val amplitudesArray = obj.optJSONArray("amplitudes") ?: JSONArray()
        val amplitudes = (0 until amplitudesArray.length()).map { idx ->
            amplitudesArray.optDouble(idx, 0.0).toFloat()
        }

        val segmentsArray = obj.optJSONArray("climaxSegments") ?: JSONArray()
        val segments = (0 until segmentsArray.length()).mapNotNull { idx ->
            val s = segmentsArray.optJSONObject(idx) ?: return@mapNotNull null
            s.optInt("start", -1).takeIf { it >= 0 }?.let { start ->
                start to s.optInt("end", start)
            }
        }

        return VoiceHistoryItem(
            id = obj.optLong("id", 0L),
            savedAtMillis = obj.optLong("savedAtMillis", 0L),
            startMillis = obj.optLong("startMillis", 0L),
            endMillis = obj.optLong("endMillis", 0L),
            locationLabel = obj.optString("locationLabel", "未知位置"),
            amplitudes = amplitudes,
            climaxSegments = segments,
            peakPercent = obj.optInt("peakPercent", 0),
            peakSecond = obj.optString("peakSecond", "0.0"),
            activePercent = obj.optInt("activePercent", 0),
            climaxCount = obj.optInt("climaxCount", 0),
            climaxTotalSecond = obj.optString("climaxTotalSecond", "0.0"),
            longestClimaxSecond = obj.optString("longestClimaxSecond", "0.0"),
            summary = obj.optString("summary", ""),
            maleSummary = obj.optString("maleSummary", ""),
            femaleSummary = obj.optString("femaleSummary", "")
        )
    }
}
