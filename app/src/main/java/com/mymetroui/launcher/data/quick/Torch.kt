package com.mymetroui.launcher.data.quick

import android.content.Context
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

interface Torch {
    val available: Boolean
    val isOn: StateFlow<Boolean>
    fun set(on: Boolean)
}

class CameraTorch(context: Context) : Torch {
    private val manager = context.getSystemService(CameraManager::class.java)
    private val cameraId: String? = try {
        manager.cameraIdList.firstOrNull {
            manager.getCameraCharacteristics(it).get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
        }
    } catch (_: CameraAccessException) {
        null
    }
    private val state = MutableStateFlow(false)

    override val available: Boolean get() = cameraId != null
    override val isOn: StateFlow<Boolean> = state

    init {
        if (cameraId != null) {
            manager.registerTorchCallback(object : CameraManager.TorchCallback() {
                override fun onTorchModeChanged(id: String, enabled: Boolean) { if (id == cameraId) state.value = enabled }
                override fun onTorchModeUnavailable(id: String) { if (id == cameraId) state.value = false }
            }, null)
        }
    }

    override fun set(on: Boolean) {
        val id = cameraId ?: return
        try {
            manager.setTorchMode(id, on)
        } catch (_: CameraAccessException) {
            state.value = false // camera busy or flash unavailable
        } catch (_: IllegalArgumentException) {
            state.value = false
        }
    }
}
