package com.tamedphantoms.mod

import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger

/** Идентификатор и лог. Точку входа подставляет загрузчик. */
object TamedPhantomsMod {
    const val MOD_ID = "tamedphantoms"

    @JvmField
    val LOGGER: Logger = LogManager.getLogger(MOD_ID)
}
