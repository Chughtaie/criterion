package com.chughtai.criterion

import android.content.Context
import androidx.hilt.work.HiltWorkerFactory
import androidx.test.core.app.ApplicationProvider
import androidx.work.ListenableWorker
import androidx.work.testing.TestListenableWorkerBuilder
import androidx.work.workDataOf
import com.chughtai.criterion.audio.PlaybackService
import com.chughtai.criterion.core.content.QuranContentDb
import com.chughtai.criterion.prayer.PrayerCalculator
import com.chughtai.criterion.work.AudioDownloadWorker
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import kotlinx.coroutines.runBlocking
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.OkHttpClient
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import javax.inject.Inject

@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], application = HiltTestApplication::class)
class HiltRuntimeTest {
    @get:Rule val hilt = HiltAndroidRule(this)
    @Inject lateinit var okHttp: OkHttpClient
    @Inject lateinit var db: QuranContentDb
    @Inject lateinit var workerFactory: HiltWorkerFactory
    @Inject lateinit var prayer: PrayerCalculator

    @Before fun inject() = hilt.inject()

    @Test fun viewModel_graph_runs_ftsQuery_fromAppMergedAsset() {
        val vm = HomeViewModel(prayer, db)
        vm.search("الرَّحْمَٰنِ")
        // viewModelScope runs on the Robolectric main looper: pump it instead of runBlocking (which deadlocks)
        var n = 0
        while (vm.hits.value.isEmpty() && n++ < 500) {
            org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle(); Thread.sleep(10)
        }
        println("VM hits=${vm.hits.value.size} first=${vm.hits.value.firstOrNull()} times=${vm.times}")
        assertThat(vm.hits.value).hasSize(45)
    }

    @Test fun mediaLibraryService_createsSession_withInjectedOkHttp() {
        val controller = Robolectric.buildService(PlaybackService::class.java).create()
        val svc = controller.get()
        assertThat(svc.okHttp).isSameInstanceAs(okHttp)
        val session = svc.onGetSession(
            androidx.media3.session.MediaSession.ControllerInfo.createTestOnlyControllerInfo(
                "test", 0, 0, 0, 0, false, android.os.Bundle.EMPTY, false))
        assertThat(session).isNotNull()
        controller.destroy()
    }

    @Test fun hiltWorker_assistedInjection_downloads() {
        val server = MockWebServer()
        server.enqueue(MockResponse.Builder().code(200).body("ID3-fake-mp3-bytes").build())
        server.start()
        try {
            val ctx: Context = ApplicationProvider.getApplicationContext()
            val worker = TestListenableWorkerBuilder<AudioDownloadWorker>(ctx)
                .setWorkerFactory(workerFactory)
                .setInputData(workDataOf(AudioDownloadWorker.KEY_URL to server.url("/a.mp3").toString()))
                .build()
            val result = runBlocking { worker.doWork() }
            println("worker result=$result")
            assertThat(result).isInstanceOf(ListenableWorker.Result.Success::class.java)
            assertThat(result.outputData.getInt(AudioDownloadWorker.KEY_BYTES, -1)).isEqualTo(18)
        } finally { server.close() }
    }
}
