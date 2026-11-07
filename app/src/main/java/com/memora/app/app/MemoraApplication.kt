package com.memora.app.app

import android.app.Application

/**
 * Application class.
 *
 * Deliberately lightweight: Memora never starts background services, never
 * scans the database and never runs continuous processing at launch. All work
 * happens while the user is using the app or explicitly activates a feature.
 * Later phases initialise the database and backup tooling here lazily.
 */
class MemoraApplication : Application()
