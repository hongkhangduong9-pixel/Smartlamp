package com.example.tiles

import android.app.StatusBarManager
import android.content.ComponentName
import android.content.Context
import android.graphics.drawable.Icon
import android.os.Build
import com.example.R
import java.util.concurrent.Executors

object TileRegistry {
    val allTileClasses: List<Class<out BaseLightTileService>> = listOf(
        LightTileService1::class.java,
        LightTileService2::class.java,
        LightTileService3::class.java,
        LightTileService4::class.java,
        LightTileService5::class.java,
        LightTileService6::class.java,
        LightTileService7::class.java,
        LightTileService8::class.java,
    )

    fun classForSlot(slotIndex: Int): Class<out BaseLightTileService> {
        return allTileClasses.getOrElse(slotIndex - 1) { LightTileService1::class.java }
    }

    fun canRequestAddTile(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

    fun requestAddTileToQuickSettings(
        context: Context,
        slotIndex: Int,
        label: String,
        onResult: (String) -> Unit,
    ) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            onResult("Swipe down Quick Settings twice, tap Edit, and drag \"Light $slotIndex\" into your active tiles.")
            return
        }
        try {
            val sbm = context.getSystemService(StatusBarManager::class.java)
            if (sbm == null) {
                onResult("StatusBarManager unavailable. Add \"Light $slotIndex\" from Quick Settings Edit menu.")
                return
            }
            val component = ComponentName(context, classForSlot(slotIndex))
            val icon = Icon.createWithResource(context, R.drawable.ic_qs_lightbulb)
            sbm.requestAddTileService(
                component,
                label.ifBlank { "Light $slotIndex" },
                icon,
                Executors.newSingleThreadExecutor(),
            ) { resultCode ->
                val message = when (resultCode) {
                    StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_ADDED ->
                        "Added \"$label\" to Quick Settings."
                    StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_ALREADY_ADDED ->
                        "\"$label\" is already in your Quick Settings."
                    else ->
                        "Tile add dismissed. You can add it anytime from Quick Settings Edit."
                }
                onResult(message)
            }
        } catch (t: Throwable) {
            onResult("Could not open system prompt: ${t.message ?: "Use Quick Settings Edit menu"}")
        }
    }
}
