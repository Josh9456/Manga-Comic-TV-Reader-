package com.mangatv.reader

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.mangatv.reader.ui.components.TvStoragePermissionPrompt
import com.mangatv.reader.ui.navigation.AppNavGraph
import com.mangatv.reader.ui.theme.MangaTVTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MangaTVTheme {
                val navController = rememberNavController()
                Box(modifier = Modifier.fillMaxSize()) {
                    AppNavGraph(navController = navController)
                    TvStoragePermissionPrompt()
                }
            }
        }
    }
}
