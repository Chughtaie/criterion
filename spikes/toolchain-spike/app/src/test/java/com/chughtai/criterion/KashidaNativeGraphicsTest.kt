package com.chughtai.criterion

import android.graphics.Paint
import android.graphics.Typeface
import android.os.Build
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/** Robolectric NATIVE graphics: real Paint/Minikin/HarfBuzz from Robolectric's host native runtime. */
@RunWith(ParameterizedRobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(application = android.app.Application::class)
class KashidaNativeGraphicsTest(private val fontRes: String) {
    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}")
        fun fonts() = listOf(arrayOf("dk-indopak-cv-glyf.ttf"), arrayOf("qul-dk-indopak-glyf.ttf"), arrayOf("dk-org-indopak.otf"))
    }

    private fun tf(): Typeface {
        val f = File.createTempFile("font", fontRes.substringAfterLast('.').let { ".$it" })
        javaClass.getResourceAsStream("/kashida-fonts/$fontRes")!!.use { i -> f.outputStream().use { i.copyTo(it) } }
        return Typeface.createFromFile(f)
    }

    private fun adv(p: Paint, s: String) = p.getRunAdvance(s, 0, s.length, 0, s.length, true, s.length)

    @Test @Config(sdk = [26, 28, 30, 33, 36])
    fun cv01ChangesRunAdvance() {
        val p = Paint().apply { typeface = tf(); textSize = 1000f }  // 1000 px == 1000 font units (upem 1000)
        val words = mapOf("alkitabu" to "الْكِتٰبُ",
            "bismi" to "بِسْمِ")
        for ((k, w) in words) {
            val out = mutableListOf<String>()
            for (v in listOf(0, 1, 2, 3, 6)) {
                p.fontFeatureSettings = if (v == 0) null else "'cv01' $v"
                out += "cv01=$v:${"%.1f".format(adv(p, w))}"
            }
            p.fontFeatureSettings = "cv01=3"   // HarfBuzz '=' syntax also accepted
            out += "cv01=3(eq):${"%.1f".format(adv(p, w))}"
            p.fontFeatureSettings = null
            println("KASHIDA sdk=${Build.VERSION.SDK_INT} font=$fontRes $k ${out.joinToString(" ")}")
        }
    }
}
