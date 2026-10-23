package com.example.util

/**
 * Helper utility for semantic version parsing, comparison, and update validation.
 * Supports standard semantic version strings such as "1.2.0", "1.3.0", "1.4.0", "2.0.0",
 * with or without "v" prefix.
 */
object AppVersionHelper {

    /**
     * Determines whether [targetVersion] is strictly newer (higher) than [currentVersion].
     * E.g.:
     *  - isVersionNewer("1.2.0", "1.3.0") -> true
     *  - isVersionNewer("1.3.0", "1.4.0") -> true
     *  - isVersionNewer("1.3.0", "1.3.0") -> false
     *  - isVersionNewer("1.3.0", "1.2.0") -> false
     */
    fun isVersionNewer(currentVersion: String, targetVersion: String): Boolean {
        return compareVersions(targetVersion, currentVersion) > 0
    }

    /**
     * Compares two semantic version strings.
     * Returns:
     *   > 0 if [v1] is newer than [v2]
     *   < 0 if [v1] is older than [v2]
     *   0 if both versions are equivalent
     */
    fun compareVersions(v1: String, v2: String): Int {
        val clean1 = v1.trim().removePrefix("v").removePrefix("V")
        val clean2 = v2.trim().removePrefix("v").removePrefix("V")

        val parts1 = clean1.split(".").mapNotNull { it.trim().toIntOrNull() }
        val parts2 = clean2.split(".").mapNotNull { it.trim().toIntOrNull() }

        val maxLen = maxOf(parts1.size, parts2.size)
        for (i in 0 until maxLen) {
            val p1 = parts1.getOrElse(i) { 0 }
            val p2 = parts2.getOrElse(i) { 0 }
            if (p1 != p2) {
                return p1.compareTo(p2)
            }
        }
        return 0
    }
}
