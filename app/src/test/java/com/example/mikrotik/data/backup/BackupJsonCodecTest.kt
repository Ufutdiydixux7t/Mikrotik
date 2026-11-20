package com.example.mikrotik.data.backup

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class BackupJsonCodecTest {
    private val sample = BackupDocument(
        exportedAt = 1_700_000_000_000,
        vouchers = listOf(
            BackupVoucher(
                code = "VOUCHER-أ-1", durationMinutes = 1440, dataLimitBytes = 1_073_741_824,
                boundMacAddress = "aa:bb:cc:dd:ee:ff", isUsed = true, isBlocked = false,
                createdAt = 1_699_000_000_000, firstUsedAt = 1_699_100_000_000,
                bytesUsedSoFar = 2048, packageType = "A"
            )
        ),
        language = "ar", darkMode = true, appColorTheme = "ocean_blue",
        portalName = "Mikrotik", portalTheme = "green", packageBudgetEnabled = true,
        selectedPackageType = "A", capacities = mapOf("A" to BackupCapacity(2_000_000_000, "GB")),
        deviceNames = mapOf("aa:bb:cc:dd:ee:ff" to "Office AP"),
        sourceDailyBytes = mapOf("2026-11-20" to 10_000L),
        clientDailyBytes = mapOf("2026-11-20" to 8_000L),
        homeDailyBytes = mapOf("2026-11-20" to 9_000L), clientTotalBytes = 50_000L
    )

    @Test fun roundTripPreservesVouchersAndSafePreferences() {
        val json = BackupJsonCodec.encode(sample)
        assertEquals(sample, BackupJsonCodec.decode(json))
    }

    @Test fun rejectsUnsupportedVersion() {
        val json = BackupJsonCodec.encode(sample).replace("\"schemaVersion\":1", "\"schemaVersion\":2")
        assertThrows(IllegalArgumentException::class.java) { BackupJsonCodec.decode(json) }
    }

    @Test fun rejectsWrongValueTypesAndUnknownRootFields() {
        val json = BackupJsonCodec.encode(sample)
        assertThrows(IllegalArgumentException::class.java) {
            BackupJsonCodec.decode(json.replace("\"darkMode\":true", "\"darkMode\":\"yes\""))
        }
        assertThrows(IllegalArgumentException::class.java) {
            BackupJsonCodec.decode(json.replace("\"schemaVersion\":1,", "\"schemaVersion\":1,\"active_sessions\":[],"))
        }
    }

    @Test fun rejectsInvalidMacAndNegativeUsage() {
        val badMac = BackupJsonCodec.encode(sample).replace("aa:bb:cc:dd:ee:ff", "not-a-mac")
        assertThrows(IllegalArgumentException::class.java) { BackupJsonCodec.decode(badMac) }
        val negative = BackupJsonCodec.encode(sample).replace("\"clientTotalBytes\":50000", "\"clientTotalBytes\":-1")
        assertThrows(IllegalArgumentException::class.java) { BackupJsonCodec.decode(negative) }
    }

    @Test fun rejectsMalformedAndOversizedFiles() {
        assertThrows(IllegalArgumentException::class.java) { BackupJsonCodec.decode("{broken") }
        assertThrows(IllegalArgumentException::class.java) { BackupJsonCodec.decode(" ".repeat(BackupJsonCodec.MAX_BYTES + 1)) }
    }
}
