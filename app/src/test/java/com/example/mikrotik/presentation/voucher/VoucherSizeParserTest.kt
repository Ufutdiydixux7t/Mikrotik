package com.example.mikrotik.presentation.voucher

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class VoucherSizeParserTest {
    @Test
    fun convertsMegabytesToBytes() {
        assertEquals(500L * 1024L * 1024L, VoucherSizeParser.toBytes("500", VoucherDataUnit.MB))
    }

    @Test
    fun convertsFractionalGigabytesToBytes() {
        assertEquals(512L * 1024L * 1024L, VoucherSizeParser.toBytes("0.5", VoucherDataUnit.GB))
    }

    @Test
    fun acceptsArabicDigitsAndDecimalSeparator() {
        assertEquals(1536L * 1024L * 1024L, VoucherSizeParser.toBytes("١٫٥", VoucherDataUnit.GB))
    }

    @Test
    fun rejectsZeroNegativeAndInvalidValues() {
        assertNull(VoucherSizeParser.toBytes("0", VoucherDataUnit.MB))
        assertNull(VoucherSizeParser.toBytes("-5", VoucherDataUnit.GB))
        assertNull(VoucherSizeParser.toBytes("abc", VoucherDataUnit.MB))
        assertNull(VoucherSizeParser.toBytes("999999999999999999999999", VoucherDataUnit.GB))
    }
}
