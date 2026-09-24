package com.example.help_markun.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** 端末の再起動・アプリ更新のあと、見守りがオンなら自動で再開する */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED, Intent.ACTION_MY_PACKAGE_REPLACED -> HelpScanService.startIfAllowed(context)
        }
    }
}
