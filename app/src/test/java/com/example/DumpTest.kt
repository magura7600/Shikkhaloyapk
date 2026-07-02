package com.example

import org.junit.Test
import java.net.URL
import java.net.HttpURLConnection
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString

class DumpTest {
    @Test
    fun dump() {
        try {
            val course = CourseItem(
                id = java.util.UUID.randomUUID().toString(),
                title = "Test JVM Post",
                description = "Testing Supabase Insert from JVM",
                paymentDetails = "Please send money...|||ROUTINE_DATA:v2;https://banner.com;01/07/2026;31/07/2026;someRoutineUrl",
                isQuarterOn = false,
                quarters = listOf(CourseQuarter(name = "Quarter 1", price = "500", startDate = "01/07/2026", endDate = "31/07/2026"))
            )
            
            val json = Json { encodeDefaults = true }
            val jsonStr = json.encodeToString(course)
            println("SERIALIZED JSON: $jsonStr")
            
            val url = URL("https://gputlynskpbbiexbpphj.supabase.co/rest/v1/courses")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("apikey", "sb_publishable_NwiSPi0Rl4VAf_B2v5Fp6g_KgwTb_Ol")
            conn.setRequestProperty("Authorization", "Bearer sb_publishable_NwiSPi0Rl4VAf_B2v5Fp6g_KgwTb_Ol")
            conn.setRequestProperty("Content-Type", "application/json")
            conn.setRequestProperty("Prefer", "return=representation")
            conn.doOutput = true
            
            conn.outputStream.use { it.write(jsonStr.toByteArray()) }
            
            val responseCode = conn.responseCode
            println("RESPONSE CODE: $responseCode")
            if (responseCode >= 200 && responseCode < 300) {
                val text = conn.inputStream.bufferedReader().use { it.readText() }
                println("SUCCESS RESPONSE: $text")
            } else {
                val err = conn.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
                println("ERROR RESPONSE: $err")
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
