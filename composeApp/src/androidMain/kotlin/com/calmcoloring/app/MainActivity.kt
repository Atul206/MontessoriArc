package com.calmcoloring.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.calmcoloring.app.platform.appContext
import com.calmcoloring.app.platform.currentActivity

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // Must run before super.onCreate() (androidx.core.splashscreen
        // contract) — reads the Theme.CalmColoring.Starting attrs set on
        // this activity in AndroidManifest.xml and hands off to
        // Theme.CalmColoring automatically once the splash dismisses.
        installSplashScreen()
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        // Assigned here (ahead of Task 9's MainActivity work) so Task 6's print
        // flow — and Task 8's share flow — have a real Context to use; this
        // assignment is idempotent and safe to leave in place for later tasks.
        appContext = applicationContext
        setContent { CalmColoringApp() }
    }

    override fun onResume() {
        super.onResume()
        // android.print.PrintManager requires an actual Activity Context
        // (see Printer.android.kt); track the foreground activity instance
        // separately from the application-wide appContext.
        currentActivity = this
    }

    override fun onPause() {
        currentActivity = null
        super.onPause()
    }
}
