package com.example.mikrotik.core.network

import org.junit.Assert.assertEquals
import org.junit.Test

class UsageSampleMetadataTest {
    @Test
    fun classifiesEachAvailableCounterSource() {
        assertEquals(UsageSampleSource.WIFI_STATION, UsageSampleMetadata.source(true, false))
        assertEquals(UsageSampleSource.IPTABLES, UsageSampleMetadata.source(false, true))
        assertEquals(UsageSampleSource.BOTH, UsageSampleMetadata.source(true, true))
        assertEquals(UsageSampleSource.UNAVAILABLE, UsageSampleMetadata.source(false, false))
    }

    @Test
    fun identityIsStableAcrossMacCaseAndWhitespace() {
        assertEquals(
            UsageSampleMetadata.identity(" AA:BB:CC:DD:EE:FF ", "VOUCHER-1"),
            UsageSampleMetadata.identity("aa:bb:cc:dd:ee:ff", "VOUCHER-1")
        )
    }

    @Test
    fun identitySeparatesDifferentVouchersForTheSameDevice() {
        assertEquals(
            false,
            UsageSampleMetadata.identity("aa:bb:cc:dd:ee:ff", "VOUCHER-1") ==
                UsageSampleMetadata.identity("aa:bb:cc:dd:ee:ff", "VOUCHER-2")
        )
    }
}
