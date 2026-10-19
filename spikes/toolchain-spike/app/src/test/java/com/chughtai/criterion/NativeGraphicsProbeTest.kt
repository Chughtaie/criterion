package com.chughtai.criterion

import android.graphics.Paint
import android.os.Build
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(application = android.app.Application::class)
class NativeGraphicsProbeTest {
    @Test @Config(sdk = [24, 25, 26, 29, 33, 34, 35, 36])
    fun probe() {
        val dir = System.getProperty("robolectric.nativeruntime.fontdir")
        println("PROBE sdk=${Build.VERSION.SDK_INT} fontdir=$dir files=${dir?.let { File(it).list()?.size }} xml=${dir?.let { File(it).list()?.filter { n -> n.endsWith(".xml") } }}")
        try {
            val w = Paint().apply { textSize = 100f }.measureText("Hello")
            println("PROBE sdk=${Build.VERSION.SDK_INT} measureText(Hello)=$w")
        } catch (t: Throwable) {
            var c: Throwable? = t; while (c?.cause != null) c = c.cause
            println("PROBE sdk=${Build.VERSION.SDK_INT} FAIL ${t.javaClass.simpleName} root=${c}")
            c?.stackTrace?.take(8)?.forEach { println("PROBE    at $it") }
        }
    }
}
