package com.xltool.quadrant.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.xltool.quadrant.data.Quadrant
import com.xltool.quadrant.data.TaskRepository
import com.xltool.quadrant.ui.components.DraggableFab
import com.xltool.quadrant.ui.components.DraggableSearchButton
import com.xltool.quadrant.ui.home.HomeViewModel
import com.xltool.quadrant.ui.home.QuadrantScreen
import com.xltool.quadrant.ui.navigation.BottomNavBar
import com.xltool.quadrant.ui.navigation.BottomNavTab
import com.xltool.quadrant.ui.profile.ProfileScreen
import com.xltool.quadrant.ui.review.ReviewScreen
import com.xltool.quadrant.ui.search.TaskSearchScreen

@Composable
fun MainScreen(
    homeViewModel: HomeViewModel,
    taskRepository: TaskRepository
) {
    var currentTab by rememberSaveable { mutableStateOf(BottomNavTab.HOME) }
    var showSearchScreen by rememberSaveable { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        // 主界面
        Column(modifier = Modifier.fillMaxSize()) {
            // 主内容区域
            Box(modifier = Modifier.weight(1f)) {
                when (currentTab) {
                    BottomNavTab.HOME -> QuadrantScreen(
                        vm = homeViewModel,
                        onOpenSearch = { showSearchScreen = true }
                    )
                    BottomNavTab.REVIEW -> ReviewScreen(repository = taskRepository)
                    BottomNavTab.PROFILE -> ProfileScreen()
                }
            }

            // 底部导航栏
            BottomNavBar(
                currentTab = currentTab,
                onTabSelected = { currentTab = it },
                modifier = Modifier.fillMaxWidth()
            )
        }

        // 可拖动悬浮按钮 - 仅在首页且非查询页面时显示
        if (currentTab == BottomNavTab.HOME && !showSearchScreen) {
            // 搜索按钮容器 - 有底部padding避开导航栏
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 56.dp)
            ) {
                // 搜索按钮 - 默认在"今日最重要的一件事"模块右下角位置
                DraggableSearchButton(
                    onClick = { showSearchScreen = true },
                    initialOffsetX = -16f,
                    initialOffsetY = 180f
                )
            }
            
            // 添加任务按钮容器 - 无底部padding，允许悬浮在导航栏上方
            Box(
                modifier = Modifier.fillMaxSize()
            ) {
                // 添加任务按钮 - 位于底部导航栏的"复盘"和"我的"中间
                // X: -60dp 大约在屏幕右侧1/6处（复盘和我的之间）
                // Y: -28dp 垂直居中于导航栏（导航栏高度56dp的一半）
                DraggableFab(
                    onClick = {
                        // 点击悬浮球打开任务创建界面
                        // 默认创建到重要紧急象限
                        homeViewModel.openCreate(Quadrant.IMPORTANT_URGENT)
                    },
                    initialOffsetX = -60f,
                    initialOffsetY = -28f
                )
            }
        }

        // 任务查询页面（全屏覆盖）
        AnimatedVisibility(
            visible = showSearchScreen,
            enter = slideInHorizontally { it },
            exit = slideOutHorizontally { it }
        ) {
            TaskSearchScreen(
                repository = taskRepository,
                onNavigateBack = { showSearchScreen = false },
                onEditTask = { taskId ->
                    showSearchScreen = false
                    homeViewModel.openEdit(taskId)
                }
            )
        }
    }
}
