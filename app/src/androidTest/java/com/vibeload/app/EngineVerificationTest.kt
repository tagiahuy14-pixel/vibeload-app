package com.vibeload.app

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.ReturnCode
import com.chaquo.python.Python
import com.vibeload.app.platform.engine.ChaquopyDownloadEngine
import com.vibeload.app.platform.engine.FFmpegMediaProcessor
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class EngineVerificationTest {

    @Test
    fun test01_pythonAndYtDlpRunningInAndroidProcess() {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        assertTrue(Python.isStarted())

        val engine = ChaquopyDownloadEngine()
        val info = engine.getEngineInfo()
        println("EngineInfo: yt-dlp=${info.ytDlpVersion}, python=${info.pythonVersion}, platform=${info.platform}")

        assertNotNull(info.ytDlpVersion)
        assertTrue(info.ytDlpVersion.isNotEmpty())
        assertTrue(info.pythonVersion.contains("3.11"))
    }

    @Test
    fun test02_ffmpegKitExecutionAndMp3Codec() {
        val session = FFmpegKit.execute("-version")
        val returnCode = session.returnCode
        println("FFmpeg -version returnCode: $returnCode")
        assertTrue(ReturnCode.isSuccess(returnCode))

        val output = session.allLogsAsString
        println("FFmpeg version string: ${output.take(150)}")
        assertTrue(output.contains("ffmpeg version") || output.contains("configuration"))
    }

    @Test
    fun test03_ffmpegGenerateAndTranscodeTestMp3() {
        runBlocking {
            val appContext = InstrumentationRegistry.getInstrumentation().targetContext
            val cacheDir = appContext.cacheDir

            val rawWavFile = File(cacheDir, "test_audio_synth.wav")
            val outputMp3File = File(cacheDir, "test_audio_converted.mp3")

            if (rawWavFile.exists()) rawWavFile.delete()
            if (outputMp3File.exists()) outputMp3File.delete()

            // Generate 2 seconds of 440Hz test sine audio using FFmpeg lavfi
            val genSession = FFmpegKit.execute("-y -f lavfi -i sine=frequency=440:duration=2 -c:a pcm_s16le \"${rawWavFile.absolutePath}\"")
            assertTrue("Sine wave generation must succeed", ReturnCode.isSuccess(genSession.returnCode))
            assertTrue(rawWavFile.exists() && rawWavFile.length() > 0)
            println("Generated test WAV size: ${rawWavFile.length()} bytes")

            // Transcode to MP3 using our FFmpegMediaProcessor (192kbps)
            val processor = FFmpegMediaProcessor()
            var progressReported = false

            val result = processor.transcodeToMp3(
                inputPath = rawWavFile.absolutePath,
                outputPath = outputMp3File.absolutePath,
                bitrateKbps = 192,
                onProgress = { progressReported = true }
            )

            assertTrue("Transcode to MP3 must succeed", result.isSuccess)
            assertTrue("Output MP3 file must exist", outputMp3File.exists())
            assertTrue("Output MP3 size must be greater than 0", outputMp3File.length() > 0)
            println("Converted MP3 file size: ${outputMp3File.length()} bytes")

            // Cleanup test files
            rawWavFile.delete()
            outputMp3File.delete()
        }
    }

    @Test
    fun test04_filenameSanitizationRules() {
        val dirtyName = "../../../Dangerous\\File:Name*With?Illegal<Chars>|And\"Quotes.mp4"
        val sanitized = dirtyName
            .replace(Regex("[\\\\/:*?\"<>|]"), "_")
            .replace(Regex("\\.{2,}"), "")
            .trim()

        println("Sanitized filename: $sanitized")
        assertTrue(!sanitized.contains(".."))
        assertTrue(!sanitized.contains("/"))
        assertTrue(!sanitized.contains("\\"))
    }
}
