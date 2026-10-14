/*
 * Copyright 2024 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example.platform.ui.appwidgets.glance.layout.toolbars.layout

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.glance.ColorFilter
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.Action
import androidx.glance.action.clickable
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.height
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.semantics.contentDescription
import androidx.glance.semantics.semantics
import androidx.glance.unit.ColorProvider

/**
 * A rectangular button displaying the provided icon on a background of
 * provided corner radius and colors.
 *
 * @param imageProvider icon to be displayed at center of the button
 * @param onClick [Action] to be performed on click of button
 * @param roundedCornerShape type of rounding to be applied to the button
 * @param contentDescription description about the button that can be used by the accessibility
 *                           services
 * @param iconSize size of the icon displayed at center of the button
 * @param modifier the modifier to be applied to this button.
 * @param backgroundColor background color for the button
 * @param contentColor color of the icon displayed at center of the button
 */
@Composable
fun RectangularIconButton(
  imageProvider: ImageProvider,
  onClick: Action,
  roundedCornerShape: RoundedCornerShape,
  contentDescription: String,
  iconSize: Dp,
  modifier: GlanceModifier,
  backgroundColor: ColorProvider = GlanceTheme.colors.primary,
  contentColor: ColorProvider = GlanceTheme.colors.onPrimary,
) {
  Box(
    contentAlignment = Alignment.Center,
    modifier = modifier
      .background(backgroundColor)
      .cornerRadius(roundedCornerShape.cornerRadius)
      .semantics { this.contentDescription = contentDescription }
      .clickable(onClick)
  ) {
    Image(
      provider = imageProvider,
      contentDescription = null,
      colorFilter = ColorFilter.tint(contentColor),
      modifier = GlanceModifier.size(iconSize)
    )
  }
}

/**
 * A fixed height pill-shaped button meant to be displayed in a title bar.
 *
 * @param iconImageProvider icon to be displayed in the button
 * @param iconSize size of the icon displayed at center of the button
 * @param backgroundColor background color for the button
 * @param contentColor color of the icon displayed in the button
 * @param contentDescription description about the button that can be used by the accessibility
 *                           services
 * @param onClick [Action] to be performed on click of button
 * @param modifier the modifier to be applied to this button.
 */
@Composable
fun PillShapedButton(
  iconImageProvider: ImageProvider,
  iconSize: Dp,
  backgroundColor: ColorProvider,
  contentColor: ColorProvider,
  contentDescription: String,
  onClick: Action,
  modifier: GlanceModifier,
) {
  Box( // A clickable transparent outer container
    contentAlignment = Alignment.Center,
    modifier = modifier
      .semantics { this.contentDescription = contentDescription }
      .height(48.dp)
      .clickable(onClick),
  ) {
    Box( // A filled background with smaller height
      contentAlignment = Alignment.Center,
      modifier = GlanceModifier
        .width(52.dp)
        .height(32.dp)
        .background(backgroundColor)
        .cornerRadius(RoundedCornerShape.FULL.cornerRadius)
    ) { // The icon.
      Image(
        provider = iconImageProvider,
        contentDescription = null,
        colorFilter = ColorFilter.tint(contentColor),
        modifier = GlanceModifier.size(iconSize)
      )
    }
  }
}

/**
 * Defines the roundness of a shape inline with the tokens used in M3
 * https://m3.material.io/styles/shape/shape-scale-tokens
 */
enum class RoundedCornerShape(val cornerRadius: Dp) {
  FULL(100.dp),
  MEDIUM(16.dp),
}
