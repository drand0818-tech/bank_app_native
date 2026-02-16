package com.example.yonosbi

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.yonosbi.ui.screens.HomeScreen
import com.example.yonosbi.ui.screens.UserDetailsFormScreen
import com.example.yonosbi.ui.theme.YonosbiTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            YonosbiTheme {
                AppNavigation()
            }
        }
    }
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    
    NavHost(
        navController = navController,
        startDestination = "home"
    ) {
        composable("home") {
            HomeScreen(navController = navController)
        }
        composable("user_details_form") {
            UserDetailsFormScreen()
        }
    }
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    YonosbiTheme {
        val navController = rememberNavController()
        HomeScreen(navController = navController)
    }
}