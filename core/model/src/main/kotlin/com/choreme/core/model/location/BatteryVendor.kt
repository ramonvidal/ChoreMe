package com.choreme.core.model.location

/** Phone makers whose own battery savers stop background apps harder than plain Android does. */
enum class BatteryVendor { XIAOMI, SAMSUNG, HUAWEI, OPPO, VIVO, OTHER }

/** The maker of a phone ([manufacturer] is what Android reports, in any letter case), or [BatteryVendor.OTHER]. */
fun batteryVendorOf(manufacturer: String?): BatteryVendor = when (manufacturer?.trim()?.lowercase()) {
    "xiaomi", "redmi", "poco" -> BatteryVendor.XIAOMI
    "samsung" -> BatteryVendor.SAMSUNG
    "huawei", "honor" -> BatteryVendor.HUAWEI
    "oppo", "realme", "oneplus" -> BatteryVendor.OPPO
    "vivo", "iqoo" -> BatteryVendor.VIVO
    else -> BatteryVendor.OTHER
}
