package com.example.scrapsetu

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.navigation.compose.rememberNavController
import com.example.scrapsetu.ui.theme.ScrapSetuTheme
import com.example.scrapsetu.view.navigation.NavGraph
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ScrapSetuTheme {
                val navController = rememberNavController()
                NavGraph(navController = navController)
            }
        }
    }
}