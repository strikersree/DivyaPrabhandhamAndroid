package com.srinivaskannan.divyaprabhandham.update

import android.app.Activity
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory

/**
 * The real one. Its debug twin can be told to hand back a fake instead —
 * see src/debug — which is the only way to exercise the update flow without
 * shipping a Play release first.
 */
fun createAppUpdateManager(activity: Activity): AppUpdateManager =
    AppUpdateManagerFactory.create(activity)
