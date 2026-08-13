package com.cheacher.app.progress

import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSUserDomainMask

actual fun cheacherDataStorePath(): String {
    val documents = NSSearchPathForDirectoriesInDomains(
        NSDocumentDirectory,
        NSUserDomainMask,
        true,
    ).firstOrNull() as? String ?: error("no documents directory")
    return "$documents/cheacher.preferences_pb"
}
