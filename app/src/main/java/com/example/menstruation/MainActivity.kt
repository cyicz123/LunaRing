package com.example.menstruation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.navigation.compose.rememberNavController
import com.example.menstruation.ui.navigation.NavGraph
import com.example.menstruation.ui.theme.MenstruationTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MenstruationTheme {
                val navController = rememberNavController()
                NavGraph(navController = navController)
            }
        }
    }
}