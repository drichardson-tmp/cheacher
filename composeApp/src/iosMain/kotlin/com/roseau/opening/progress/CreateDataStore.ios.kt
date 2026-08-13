package com.roseau.opening.progress

import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSUserDomainMask

actual fun roseauDataStorePath(): String {
    val documents = NSSearchPathForDirectoriesInDomains(
        NSDocumentDirectory,
        NSUserDomainMask,
        true,
    ).firstOrNull() as? String ?: error("no documents directory")
    return "$documents/roseau.preferences_pb"
}
