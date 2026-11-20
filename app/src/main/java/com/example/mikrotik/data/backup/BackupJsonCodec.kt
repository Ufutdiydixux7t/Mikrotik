package com.example.mikrotik.data.backup

import java.time.LocalDate

/** Pure Kotlin (no Android dependencies) schema and JSON conversion for manual backups. */
data class BackupVoucher(
    val code: String,
    val durationMinutes: Long,
    val dataLimitBytes: Long,
    val boundMacAddress: String?,
    val isUsed: Boolean,
    val isBlocked: Boolean,
    val createdAt: Long,
    val firstUsedAt: Long?,
    val bytesUsedSoFar: Long,
    val packageType: String
)

data class BackupCapacity(val bytes: Long, val unit: String)

data class BackupDocument(
    val exportedAt: Long,
    val vouchers: List<BackupVoucher>,
    val language: String?,
    val darkMode: Boolean?,
    val appColorTheme: String?,
    val portalName: String?,
    val portalTheme: String?,
    val packageBudgetEnabled: Boolean?,
    val selectedPackageType: String?,
    val capacities: Map<String, BackupCapacity>,
    val deviceNames: Map<String, String>,
    val sourceDailyBytes: Map<String, Long>,
    val clientDailyBytes: Map<String, Long>,
    val homeDailyBytes: Map<String, Long>,
    val clientTotalBytes: Long?
)

object BackupJsonCodec {
    const val SCHEMA_VERSION = 1
    const val MAX_BYTES = 5 * 1024 * 1024
    const val MAX_VOUCHERS = 50_000
    private val macPattern = Regex("(?i)(?:[0-9a-f]{2}:){5}[0-9a-f]{2}")
    private val packagePattern = Regex("[\\p{L}\\p{N}_-]{1,12}")
    private val datePattern = Regex("\\d{4}-\\d{2}-\\d{2}")
    private val themeCodes = setOf("default", "burgundy_beige", "ocean_blue", "forest", "purple", "rose")

    fun encode(document: BackupDocument): String {
        validate(document)
        val root = obj(
            "schemaVersion" to num(SCHEMA_VERSION.toLong()),
            "exportedAt" to num(document.exportedAt),
            "vouchers" to arr(document.vouchers.map { v -> obj(
                "code" to str(v.code), "durationMinutes" to num(v.durationMinutes),
                "dataLimitBytes" to num(v.dataLimitBytes), "boundMacAddress" to (v.boundMacAddress?.let(::str) ?: JNull),
                "isUsed" to bool(v.isUsed), "isBlocked" to bool(v.isBlocked), "createdAt" to num(v.createdAt),
                "firstUsedAt" to (v.firstUsedAt?.let(::num) ?: JNull), "bytesUsedSoFar" to num(v.bytesUsedSoFar),
                "packageType" to str(v.packageType)
            ) }),
            "settings" to obj(
                "language" to optional(document.language?.let(::str)), "darkMode" to optional(document.darkMode?.let(::bool)),
                "appColorTheme" to optional(document.appColorTheme?.let(::str)),
                "portalName" to optional(document.portalName?.let(::str)), "portalTheme" to optional(document.portalTheme?.let(::str)),
                "packageBudgetEnabled" to optional(document.packageBudgetEnabled?.let(::bool)),
                "selectedPackageType" to optional(document.selectedPackageType?.let(::str)),
                "capacities" to JObj(document.capacities.mapValues { (_, v) -> obj("bytes" to num(v.bytes), "unit" to str(v.unit)) }),
                "deviceNames" to JObj(document.deviceNames.mapValues { str(it.value) })
            ),
            "usage" to obj(
                "sourceDailyBytes" to longMap(document.sourceDailyBytes),
                "clientDailyBytes" to longMap(document.clientDailyBytes),
                "homeDailyBytes" to longMap(document.homeDailyBytes),
                "clientTotalBytes" to optional(document.clientTotalBytes?.let(::num))
            )
        )
        return stringify(root).also {
            require(it.toByteArray(Charsets.UTF_8).size <= MAX_BYTES) { "Backup file exceeds the 5 MiB limit" }
        }
    }

    fun decode(json: String): BackupDocument = try {
        decodeValidated(json)
    } catch (error: IllegalArgumentException) {
        throw error
    } catch (error: Exception) {
        throw IllegalArgumentException("Invalid backup document: ${error.message ?: "invalid value"}", error)
    }

