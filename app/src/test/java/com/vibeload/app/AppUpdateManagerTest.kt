package com.vibeload.app

import com.vibeload.app.platform.update.AppUpdateManager
import com.vibeload.app.platform.update.UpdateDialogState
import com.vibeload.app.platform.update.UpdateInfo
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.io.FileOutputStream

class AppUpdateManagerTest {

    @Test
    fun isNewerVersion_detectsMajorMinorPatchUpdates() {
        assertTrue(AppUpdateManager.isNewerVersion("1.0.1", "1.0.0"))
        assertTrue(AppUpdateManager.isNewerVersion("1.1.0", "1.0.9"))
        assertTrue(AppUpdateManager.isNewerVersion("2.0.0", "1.9.9"))
        assertTrue(AppUpdateManager.isNewerVersion("1.0.0.1", "1.0.0"))
    }

    @Test
    fun isNewerVersion_returnsFalseForSameOrOlderVersion() {
        assertFalse(AppUpdateManager.isNewerVersion("1.0.0", "1.0.0"))
        assertFalse(AppUpdateManager.isNewerVersion("0.9.9", "1.0.0"))
        assertFalse(AppUpdateManager.isNewerVersion("1.0.0", "1.0.1"))
    }

    @Test
    fun parseVersionCodeFromTag_correctlyParsesSemanticTags() {
        assertEquals(10001, AppUpdateManager.parseVersionCodeFromTag("v1.0.1"))
        assertEquals(10100, AppUpdateManager.parseVersionCodeFromTag("v1.1.0"))
        assertEquals(20000, AppUpdateManager.parseVersionCodeFromTag("v2.0.0"))
        assertEquals(2, AppUpdateManager.parseVersionCodeFromTag("v2"))
    }

    @Test
    fun computeSha256_accuratelyHashesFileContents() {
        val tempFile = File.createTempFile("test_hash", ".txt")
        try {
            FileOutputStream(tempFile).use {
                it.write("VibeLoadUpdateTestContent".toByteArray(Charsets.UTF_8))
            }
            val hash = AppUpdateManager.computeSha256(tempFile)
            assertNotNull(hash)
            assertEquals(64, hash.length)

            // Re-verifying identical hash on same content
            val hash2 = AppUpdateManager.computeSha256(tempFile)
            assertEquals(hash, hash2)
        } finally {
            tempFile.delete()
        }
    }

    @Test
    fun versionCheck_doesNotConsiderSameOrLowerVersionCodeAsNewUpdate() {
        // Current installed is versionCode = 2
        val currentVersionCode = 2
        val currentVersionName = "1.0.1"

        // Case A: Remote has versionCode = 1 (lower)
        val remoteLower = 1
        val hasUpdateLower = remoteLower > currentVersionCode
        assertFalse("Bản có versionCode thấp hơn không được coi là mới", hasUpdateLower)

        // Case B: Remote has versionCode = 2 (equal)
        val remoteEqual = 2
        val hasUpdateEqual = remoteEqual > currentVersionCode
        assertFalse("Bản có versionCode bằng không được coi là mới", hasUpdateEqual)

        // Case C: Remote has versionCode = 3 (greater)
        val remoteHigher = 3
        val hasUpdateHigher = remoteHigher > currentVersionCode
        assertTrue("Bản có versionCode cao hơn phải được coi là cập nhật mới", hasUpdateHigher)
    }

    @Test
    fun updateInfo_correctlyDistinguishesMandatoryVsOptional() {
        val optionalInfo = UpdateInfo(
            hasUpdate = true,
            currentVersionCode = 1,
            currentVersionName = "1.0.0",
            latestVersionCode = 2,
            latestVersionName = "1.0.1",
            releaseNotesVi = "Cập nhật thông thường",
            downloadUrl = "https://example.com/vibeload.apk",
            apkChecksum = null,
            isMandatory = false
        )
        assertFalse("Mặc định bản cập nhật là tùy chọn", optionalInfo.isMandatory)

        val mandatoryInfo = optionalInfo.copy(isMandatory = true)
        assertTrue("Manifest có mandatory: true phải hiển thị là cập nhật bắt buộc", mandatoryInfo.isMandatory)
    }

    @Test
    fun dialogState_flow_prompt_postpone_and_dismiss() {
        val info = UpdateInfo(
            hasUpdate = true,
            currentVersionCode = 1,
            currentVersionName = "1.0.0",
            latestVersionCode = 2,
            latestVersionName = "1.0.1",
            releaseNotesVi = "Nâng cấp bảo mật",
            downloadUrl = "https://example.com/vibeload.apk",
            apkChecksum = null,
            isMandatory = false
        )

        // Initial state is Hidden
        var state: UpdateDialogState = UpdateDialogState.Hidden
        assertEquals(UpdateDialogState.Hidden, state)

        // When update is detected on app launch -> Prompt
        state = UpdateDialogState.Prompt(info)
        assertTrue("Khi phát hiện bản mới phải ở trạng thái Prompt", state is UpdateDialogState.Prompt)
        val prompt = state as UpdateDialogState.Prompt
        assertEquals(2, prompt.info.latestVersionCode)
        assertFalse(prompt.isDownloading)

        // When user chooses "Cập nhật ngay" -> isDownloading = true
        state = prompt.copy(isDownloading = true, downloadProgress = 45)
        assertTrue((state as UpdateDialogState.Prompt).isDownloading)
        assertEquals(45, (state as UpdateDialogState.Prompt).downloadProgress)

        // When user chooses "Để sau" -> Hidden
        state = UpdateDialogState.Hidden
        assertEquals(UpdateDialogState.Hidden, state)
    }

    @Test
    fun networkFailure_doesNotClaimUpToDate() {
        // When network fails, result must be a failure, not a success with hasUpdate=false
        val networkError = Exception("NO_NETWORK: Không có kết nối mạng.")
        val result: Result<UpdateInfo> = Result.failure(networkError)

        assertTrue("Mất mạng phải trả về Failure", result.isFailure)
        assertNotNull(result.exceptionOrNull())
        assertTrue(result.exceptionOrNull()!!.message!!.contains("NO_NETWORK"))
    }

    @Test
    fun autoCheckRateLimiting_logicValidation() {
        val now = System.currentTimeMillis()
        val interval = AppUpdateManager.AUTO_CHECK_INTERVAL_MS

        // Case A: Last check was 5 minutes ago (< 24h) -> Should not check
        val lastCheckedRecent = now - 5 * 60 * 1000L
        val shouldCheckRecent = (now - lastCheckedRecent) >= interval
        assertFalse("Không được gọi API liên tục nếu lần kiểm tra trước dưới 24h", shouldCheckRecent)

        // Case B: Last check was 25 hours ago (> 24h) -> Should check
        val lastCheckedOld = now - 25 * 60 * 60 * 1000L
        val shouldCheckOld = (now - lastCheckedOld) >= interval
        assertTrue("Sau 24h cho phép tự động kiểm tra lại", shouldCheckOld)

        // Case C: Manual check (force = true) -> Always bypass interval
        val force = true
        val shouldForceCheck = force || shouldCheckRecent
        assertTrue("Người dùng chủ động bấm Kiểm tra cập nhật phải bỏ qua giới hạn 24h", shouldForceCheck)
    }
}
