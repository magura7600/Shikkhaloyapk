package com.example

import org.junit.Test
import com.yausername.youtubedl_android.mapper.VideoInfo
import com.yausername.youtubedl_android.mapper.VideoFormat

class DumpTest {
    @Test
    fun dump() {
        val infoClass = VideoInfo::class.java
        for (field in infoClass.declaredFields) {
            println("INFO_FIELD: ${field.name} type ${field.type}")
        }
        val formatClass = VideoFormat::class.java
        for (field in formatClass.declaredFields) {
            println("FORMAT_FIELD: ${field.name} type ${field.type}")
        }
    }
}
