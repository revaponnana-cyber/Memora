package com.memora.app.app

import android.app.Application

/**
 * Application class.
 *
 * Deliberately lightweight: Memora never starts background services, never
 * scans the database and never runs continuous processing at launch. The
 * language agent runs only while the user submits a sentence.
 */
class MemoraApplication : Application() {

    /** Container holding the database, repository and use cases (lazy per call). */
    val container: AppContainer by lazy { AppContainer(this) }
}