    private fun decodeValidated(json: String): BackupDocument {
        require(json.toByteArray(Charsets.UTF_8).size <= MAX_BYTES) { "Backup file exceeds the 5 MiB limit" }
        val root = JsonParser(json).parse().asObject("root")
        require(root.keys == setOf("schemaVersion", "exportedAt", "vouchers", "settings", "usage")) { "Unexpected backup structure" }
        require(root.required("schemaVersion").asLong("schemaVersion") == SCHEMA_VERSION.toLong()) { "Unsupported backup schema version" }
        val voucherArray = root.required("vouchers").asArray("vouchers")
        require(voucherArray.size <= MAX_VOUCHERS) { "Too many vouchers" }
        val vouchers = voucherArray.mapIndexed { i, item ->
            val o = item.asObject("vouchers[$i]")
            require(o.keys == setOf("code", "durationMinutes", "dataLimitBytes", "boundMacAddress", "isUsed", "isBlocked", "createdAt", "firstUsedAt", "bytesUsedSoFar", "packageType")) { "Invalid voucher fields" }
            BackupVoucher(
                o.required("code").asString("code"), o.required("durationMinutes").asLong("durationMinutes"),
                o.required("dataLimitBytes").asLong("dataLimitBytes"), o.required("boundMacAddress").nullableString("boundMacAddress"),
                o.required("isUsed").asBoolean("isUsed"), o.required("isBlocked").asBoolean("isBlocked"),
                o.required("createdAt").asLong("createdAt"), o.required("firstUsedAt").nullableLong("firstUsedAt"),
                o.required("bytesUsedSoFar").asLong("bytesUsedSoFar"), o.required("packageType").asString("packageType")
            )
        }
        val s = root.required("settings").asObject("settings")
        require(s.keys == setOf("language", "darkMode", "appColorTheme", "portalName", "portalTheme", "packageBudgetEnabled", "selectedPackageType", "capacities", "deviceNames")) { "Invalid settings fields" }
        val capacities = s.required("capacities").asObject("capacities").mapValues { (type, value) ->
            val cap = value.asObject("capacity")
            require(cap.keys == setOf("bytes", "unit")) { "Invalid capacity" }
            BackupCapacity(cap.required("bytes").asLong("capacity.bytes"), cap.required("unit").asString("capacity.unit"))
        }
        val names = s.required("deviceNames").asObject("deviceNames").mapValues { it.value.asString("device name") }
        val u = root.required("usage").asObject("usage")
        require(u.keys == setOf("sourceDailyBytes", "clientDailyBytes", "homeDailyBytes", "clientTotalBytes")) { "Invalid usage fields" }
        val document = BackupDocument(
            exportedAt = root.required("exportedAt").asLong("exportedAt"), vouchers = vouchers,
            language = s.required("language").nullableString("language"), darkMode = s.required("darkMode").nullableBoolean("darkMode"),
            appColorTheme = s.required("appColorTheme").nullableString("appColorTheme"), portalName = s.required("portalName").nullableString("portalName"),
            portalTheme = s.required("portalTheme").nullableString("portalTheme"), packageBudgetEnabled = s.required("packageBudgetEnabled").nullableBoolean("packageBudgetEnabled"),
            selectedPackageType = s.required("selectedPackageType").nullableString("selectedPackageType"), capacities = capacities,
            deviceNames = names, sourceDailyBytes = longMap(u.required("sourceDailyBytes"), "sourceDailyBytes"),
            clientDailyBytes = longMap(u.required("clientDailyBytes"), "clientDailyBytes"), homeDailyBytes = longMap(u.required("homeDailyBytes"), "homeDailyBytes"),
            clientTotalBytes = u.required("clientTotalBytes").nullableLong("clientTotalBytes")
        )
        validate(document)
        return document
    }

