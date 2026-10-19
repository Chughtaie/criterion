package com.chughtai.criterion

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.serialization.Serializable

@Serializable data object Home : NavKey
@Serializable data class Reader(val page: Int) : NavKey

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { MaterialTheme { AppNav() } }
    }
}

@Composable
fun AppNav() {
    val backStack = rememberNavBackStack(Home)
    NavDisplay(
        backStack = backStack,
        onBack = { backStack.removeLastOrNull() },
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator(),
        ),
        entryProvider = entryProvider {
            entry<Home> {
                val vm: HomeViewModel = hiltViewModel()
                val hits by vm.hits.collectAsStateWithLifecycle()
                Scaffold(Modifier.fillMaxSize()) { pad ->
                    Column(Modifier.padding(pad)) {
                        Text("Fajr ${vm.times.fajr}  Asr ${vm.times.asr}  Isha ${vm.times.isha}")
                        Button(onClick = { vm.search("الرحمن") }) { Text("Search") }
                        Text(hits.joinToString())
                        Button(onClick = { backStack.add(Reader(2)) }) { Text("Open page 2") }
                    }
                }
            }
            entry<Reader> { key -> Text("Page #${key.page}") }
        },
    )
}
