package com.example.blescanner.presentation.permission.screen

import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.example.blescanner.presentation.permission.util.PermissionUtils
import kotlinx.coroutines.delay

@Composable
fun requestPermissions(
    permissions: Array<String> = PermissionUtils.permissions.toTypedArray(),
    autoLaunch: Boolean = true,
    onResult: (allGranted: Boolean) -> Unit = {}
): Boolean {
    val context = LocalContext.current

    var allGranted by remember { mutableStateOf(checkAllPermissions(context, permissions)) }

    val permsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        val granted = result.values.all { it }
        allGranted = granted
        onResult(granted)
    }

    LaunchedEffect(key1 = Unit) {
        if (autoLaunch && !allGranted) {
            delay(200)
            permsLauncher.launch(permissions)
        }
    }
    return allGranted
}

private fun checkAllPermissions(context: Context, permissions: Array<String>): Boolean =
    permissions.all { p ->
        ContextCompat.checkSelfPermission(context, p) == PackageManager.PERMISSION_GRANTED
    }