package androidx.loader.app

import androidx.loader.app.LoaderManager
import androidx.loader.app.LoaderManagerImpl

/**
 * Created by mariotaku on 2016/11/26.
 */

fun LoaderManager.hasRunningLoadersSafe(): Boolean {
    if (this !is LoaderManagerImpl) return false
    return this.hasRunningLoaders()
//    var loadersRunning = false
//    val count = mLoaders.size()
//    for (i in 0 until count) {
//        val li = mLoaders.valueAt(i) ?: continue
//        loadersRunning = loadersRunning or (li.mStarted && !li.mDeliveredData)
//    }
//    return loadersRunning
}