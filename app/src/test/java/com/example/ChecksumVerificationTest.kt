package com.example

import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.io.FileOutputStream
import java.io.FileInputStream
import java.security.MessageDigest

class ChecksumVerificationTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private fun calculateSha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        FileInputStream(file).use { inputStream ->
            val buffer = ByteArray(8192)
            var bytesRead: Int
            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                digest.update(buffer, 0, bytesRead)
            }
        }
        val hashBytes = digest.digest()
        val hexString = java.lang.StringBuilder()
        for (b in hashBytes) {
            val hex = Integer.toHexString(0xff and b.toInt())
            if (hex.length == 1) hexString.append('0')
            hexString.append(hex)
        }
        return hexString.toString()
    }

    @Test
    fun testSha256_forKnownContent_returnsCorrectHash() {
        // Arrange - Create a file with known content "hello"
        val file = tempFolder.newFile("test_file.txt")
        FileOutputStream(file).use { out ->
            out.write("hello".toByteArray(Charsets.UTF_8))
        }

        // The expected SHA-256 hash of "hello" (calculated using standard tools):
        // 2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824
        val expectedHash = "2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824"

        // Act
        val calculated = calculateSha256(file)

        // Assert
        assertEquals(expectedHash, calculated)
    }

    @Test
    fun testSha256_forEmptyContent_returnsCorrectHash() {
        // Arrange - Empty file
        val file = tempFolder.newFile("empty_file.txt")

        // Expected SHA-256 hash of empty content:
        // e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855
        val expectedHash = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855"

        // Act
        val calculated = calculateSha256(file)

        // Assert
        assertEquals(expectedHash, calculated)
    }
}
