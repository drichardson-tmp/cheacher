package com.roseau.opening.progress

/**
 * Android needs a `Context` to know where files live, and expect/actual functions
 * cannot take one without infecting common code. So [MainActivity] drops the files-dir
 * path in here once at startup and the actual reads it back.
 */
object AndroidDataStoreLocation {
    @Volatile
    var filesDirPath: String? = null
}

actual fun roseauDataStorePath(): String {
    val dir = checkNotNull(AndroidDataStoreLocation.filesDirPath) {
        "AndroidDataStoreLocation.filesDirPath must be set before the ProgressStore is touched"
    }
    return "$dir/roseau.preferences_pb"
}
