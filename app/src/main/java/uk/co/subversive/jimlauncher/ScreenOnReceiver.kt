package uk.co.subversive.jimlauncher

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class ScreenOnReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_SCREEN_ON ||
            intent.action == Intent.ACTION_USER_PRESENT) {

            val launchIntent = Intent(context, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
            }
            context.startActivity(launchIntent)
        }
    }
}
