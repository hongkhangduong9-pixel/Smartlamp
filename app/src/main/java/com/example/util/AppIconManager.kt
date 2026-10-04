package com.example.util

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager

data class AppIconOption(
    val key: String,
    val name: String,
    val description: String,
    val aliasName: String,
    val previewColor: Long
)

object AppIconManager {
    val ICONS = listOf(
        AppIconOption(
            key = "default",
            name = "Vàng Hổ Phách (Mặc định)",
            description = "Biểu tượng bóng đèn phát sáng vàng ấm kinh điển",
            aliasName = "com.example.MainActivityDefault",
            previewColor = 0xFFF59E0BL
        ),
        AppIconOption(
            key = "cyan",
            name = "Cyber Cyan Neon",
            description = "Phong cách ánh sáng công nghệ xanh ngọc sắc sảo",
            aliasName = "com.example.MainActivityCyan",
            previewColor = 0xFF06B6D4L
        ),
        AppIconOption(
            key = "emerald",
            name = "Eco Emerald Green",
            description = "Xanh ngọc lục bảo tiết kiệm năng lượng tươi sáng",
            aliasName = "com.example.MainActivityEmerald",
            previewColor = 0xFF10B981L
        ),
        AppIconOption(
            key = "purple",
            name = "Ambient Neon Purple",
            description = "Tím huyền ảo, phong cách ánh sáng tương lai",
            aliasName = "com.example.MainActivityPurple",
            previewColor = 0xFFA855F7L
        )
    )

    /**
     * Changes the PRIMARY launcher icon itself directly via Activity-Alias.
     * Does NOT create duplicate shortcuts or extra icons!
     */
    fun applyIcon(context: Context, iconKey: String): Boolean {
        val targetOption = ICONS.find { it.key == iconKey } ?: ICONS.first()
        val pm = context.packageManager
        val packageName = context.packageName

        return try {
            // First enable the selected target icon alias
            val targetComponent = ComponentName(packageName, targetOption.aliasName)
            pm.setComponentEnabledSetting(
                targetComponent,
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP
            )

            // Then disable all other icon aliases
            ICONS.filter { it.key != targetOption.key }.forEach { opt ->
                val otherComponent = ComponentName(packageName, opt.aliasName)
                pm.setComponentEnabledSetting(
                    otherComponent,
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                    PackageManager.DONT_KILL_APP
                )
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