    private fun validate(d: BackupDocument) {
        require(d.exportedAt >= 0) { "Invalid export timestamp" }
        require(d.vouchers.size <= MAX_VOUCHERS) { "Too many vouchers" }
        require(d.vouchers.map { it.code }.distinct().size == d.vouchers.size) { "Duplicate voucher codes" }
        d.vouchers.forEach { v ->
            require(v.code.isNotBlank() && v.code.length <= 128 && v.code.none(Char::isISOControl)) { "Invalid voucher code" }
            require(v.durationMinutes >= 0 && v.dataLimitBytes >= 0 && v.bytesUsedSoFar >= 0 && v.createdAt >= 0) { "Negative voucher value" }
            require(v.firstUsedAt == null || v.firstUsedAt >= 0) { "Invalid first-use time" }
            require(v.boundMacAddress == null || (v.boundMacAddress.length <= 64 && v.boundMacAddress.none(Char::isISOControl))) { "Invalid voucher binding" }
            require(packagePattern.matches(v.packageType)) { "Invalid package type" }
        }
        require(d.language == null || d.language in setOf("ar", "en")) { "Invalid language" }
        require(d.appColorTheme == null || d.appColorTheme in themeCodes) { "Invalid app theme" }
        require(d.portalName == null || (d.portalName.length <= 40 && d.portalName.none(Char::isISOControl))) { "Invalid portal name" }
        require(d.portalTheme == null || d.portalTheme in setOf("green", "blue", "red", "pink", "purple", "orange")) { "Invalid portal theme" }
        require(d.selectedPackageType == null || (packagePattern.matches(d.selectedPackageType) && d.selectedPackageType !in setOf("LEGACY", "NONE"))) { "Invalid selected package type" }
        d.capacities.forEach { (type, cap) ->
            require(packagePattern.matches(type) && type !in setOf("LEGACY", "NONE")) { "Invalid capacity type" }
            require(cap.bytes >= 0 && cap.unit in setOf("MB", "GB")) { "Invalid capacity" }
        }
        require(d.capacities.size <= 128) { "Too many package capacities" }
        d.deviceNames.forEach { (mac, name) ->
            require(macPattern.matches(mac)) { "Invalid device address" }
            require(name.isNotBlank() && name.length <= 100 && name.none(Char::isISOControl)) { "Invalid device name" }
        }
        require(d.deviceNames.size <= 10_000) { "Too many device names" }
        listOf(d.sourceDailyBytes, d.clientDailyBytes, d.homeDailyBytes).forEach { values ->
            require(values.size <= 20_000) { "Too many usage dates" }
            values.forEach { (date, bytes) ->
                require(datePattern.matches(date) && runCatching { LocalDate.parse(date) }.isSuccess && bytes >= 0) { "Invalid usage value" }
            }
        }
        require(d.clientTotalBytes == null || d.clientTotalBytes >= 0) { "Invalid total usage" }
    }

    private fun longMap(value: JValue, name: String): Map<String, Long> = value.asObject(name).mapValues { it.value.asLong(name) }
    private fun longMap(values: Map<String, Long>) = JObj(values.mapValues { num(it.value) })
    private fun optional(v: JValue?): JValue = v ?: JNull
    private fun obj(vararg fields: Pair<String, JValue>) = JObj(linkedMapOf(*fields))
    private fun arr(values: List<JValue>) = JArr(values)
    private fun str(v: String) = JStr(v)
    private fun num(v: Long) = JNum(v)
    private fun bool(v: Boolean) = JBool(v)

    private fun stringify(value: JValue): String = when (value) {
        JNull -> "null"
        is JBool -> value.value.toString()
        is JNum -> value.value.toString()
        is JStr -> quote(value.value)
        is JArr -> value.values.joinToString(prefix = "[", postfix = "]") { stringify(it) }
        is JObj -> value.values.entries.joinToString(prefix = "{", postfix = "}") { "${quote(it.key)}:${stringify(it.value)}" }
    }
    private fun quote(text: String): String = buildString {
        append('"'); text.forEach { c -> when (c) {
            '"' -> append("\\\""); '\\' -> append("\\\\"); '\n' -> append("\\n"); '\r' -> append("\\r"); '\t' -> append("\\t")
            else -> if (c.code < 0x20) append("\\u%04x".format(c.code)) else append(c)
        } }; append('"')
    }

    private sealed interface JValue
    private data object JNull : JValue
    private data class JBool(val value: Boolean) : JValue
    private data class JNum(val value: Long) : JValue
    private data class JStr(val value: String) : JValue
    private data class JArr(val values: List<JValue>) : JValue
    private data class JObj(val values: Map<String, JValue>) : JValue

