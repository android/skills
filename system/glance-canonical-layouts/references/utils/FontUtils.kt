/*
 * Copyright 2026 The Android Open Source Project
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

package com.example.platform.ui.appwidgets.glance.layout.utils

import android.content.Context
import android.text.TextUtils
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.appcompat.widget.AppCompatTextView
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import androidx.core.widget.TextViewCompat

/**
 * Holds utilities around fonts.
 */
internal object FontUtils {
  // An alternate implementation can extract the logic from following reference instead of using a
  // textView.
  // https://cs.android.com/androidx/platform/frameworks/support/+/androidx-main:appcompat/appcompat/src/main/java/androidx/appcompat/widget/AppCompatTextViewAutoSizeHelper.java;l=679;drc=573d69d7458c96885cdcbbf8471e24dd8d293997

  /**
   * Returns the font size and max lines that can display the text uniformly in the given available
   * width and height.
   *
   * Chooses the font size from the given min and max font size range.
   */
  fun calculateFontSizeAndMaxLines(
    context: Context,
    text: String,
    availableWidth: Dp,
    availableHeight: Dp,
    minFontSize: TextUnit,
    maxFontSize: TextUnit,
  ): Pair<TextUnit, Int> {
    fun spToPx(value: TextUnit): Int {
      return (value * context.resources.displayMetrics.scaledDensity).value.toInt();
    }

    fun dpToPx(dp: Dp): Int {
      return TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP,
        dp.value,
        context.resources.displayMetrics
      ).toInt()
    }

    val layoutWidth = dpToPx(availableWidth)
    val layoutHeight = dpToPx(availableHeight)

    // setup layout and text view
    val layout = LinearLayout(context).apply {
      layoutParams = LinearLayout.LayoutParams(layoutWidth, layoutHeight)
    }
    val textView = AppCompatTextView(context).apply {
      this.text = text
      this.ellipsize = TextUtils.TruncateAt.END
      this.setTextSize(TypedValue.COMPLEX_UNIT_SP, minFontSize.value)
      layoutParams = ViewGroup.LayoutParams(
        LinearLayout.LayoutParams.MATCH_PARENT,
        LinearLayout.LayoutParams.MATCH_PARENT
      )
    }
    TextViewCompat.setAutoSizeTextTypeUniformWithConfiguration(
      textView,
      spToPx(minFontSize),
      spToPx(maxFontSize),
      /*autoSizeStepGranularity=*/1,
      TypedValue.COMPLEX_UNIT_PX
    );

    // add and measure
    layout.addView(textView)
    layout.measure(
      View.MeasureSpec.makeMeasureSpec(layoutWidth, View.MeasureSpec.EXACTLY),
      View.MeasureSpec.makeMeasureSpec(layoutHeight, View.MeasureSpec.EXACTLY)
    )
    textView.layout(0, 0, layout.measuredWidth, layout.measuredHeight)

    // extract calculated sizes.
    val size = textView.textSize / context.resources.displayMetrics.scaledDensity
    val maxLines = (layoutHeight / textView.textSize).toInt().coerceAtLeast(1)
    return size.sp to maxLines
  }
}
