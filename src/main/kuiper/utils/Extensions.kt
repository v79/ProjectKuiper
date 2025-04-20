package utils

import java.io.File

/**
 * A generic collection of extension functions
 */

/**
 * Convert a string to a filename-safe version
 */
fun String.slug(): String {
    return this.lowercase().replace(Regex("\\W+"), "_")
}