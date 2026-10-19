package com.chughtai.criterion

import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.okhttp.OkHttpDataSource
import com.google.common.truth.Truth.assertThat
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okio.Buffer
import okhttp3.OkHttp
import okhttp3.OkHttpClient
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Media3 1.11.1's datasource-okhttp is compiled against OkHttp 4.12; prove it runs on OkHttp 5.5.0. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], application = android.app.Application::class)
@OptIn(UnstableApi::class)
class OkHttp5Media3Test {
    @Test fun okHttpDataSource_fullAndRangeReads_onOkHttp5() {
        println("OkHttp runtime version = ${OkHttp.VERSION}")
        assertThat(OkHttp.VERSION).startsWith("5.")
        val body = ByteArray(65_536) { (it % 251).toByte() }
        val server = MockWebServer()
        server.enqueue(MockResponse.Builder().code(200).body(Buffer().write(body)).build())
        server.enqueue(MockResponse.Builder().code(206)
            .addHeader("Content-Range", "bytes 1000-1499/65536")
            .body(Buffer().write(body, 1000, 500)).build())
        server.start()
        try {
            val ds = OkHttpDataSource.Factory(OkHttpClient()).setUserAgent("Criterion-test").createDataSource()
            val uri = Uri.parse(server.url("/001001.mp3").toString())
            val len = ds.open(DataSpec(uri))
            val all = ds.readAll()
            ds.close()
            assertThat(len).isEqualTo(65_536L)
            assertThat(all).isEqualTo(body)

            val ds2 = OkHttpDataSource.Factory(OkHttpClient()).createDataSource()
            ds2.open(DataSpec.Builder().setUri(uri).setPosition(1000).setLength(500).build())
            val part = ds2.readAll()
            ds2.close()
            assertThat(part).isEqualTo(body.copyOfRange(1000, 1500))
            server.takeRequest()
            val r2 = server.takeRequest()
            println("range header = ${r2.headers["Range"]}, UA(1st) ok")
            assertThat(r2.headers["Range"]).isEqualTo("bytes=1000-1499")
        } finally { server.close() }
    }

    private fun OkHttpDataSource.readAll(): ByteArray {
        val out = java.io.ByteArrayOutputStream(); val buf = ByteArray(8192)
        while (true) { val n = read(buf, 0, buf.size); if (n == -1) break; out.write(buf, 0, n) }
        return out.toByteArray()
    }
}
