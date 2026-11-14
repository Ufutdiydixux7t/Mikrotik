package com.example.mikrotik.core.hotspot

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ClientAddressResolverTest {
    private val mac = "02:11:22:33:44:55"

    @Test
    fun prefersReachableNeighborOverOldDhcpLeaseAndStaleNeighbor() {
        val ip = ClientAddressResolver.findIpv4ForMac(
            macAddress = mac,
            interfaceName = "ap0",
            neighborLines = listOf(
                "192.168.43.20 dev ap0 lladdr $mac STALE",
                "192.168.43.91 dev ap0 lladdr $mac REACHABLE"
            ),
            arpLines = emptyList(),
            dhcpLeaseLines = listOf("1999999999 $mac 192.168.43.20 note9 01:02:03:04:05:06")
        )

        assertEquals("192.168.43.91", ip)
    }

    @Test
    fun prefersDhcpLeaseOverStaleNeighborEntry() {
        val ip = ClientAddressResolver.findIpv4ForMac(
            macAddress = mac,
            interfaceName = "ap0",
            neighborLines = listOf("192.168.43.20 dev ap0 lladdr $mac STALE"),
            arpLines = emptyList(),
            dhcpLeaseLines = listOf("1999999999 $mac 192.168.43.91 note9 *")
        )

        assertEquals("192.168.43.91", ip)
    }

    @Test
    fun ignoresNeighborFromDifferentInterfaceAndInvalidAddresses() {
        val ip = ClientAddressResolver.findIpv4ForMac(
            macAddress = mac,
            interfaceName = "ap0",
            neighborLines = listOf(
                "192.168.43.91 dev wlan0 lladdr $mac REACHABLE",
                "999.168.43.91 dev ap0 lladdr $mac REACHABLE"
            ),
            arpLines = emptyList(),
            dhcpLeaseLines = emptyList()
        )

        assertNull(ip)
    }

    @Test
    fun usesValidArpEntryAsLastFallback() {
        val ip = ClientAddressResolver.findIpv4ForMac(
            macAddress = mac,
            interfaceName = "ap0",
            neighborLines = emptyList(),
            arpLines = listOf(
                "IP address HW type Flags HW address Mask Device",
                "192.168.43.91 0x1 0x2 $mac * ap0"
            ),
            dhcpLeaseLines = emptyList()
        )

        assertEquals("192.168.43.91", ip)
    }
}
