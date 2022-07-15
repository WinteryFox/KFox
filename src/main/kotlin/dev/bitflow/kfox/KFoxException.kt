package dev.bitflow.kfox

import dev.kord.common.entity.Permission

open class KFoxException(message: String? = null, cause: Throwable? = null) : RuntimeException(message, cause)

class InsufficientPermissionsException(
    val permissions: Set<Permission>,
    message: String? = null,
    cause: Throwable? = null
) : KFoxException(message, cause)
