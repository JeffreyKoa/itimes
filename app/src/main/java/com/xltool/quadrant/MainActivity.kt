package com.xltool.quadrant

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.viewmodel.compose.viewModel
import com.xltool.quadrant.ui.MainScreen
import com.xltool.quadrant.ui.home.HomeViewModel
import com.xltool.quadrant.ui.theme.XlToolTheme

import com.xltool.quadrant.data.UserPreferences

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val app = application as XlToolApp
        val repo = app.taskRepository
        val userPrefs = UserPreferences(this)

        setContent {
            XlToolTheme {
                val vm: HomeViewModel = viewModel(factory = HomeViewModel.factory(repo, userPrefs))
                MainScreen(
                    homeViewModel = vm,
                    taskRepository = repo
                )
            }
        }
    }
}


