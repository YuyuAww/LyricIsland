package com.lidesheng.hyperlyric.ui.page.hooksettings.xposedLyricSettings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.lidesheng.hyperlyric.R
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.HorizontalDivider
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.preference.SwitchPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme

fun LazyListScope.advancedSections(
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
    disableTranslation: Boolean,
    onDisableTranslationChange: (Boolean) -> Unit,
    translationOnly: Boolean,
    onTranslationOnlyChange: (Boolean) -> Unit,
    swapTranslation: Boolean,
    onSwapTranslationChange: (Boolean) -> Unit,
    nextLyricLine: Boolean,
    onNextLyricLineChange: (Boolean) -> Unit
) {
    item {
        Column {
            SmallTitle(text = stringResource(id = R.string.title_verbatim_lyric))
            Card(modifier = Modifier.padding(horizontal = 12.dp).padding(bottom = 12.dp).fillMaxWidth()) {
                Column {
                    SwitchPreference(title = stringResource(id = R.string.title_gradient_progress), checked = gradientStyle, onCheckedChange = onGradientStyleChange)
                    SwitchPreference(
                        title = stringResource(id = R.string.title_syllable_relative),
                        summary = stringResource(id = R.string.summary_syllable_relative),
                        checked = syllableRelative,
                        onCheckedChange = onSyllableRelativeChange
                    )
                    SwitchPreference(title = stringResource(id = R.string.title_syllable_highlight), checked = syllableHighlight, onCheckedChange = onSyllableHighlightChange)
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    SwitchPreference(
                        title = stringResource(id = R.string.title_word_motion),
                        checked = wordMotionEnabled,
                        onCheckedChange = onWordMotionEnabledChange
                    )
                    AnimatedVisibility(visible = wordMotionEnabled) {
                        Column {
                            ArrowPreference(
                                title = stringResource(id = R.string.title_word_motion_cjk_lift),
                                onClick = onWordMotionCjkLiftClick,
                                endActions = {
                                    Text(
                                        String.format("%.2f", wordMotionCjkLift),
                                        fontSize = MiuixTheme.textStyles.body2.fontSize,
                                        color = MiuixTheme.colorScheme.onSurfaceVariantActions
                                    )
                                }
                            )
                            ArrowPreference(
                                title = stringResource(id = R.string.title_word_motion_cjk_wave),
                                endActions = {
                                    Text(
                                        String.format("%.2f", wordMotionCjkWave),
                                        fontSize = MiuixTheme.textStyles.body2.fontSize,
                                        color = MiuixTheme.colorScheme.onSurfaceVariantActions
                                    )
                                },
                                onClick = onWordMotionCjkWaveClick
                            )
                            ArrowPreference(
                                title = stringResource(id = R.string.title_word_motion_latin_lift),
                                endActions = {
                                    Text(
                                        String.format("%.2f", wordMotionLatinLift),
                                        fontSize = MiuixTheme.textStyles.body2.fontSize,
                                        color = MiuixTheme.colorScheme.onSurfaceVariantActions
                                    )
                                },
                                onClick = onWordMotionLatinLiftClick
                            )
                            ArrowPreference(
                                title = stringResource(id = R.string.title_word_motion_latin_wave),
                                endActions = {
                                    Text(
                                        String.format("%.2f", wordMotionLatinWave),
                                        fontSize = MiuixTheme.textStyles.body2.fontSize,
                                        color = MiuixTheme.colorScheme.onSurfaceVariantActions
                                    )
                                },
                                onClick = onWordMotionLatinWaveClick
                            )
                        }
                    }
                }
            }
        }
    }

    item {
        // 由于现在仅剩 Lyricon 源，supportsNextLyricLine 简化为只判断 lyricMode
        val supportsNextLyricLine = lyricMode == 0
        val translationControlsEnabled = !supportsNextLyricLine || !nextLyricLine
        Column {
            SmallTitle(text = stringResource(id = R.string.title_translation))
            Card(modifier = Modifier.padding(horizontal = 12.dp).padding(bottom = 12.dp).fillMaxWidth()) {
                Column {
                    AnimatedVisibility(visible = supportsNextLyricLine) {
                        SwitchPreference(
                            title = stringResource(id = R.string.title_next_lyric_line),
                            summary = stringResource(id = R.string.summary_next_lyric_line),
                            checked = nextLyricLine,
                            onCheckedChange = onNextLyricLineChange
                        )
                    }
                    SwitchPreference(
                        title = stringResource(id = R.string.title_disable_translation),
                        checked = disableTranslation,
                        onCheckedChange = onDisableTranslationChange,
                        enabled = translationControlsEnabled
                    )
                    SwitchPreference(
                        title = stringResource(id = R.string.title_translation_only),
                        checked = translationOnly,
                        onCheckedChange = onTranslationOnlyChange,
                        enabled = translationControlsEnabled
                    )
                    SwitchPreference(
                        title = stringResource(id = R.string.title_swap_translation),
                        checked = swapTranslation,
                        onCheckedChange = onSwapTranslationChange,
                        enabled = translationControlsEnabled
                    )
                }
            }
        }
    }
}
