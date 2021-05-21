package com.torusage

import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Get logger for certain class
 */
inline fun <reified T> T.logger(): Logger = LoggerFactory.getLogger(T::class.java)

