package com.rn.library.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GitHubUpdateCheckerTest {
    @Test
    fun compareVersions_detectsNewerPatch() {
        assertTrue(GitHubUpdateChecker.isNewer("2.4", "2.3"))
        assertTrue(GitHubUpdateChecker.isNewer("v2.3.1", "2.3"))
        assertFalse(GitHubUpdateChecker.isNewer("2.3", "2.3"))
        assertFalse(GitHubUpdateChecker.isNewer("2.2", "2.3"))
        assertEquals(0, GitHubUpdateChecker.compareVersions("2.3", "v2.3"))
    }
}
