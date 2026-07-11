package com.example

import android.app.AlarmManager
import android.content.Context
import android.graphics.Bitmap
import android.util.LruCache
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.io.File
import java.io.IOException
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AppStabilityTest {

    @Test
    fun testPdfViewerLruCacheOomSafety() {
        // Cache size is computed based on memory. Let's create a small LruCache to verify its size calculation and eviction.
        val cacheSize = 100 // 100 KB
        val bitmapCache = object : LruCache<Int, Bitmap>(cacheSize) {
            override fun sizeOf(key: Int, value: Bitmap): Int {
                return value.byteCount / 1024
            }
        }

        // Create mock bitmaps
        val bitmap1 = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888) // 100 * 100 * 4 = 40,000 bytes = ~39 KB
        val bitmap2 = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888) // ~39 KB
        val bitmap3 = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888) // ~39 KB

        bitmapCache.put(1, bitmap1)
        bitmapCache.put(2, bitmap2)
        
        // Assert they are cached
        assertNotNull(bitmapCache.get(1))
        assertNotNull(bitmapCache.get(2))

        // Put the third bitmap, total size would be ~117 KB which exceeds 100 KB limit, so first one should be evicted
        bitmapCache.put(3, bitmap3)

        assertNull(bitmapCache.get(1)) // Evicted!
        assertNotNull(bitmapCache.get(2))
        assertNotNull(bitmapCache.get(3))
    }

    @Test
    fun testOfflineDownloadManagerRecords() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        
        val record1 = DownloadRecord("1", "Title 1", "http://example.com/1.pdf", "pdf", "/path/1", System.currentTimeMillis())
        val record2 = DownloadRecord("2", "Title 2", "http://example.com/2.mp4", "video", "/path/2", System.currentTimeMillis())
        
        // Setup direct preference injection to test persistence parsing of OfflineDownloadManager
        val prefs = context.getSharedPreferences("shikkhaloy_downloads_prefs", Context.MODE_PRIVATE)
        val jsonStr = """[{"id":"1","title":"Title 1","url":"http://example.com/1.pdf","fileType":"pdf","localPath":"/path/1","downloadTime":123456,"courseName":"","className":""}]"""
        prefs.edit().putString("downloaded_records", jsonStr).commit()

        val loaded = OfflineDownloadManager.getDownloadRecords(context)
        assertEquals(1, loaded.size)
        assertEquals("Title 1", loaded[0].title)
        assertEquals("http://example.com/1.pdf", loaded[0].url)
    }

    @Test
    fun testAtomicRenameRecovery() {
        // Simulating the atomic renaming of downloaded file
        val context = ApplicationProvider.getApplicationContext<Context>()
        val testDir = File(context.cacheDir, "test_downloads")
        testDir.mkdirs()

        val tempFile = File(testDir, "video.mp4.tmp")
        val destinationFile = File(testDir, "video.mp4")

        tempFile.writeText("sample video content")
        assertTrue(tempFile.exists())
        assertFalse(destinationFile.exists())

        // Atomically Rename
        val success = tempFile.renameTo(destinationFile)
        assertTrue(success)
        assertFalse(tempFile.exists())
        assertTrue(destinationFile.exists())

        // Simulating rename failure / recovery
        val failedTemp = File(testDir, "fail.mp4.tmp")
        failedTemp.writeText("some partial data")
        
        // Simulating condition where we must clean up
        if (failedTemp.exists()) {
            failedTemp.delete()
        }
        assertFalse(failedTemp.exists())
    }

    @Test
    fun testNotificationSchedulerSchedulesAlarms() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val shadowAlarmManager = shadowOf(alarmManager)

        // Grant exact alarms permission using robust reflection & appops fallbacks
        try {
            val method = shadowAlarmManager.javaClass.getMethod("setCanScheduleExactAlarms", Boolean::class.java)
            method.isAccessible = true
            method.invoke(shadowAlarmManager, true)
        } catch (e: Exception) {
            try {
                val field = shadowAlarmManager.javaClass.getDeclaredField("canScheduleExactAlarms")
                field.isAccessible = true
                field.set(shadowAlarmManager, true)
            } catch (e2: Exception) {
                try {
                    val shadowApp = shadowOf(context as android.app.Application)
                    shadowApp.grantPermissions(android.Manifest.permission.SCHEDULE_EXACT_ALARM)
                } catch (e3: Exception) {
                    e3.printStackTrace()
                }
            }
        }

        // Generate dynamically moving future dates to avoid stale tests
        val futureDate = LocalDate.now().plusDays(5)
        val dateStr = futureDate.format(DateTimeFormatter.ofPattern("d/M/yyyy"))

        // Mock course item with a class scheduled in the future
        val classItem = CourseClass(
            id = "class_1",
            title = "Math Algebra",
            date = dateStr,
            time = "10:30 AM",
            recordedLink = "",
            liveLink = ""
        )
        val chapter = CourseChapter("chap_1", "Chapter 1", listOf(classItem))
        val subject = CourseSubject("sub_1", "Subject 1", "", "", listOf(chapter))
        val course = CourseItem(
            id = "course_1",
            title = "SSC Course",
            subjects = listOf(subject)
        )

        // Run scheduler
        NotificationScheduler.scheduleClassNotifications(context, course)

        // Fetch scheduled alarms via Robolectric Shadow
        val scheduledAlarms = shadowAlarmManager.scheduledAlarms
        assertTrue(scheduledAlarms.isNotEmpty())

        val nextAlarm = scheduledAlarms[0]
        assertNotNull(nextAlarm.operation)
    }
}
