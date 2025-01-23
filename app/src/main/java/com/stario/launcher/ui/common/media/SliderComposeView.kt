/*
 * Copyright (C) 2025 Răzvan Albu
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>
 */

package com.stario.launcher.ui.common.media

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SliderDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.AbstractComposeView
import androidx.compose.ui.unit.dp
import me.saket.squiggles.SquigglySlider

class SliderComposeView
@JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) :
    AbstractComposeView(context, attrs) {

    var progress = mutableFloatStateOf(0.5f)
    var isPlaying = mutableStateOf(false)
    var listener: OnProgressChanged? = null

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        SquigglySlider(
            value = progress.floatValue,
            colors = SliderDefaults.colors(
                thumbColor = Color.White,
                activeTickColor = Color.White,
                activeTrackColor = Color.White,
                inactiveTrackColor = Color(255, 255, 255, 120),
            ),
            onValueChange = {
                listener?.changing()
                progress.floatValue = it
            },
            onValueChangeFinished = {
                listener?.progressChanged(progress.floatValue)
            },
            squigglesSpec = SquigglySlider.SquigglesSpec(
                strokeWidth = 2.dp,
                wavelength = 22.dp,
                amplitude = if (isPlaying.value) 2.dp else 0.dp,
            ),
        )
    }

    interface OnProgressChanged {
        fun changing()
        fun progressChanged(progress: Float)
    }
}