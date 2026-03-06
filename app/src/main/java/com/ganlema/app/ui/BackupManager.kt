package com.ganlema.app.ui

import android.content.Context
import android.net.Uri
import com.ganlema.app.data.RecordEntity
import com.ganlema.app.data.RecordRepository
import com.ganlema.app.security.PinManager
import com.ganlema.app.security.PinSnapshot
import org.json.JSONArray
import org.json.JSONObject

class BackupManager(
    private val context: Context,
    private val repository: RecordRepository,
    private val cycleSettingsManager: CycleSettingsManager,
    private val pinManager: PinManager
) {
    suspend fun exportTo(uri: Uri): BackupSummary {
        val records = repository.getAll()
        val payload = BackupPayload(
            version = BACKUP_VERSION,
            exportedAtMillis = System.currentTimeMillis(),
            records = records,
            cycleSettings = cycleSettingsManager.exportSnapshot(),
            pin = pinManager.exportSnapshot()
        )
        context.contentResolver.openOutputStream(uri)?.bufferedWriter(Charsets.UTF_8).use { writer ->
            requireNotNull(writer) { "无法创建导出文件" }
            writer.write(payload.toJson().toString())
            writer.flush()
        }
        return BackupSummary(
            recordCount = records.size,
            cycleConfigured = payload.cycleSettings.configured,
            pinConfigured = !payload.pin.hashBase64.isNullOrBlank()
        )
    }

    suspend fun importFrom(uri: Uri): BackupSummary {
        val content = context.contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8).use { reader ->
            requireNotNull(reader) { "无法读取备份文件" }
            reader.readText()
        }
        val payload = BackupPayload.fromJson(JSONObject(content))
        repository.clearAll()
        payload.records.forEach { repository.add(it) }
        cycleSettingsManager.restoreSnapshot(payload.cycleSettings)
        pinManager.restoreSnapshot(payload.pin)
        return BackupSummary(
            recordCount = payload.records.size,
            cycleConfigured = payload.cycleSettings.configured,
            pinConfigured = !payload.pin.hashBase64.isNullOrBlank()
        )
    }

    companion object {
        private const val BACKUP_VERSION = 1
    }
}

data class BackupSummary(
    val recordCount: Int,
    val cycleConfigured: Boolean,
    val pinConfigured: Boolean
)

private data class BackupPayload(
    val version: Int,
    val exportedAtMillis: Long,
    val records: List<RecordEntity>,
    val cycleSettings: CycleSettingsSnapshot,
    val pin: PinSnapshot
) {
    fun toJson(): JSONObject {
        return JSONObject()
            .put("version", version)
            .put("exportedAtMillis", exportedAtMillis)
            .put("records", JSONArray().apply {
                records.forEach { record ->
                    put(
                        JSONObject()
                            .put("id", record.id)
                            .put("type", record.type)
                            .put("protections", record.protections)
                            .put("otherProtection", record.otherProtection)
                            .put("timeMillis", record.timeMillis)
                            .put("note", record.note)
                    )
                }
            })
            .put(
                "cycleSettings",
                JSONObject()
                    .put("configured", cycleSettings.configured)
                    .put("cycleLengthDays", cycleSettings.cycleLengthDays)
                    .put("periodLengthDays", cycleSettings.periodLengthDays)
                    .put("lastPeriodStartMillis", cycleSettings.lastPeriodStartMillis)
            )
            .put(
                "pin",
                JSONObject()
                    .put("hashBase64", pin.hashBase64)
                    .put("saltBase64", pin.saltBase64)
                    .put("failCount", pin.failCount)
                    .put("hidden", pin.hidden)
                    .put("biometricEnabled", pin.biometricEnabled)
            )
    }

    companion object {
        fun fromJson(json: JSONObject): BackupPayload {
            val version = json.optInt("version", 0)
            require(version in 1..1) { "不支持的备份版本：$version" }

            val recordsArray = json.optJSONArray("records") ?: JSONArray()
            val records = buildList(recordsArray.length()) {
                for (index in 0 until recordsArray.length()) {
                    val item = recordsArray.getJSONObject(index)
                    add(
                        RecordEntity(
                            id = item.optLong("id", 0L),
                            type = item.optString("type"),
                            protections = item.optString("protections"),
                            otherProtection = item.optNullableString("otherProtection"),
                            timeMillis = item.optLong("timeMillis", 0L),
                            note = item.optNullableString("note")
                        )
                    )
                }
            }

            val cycleJson = json.optJSONObject("cycleSettings") ?: JSONObject()
            val pinJson = json.optJSONObject("pin") ?: JSONObject()

            return BackupPayload(
                version = version,
                exportedAtMillis = json.optLong("exportedAtMillis", System.currentTimeMillis()),
                records = records,
                cycleSettings = CycleSettingsSnapshot(
                    configured = cycleJson.optBoolean("configured", false),
                    cycleLengthDays = cycleJson.optInt("cycleLengthDays", 28),
                    periodLengthDays = cycleJson.optInt("periodLengthDays", 5),
                    lastPeriodStartMillis = cycleJson.optLong("lastPeriodStartMillis", 0L)
                ),
                pin = PinSnapshot(
                    hashBase64 = pinJson.optNullableString("hashBase64"),
                    saltBase64 = pinJson.optNullableString("saltBase64"),
                    failCount = pinJson.optInt("failCount", 0),
                    hidden = pinJson.optBoolean("hidden", false),
                    biometricEnabled = pinJson.optBoolean("biometricEnabled", false)
                )
            )
        }
    }
}

private fun JSONObject.optNullableString(key: String): String? {
    if (isNull(key)) return null
    return optString(key).takeIf { it.isNotBlank() && it != "null" }
}