    private fun JValue.asObject(label: String) = (this as? JObj)?.values ?: error("$label must be an object")
    private fun JValue.asArray(label: String) = (this as? JArr)?.values ?: error("$label must be an array")
    private fun JValue.asString(label: String) = (this as? JStr)?.value ?: error("$label must be a string")
    private fun JValue.asLong(label: String) = (this as? JNum)?.value ?: error("$label must be an integer")
    private fun JValue.asBoolean(label: String) = (this as? JBool)?.value ?: error("$label must be a boolean")
    private fun JValue.nullableString(label: String) = if (this === JNull) null else asString(label)
    private fun JValue.nullableLong(label: String) = if (this === JNull) null else asLong(label)
    private fun JValue.nullableBoolean(label: String) = if (this === JNull) null else asBoolean(label)
    private fun Map<String, JValue>.required(key: String) = this[key] ?: error("Missing field: $key")

    private class JsonParser(private val input: String) {
        private var index = 0
        fun parse(): JValue {
            val value = value(0); whitespace(); require(index == input.length) { "Trailing JSON content" }; return value
        }
        private fun value(depth: Int): JValue {
            require(depth <= 32) { "JSON nesting too deep" }; whitespace(); require(index < input.length) { "Unexpected end of JSON" }
            return when (input[index]) {
                '{' -> objectValue(depth + 1); '[' -> arrayValue(depth + 1); '"' -> JStr(stringValue())
                't' -> { literal("true"); JBool(true) }; 'f' -> { literal("false"); JBool(false) }; 'n' -> { literal("null"); JNull }
                '-', in '0'..'9' -> numberValue(); else -> error("Invalid JSON value at $index")
            }
        }
        private fun objectValue(depth: Int): JObj {
            index++; whitespace(); val result = linkedMapOf<String, JValue>(); if (take('}')) return JObj(result)
            while (true) {
                whitespace(); require(index < input.length && input[index] == '"') { "Object key must be a string" }
                val key = stringValue(); require(key !in result) { "Duplicate object key" }; whitespace(); require(take(':')) { "Expected ':'" }
                result[key] = value(depth); whitespace(); if (take('}')) return JObj(result); require(take(',')) { "Expected ','" }
            }
        }
        private fun arrayValue(depth: Int): JArr {
            index++; whitespace(); val result = mutableListOf<JValue>(); if (take(']')) return JArr(result)
            while (true) { result += value(depth); whitespace(); if (take(']')) return JArr(result); require(take(',')) { "Expected ','" } }
        }
        private fun stringValue(): String {
            require(take('"')); val out = StringBuilder()
            while (index < input.length) {
                val c = input[index++]
                when (c) {
                    '"' -> return out.toString()
                    '\\' -> {
                        require(index < input.length) { "Incomplete escape" }; when (val e = input[index++]) {
                            '"', '\\', '/' -> out.append(e); 'b' -> out.append('\b'); 'f' -> out.append('\u000c'); 'n' -> out.append('\n'); 'r' -> out.append('\r'); 't' -> out.append('\t')
                            'u' -> { require(index + 4 <= input.length) { "Incomplete unicode escape" }; val code = input.substring(index, index + 4).toIntOrNull(16) ?: error("Invalid unicode escape"); out.append(code.toChar()); index += 4 }
                            else -> error("Invalid escape")
                        }
                    }
                    else -> { require(c.code >= 0x20) { "Control character in JSON string" }; out.append(c) }
                }
            }
            error("Unterminated string")
        }
        private fun numberValue(): JNum {
            val start = index; if (input[index] == '-') index++
            require(index < input.length) { "Invalid integer" }
            if (input[index] == '0') index++ else { require(input[index] in '1'..'9') { "Invalid integer" }; while (index < input.length && input[index].isDigit()) index++ }
            require(index == input.length || input[index] !in ".eE") { "Only integer numbers are supported" }
            return JNum(input.substring(start, index).toLongOrNull() ?: error("Integer out of range"))
        }
        private fun literal(value: String) { require(input.startsWith(value, index)) { "Invalid literal" }; index += value.length }
        private fun whitespace() { while (index < input.length && input[index] in " \t\r\n") index++ }
        private fun take(c: Char): Boolean = if (index < input.length && input[index] == c) { index++; true } else false
    }
}
