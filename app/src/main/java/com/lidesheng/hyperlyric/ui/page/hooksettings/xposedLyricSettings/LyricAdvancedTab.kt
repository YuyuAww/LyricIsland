package com.lidesheng.hyperlyric.ui.page.hooksettings.xposedLyricSettings

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.lidesheng.hyperlyric.ui.utils.pageScrollModifiers
import top.yukonga.miuix.kmp.basic.ScrollBehavior

@Composable
fun LyricAdvancedTab(
    lazyListState: LazyListState,
    topAppBarScrollBehavior: ScrollBehavior,
    contentPadding: PaddingValues,
    lyricMode: Int,
    gradientStyle: Boolean,
    onGradientStyleChange: (Boolean) -> Unit,
    syllableRelative: Boolean,
    onSyllableRelativeChange: (Boolean) -> Unit,
    syllableHighlight: Boolean,
    onSyllableHighlightChange: (Boolean) -> Unit,
    wordMotionEnabled: Boolean,
    onWordMotionEnabledChange: (Boolean) -> Unit,
    wordMotionCjkLift: Float,
    onWordMotionCjkLiftClick: () -> Unit,
    wordMotionCjkWave: Float,
    onWordMotionCjkWaveClick: () -> Unit,
    wordMotionLatinLift: Float,
    onWordMotionLatinLiftClick: () -> Unit,
    wordMotionLatinWave: Float,
    onWordMotionLatinWaveClick: () -> Unit,
    nextLyricLine: Boolean,
    onNextLyricLineChange: (Boolean) -> Unit
) {
    LazyColumn(
        state = lazyListState,
        modifier = Modifier.pageScrollModifiers(
            enableScrollEndHaptic = true,
            showTopAppBar = true,
            topAppBarScrollBehavior = topAppBarScrollBehavior
        ),
        contentPadding = contentPadding,
    ) {
        advancedSections(
            lyricMode = lyricMode,
            gradientStyle = gradientStyle,
            onGradientStyleChange = onGradientStyleChange,
            syllableRelative = syllableRelative,
            onSyllableRelativeChange = onSyllableRelativeChange,
            syllableHighlight = syllableHighlight,
            onSyllableHighlightChange = onSyllableHighlightChange,
            wordMotionEnabled = wordMotionEnabled,
            onWordMotionEnabledChange = onWordMotionEnabledChange,
            wordMotionCjkLift = wordMotionCjkLift,
            onWordMotionCjkLiftClick = onWordMotionCjkLiftClick,
            wordMotionCjkWave = wordMotionCjkWave,
            onWordMotionCjkWaveClick = onWordMotionCjkWaveClick,
            wordMotionLatinLift = wordMotionLatinLift,
            onWordMotionLatinLiftClick = onWordMotionLatinLiftClick,
            wordMotionLatinWave = wordMotionLatinWave,
            onWordMotionLatinWaveClick = onWordMotionLatinWaveClick,
            nextLyricLine = nextLyricLine,
            onNextLyricLineChange = onNextLyricLineChange
        )
    }
}
