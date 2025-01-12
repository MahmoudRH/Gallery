/*
 * SPDX-FileCopyrightText: 2023 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.feature_node.presentation.mediaview

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.media3.exoplayer.ExoPlayer
import androidx.navigation.NavController
import com.dot.gallery.core.Constants
import com.dot.gallery.core.Constants.Animation.enterAnimation
import com.dot.gallery.core.Constants.Animation.exitAnimation
import com.dot.gallery.core.Constants.DEFAULT_LOW_VELOCITY_SWIPE_DURATION
import com.dot.gallery.core.Constants.HEADER_DATE_FORMAT
import com.dot.gallery.core.Constants.Target.TARGET_TRASH
import com.dot.gallery.core.presentation.components.media.MediaPreviewComponent
import com.dot.gallery.core.presentation.components.media.VideoPlayerController
import com.dot.gallery.feature_node.domain.model.Media
import com.dot.gallery.feature_node.presentation.library.trashed.components.TrashedViewBottomBar
import com.dot.gallery.feature_node.presentation.mediaview.components.MediaViewAppBar
import com.dot.gallery.feature_node.presentation.mediaview.components.MediaViewBottomBar
import com.dot.gallery.feature_node.presentation.MediaViewModel
import com.dot.gallery.feature_node.presentation.util.getDate

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MediaViewScreen(
    navController: NavController,
    paddingValues: PaddingValues,
    mediaId: Long,
    target: String? = null,
    viewModel: MediaViewModel
) {
    val runtimeMediaId = remember { mutableStateOf(mediaId) }
    val state by remember {
        viewModel.photoState
    }
    val initialPage = remember { state.media.indexOfFirst { it.id == mediaId } }
    val pagerState = rememberPagerState(initialPage = if (initialPage == -1) 0 else initialPage)
    val scrollEnabled = remember { mutableStateOf(true) }

    val currentDate = remember { mutableStateOf("") }
    val currentMedia = remember { mutableStateOf<Media?>(null) }

    val showUI = remember { mutableStateOf(true) }
    val context = LocalContext.current
    val window = remember { (context as Activity).window }
    val windowInsetsController =
        remember { WindowCompat.getInsetsController(window, window.decorView) }

    val showUIListener: () -> Unit = remember {
        {
            if (showUI.value)
                windowInsetsController.show(WindowInsetsCompat.Type.systemBars())
            else
                windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
        }
    }

    val result = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult(),
        onResult = {}
    )
    val lastIndex = remember { mutableStateOf(-1) }
    val updateContent: (Int) -> Unit = remember {
        { page ->
            if (state.media.isNotEmpty()) {
                val index = if (page == -1) 0 else page
                if (lastIndex.value != -1)
                    runtimeMediaId.value = state.media[lastIndex.value].id
                currentDate.value = state.media[index].timestamp.getDate(HEADER_DATE_FORMAT)
                currentMedia.value = state.media[index]
            }
        }
    }

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }.collect { page ->
            updateContent(page)
        }
    }

    LaunchedEffect(state.media) {
        if (state.media.isEmpty()) {
            navController.navigateUp()
        } else {
            updateContent(state.media.indexOfFirst { it.id == runtimeMediaId.value })
        }
    }
    Box(
        modifier = Modifier
            .background(Color.Black)
            .fillMaxSize()
    ) {
        HorizontalPager(
            state = pagerState,
            pageCount = state.media.size,
            userScrollEnabled = scrollEnabled.value,
            flingBehavior = PagerDefaults.flingBehavior(
                state = pagerState,
                lowVelocityAnimationSpec = tween(
                    easing = LinearEasing,
                    durationMillis = DEFAULT_LOW_VELOCITY_SWIPE_DURATION
                )
            ),
            pageSpacing = 16.dp,
            modifier = Modifier
                .background(Color.Black)
                .fillMaxSize()
        ) { index ->
            MediaPreviewComponent(
                media = state.media[index],
                scrollEnabled = scrollEnabled,
                playWhenReady = index == pagerState.currentPage,
                onItemClick = {
                    showUI.value = !showUI.value
                    showUIListener()
                }
            ) { player: ExoPlayer, currentTime: MutableState<Long>, totalTime: Long, buffer: Int, playToggle: () -> Unit ->
                AnimatedVisibility(
                    visible = showUI.value,
                    enter = enterAnimation(Constants.DEFAULT_TOP_BAR_ANIMATION_DURATION),
                    exit = exitAnimation(Constants.DEFAULT_TOP_BAR_ANIMATION_DURATION),
                    modifier = Modifier.fillMaxSize()
                ) {
                    VideoPlayerController(
                        paddingValues = paddingValues,
                        player = player,
                        currentTime = currentTime,
                        totalTime = totalTime,
                        buffer = buffer,
                        playToggle = playToggle
                    )
                }
            }
        }
        MediaViewAppBar(
            showUI = showUI.value,
            currentDate = currentDate.value,
            paddingValues = paddingValues
        ) {
            navController.navigateUp()
        }
        if (target == TARGET_TRASH) {
            TrashedViewBottomBar(
                handler = viewModel.handler,
                showUI = showUI.value,
                paddingValues = paddingValues,
                currentMedia = currentMedia.value,
                currentIndex = pagerState.currentPage,
                result = result,
            ) {
                lastIndex.value = it
            }
        } else {
            MediaViewBottomBar(
                handler = viewModel.handler,
                showUI = showUI.value,
                paddingValues = paddingValues,
                currentMedia = currentMedia.value,
                currentIndex = pagerState.currentPage,
                result = result,
            ) {
                lastIndex.value = it
            }
        }
    }

}