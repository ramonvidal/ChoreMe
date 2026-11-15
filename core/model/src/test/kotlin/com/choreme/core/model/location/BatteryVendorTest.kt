package com.choreme.core.model.location

import org.junit.Assert.assertEquals
import org.junit.Test

class BatteryVendorTest {
    @Test
    fun `makers are recognised in any case, with their sister brands`() {
        assertEquals(BatteryVendor.XIAOMI, batteryVendorOf("Xiaomi"))
        assertEquals(BatteryVendor.XIAOMI, batteryVendorOf("POCO"))
        assertEquals(BatteryVendor.SAMSUNG, batteryVendorOf("samsung"))
        assertEquals(BatteryVendor.HUAWEI, batteryVendorOf("HONOR"))
        assertEquals(BatteryVendor.OPPO, batteryVendorOf("realme"))
        assertEquals(BatteryVendor.OPPO, batteryVendorOf(" OnePlus "))
        assertEquals(BatteryVendor.VIVO, batteryVendorOf("vivo"))
    }

    @Test
    fun `anything else is plain Android`() {
        assertEquals(BatteryVendor.OTHER, batteryVendorOf("Google"))
        assertEquals(BatteryVendor.OTHER, batteryVendorOf(null))
    }
}
