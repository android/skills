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
import androidx.glance.GlanceModifier
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width

/**
 * Arranges the provided [items] vertically spaced by given [spacing].
 */
@Composable
fun SpacedColumn(
  items: List<@Composable () -> Unit>,
  spacing: Dp,
  modifier: GlanceModifier = GlanceModifier.fillMaxHeight(),
) {
  val padding = spacing / 2 // split spacing between siblings

  Row(modifier = modifier) {
    Column(
      modifier = GlanceModifier
        .fillMaxHeight()
        .defaultWeight()
    ) {
      items.forEachIndexed { index, item ->
        val paddingModifier = when (index) {
          // Only bottom padding
          0 -> GlanceModifier.padding(bottom = padding)

          // Top and bottom padding
          items.lastIndex -> GlanceModifier.padding(top = padding)

          // Only top padding
          else -> GlanceModifier.padding(
            top = padding,
            bottom = padding
          )
        }

        Box(
          modifier = paddingModifier
            .fillMaxWidth()
            .defaultWeight()
        ) {
          item()
        }
      }
    }
  }
}

/**
 * Arranges the provided [items] horizontally spaced by given [spacing].
 */
@Composable
fun SpacedRow(
  items: List<@Composable () -> Unit>,
  spacing: Dp,
  modifier: GlanceModifier = GlanceModifier.fillMaxWidth(),
) {
  val padding = spacing / 2 // split spacing between siblings

  Column(modifier = modifier) {
    Row(
      modifier = GlanceModifier
        .fillMaxWidth()
        .defaultWeight()
    ) {
      items.forEachIndexed { index, item ->
        val paddingModifier = when (index) {
          // Right padding only
          0 -> GlanceModifier.padding(end = padding)

          // Left padding only
          items.lastIndex -> GlanceModifier.padding(start = padding)

          // Both left and right padding
          else -> GlanceModifier.padding(start = padding, end = padding)
        }

        Box(
          modifier = paddingModifier
            .fillMaxHeight()
            .defaultWeight()
        ) {
          item()
        }
      }
    }
  }
}

/**
 * Arranges given [items] in a grid of up to 2 rows spaced by [spacing] in the available space.
 *
 * Suitable for cases where there are multiple [items] that can be arranged in two rows.
 */
@Composable
fun TwoRowGrid(
  items: List<@Composable () -> Unit>,
  spacing: Dp,
  modifier: GlanceModifier = GlanceModifier.fillMaxSize(),
) {
  val middle = items.size / 2
  val rowOneItems = items.subList(0, middle)
  val rowTwoItems = items.subList(middle, items.size)

  Column(modifier = modifier) {
    if (rowOneItems.isNotEmpty()) {
      SpacedRow(
        items = rowOneItems,
        spacing = spacing,
        modifier = GlanceModifier
          .fillMaxWidth()
          .defaultWeight()
          .padding(bottom = spacing / 2),
      )
    }
    if (rowTwoItems.isNotEmpty()) {
      SpacedRow(
        items = rowTwoItems,
        spacing = spacing,
        modifier = GlanceModifier
          .fillMaxWidth()
          .padding(top = spacing / 2)
          .defaultWeight()
      )
    }
  }
}

/**
 * Arranges the provided [items] in a grid of two rows spaced by [spacing] with a [sideBarItem] on
 * the left.
 */
@Composable
fun SideBarTwoRowGrid(
  sideBarItem: @Composable () -> Unit,
  items: List<@Composable () -> Unit>,
  sideBarWidth: Dp,
  spacing: Dp,
  modifier: GlanceModifier = GlanceModifier.fillMaxSize(),
) {
  Row(modifier = modifier) {
    Box(
      modifier = GlanceModifier
        .fillMaxHeight()
        .width(sideBarWidth)
    ) {
      sideBarItem()
    }
    Spacer(
      modifier = GlanceModifier
        .fillMaxHeight()
        .width(spacing)
    )
    TwoRowGrid(
      items = items,
      spacing = spacing,
      modifier = GlanceModifier
        .fillMaxHeight()
        .defaultWeight()
    )
  }
}

/**
 * Arranges the provided [items] in a grid of two rows spaced by [spacing] with a [headerItem] on the top.
 */
@Composable
fun HeaderTwoRowGrid(
  headerItem: @Composable () -> Unit,
  items: List<@Composable () -> Unit>,
  headerHeight: Dp,
  spacing: Dp,
  modifier: GlanceModifier = GlanceModifier.fillMaxSize(),
) {
  Column(modifier = modifier) {
    Box(
      modifier = GlanceModifier
        .height(headerHeight)
        .fillMaxWidth()
    ) {
      headerItem()
    }
    Spacer(
      modifier = GlanceModifier
        .fillMaxWidth()
        .height(spacing)
    )
    TwoRowGrid(
      items = items,
      spacing = spacing,
      modifier = GlanceModifier
        .fillMaxWidth()
        .defaultWeight()
    )
  }
}
