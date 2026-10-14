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

package com.example.platform.ui.appwidgets.glance.layout

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.DrawableRes
import androidx.annotation.RequiresApi
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import com.example.platform.ui.appwidgets.R
import com.example.platform.ui.appwidgets.glance.layout.collections.ActionListAppWidgetAppWidgetReceiver
import com.example.platform.ui.appwidgets.glance.layout.collections.CheckListAppWidgetReceiver
import com.example.platform.ui.appwidgets.glance.layout.collections.ImageGridAppWidgetReceiver
import com.example.platform.ui.appwidgets.glance.layout.collections.ImageTextListAppWidgetReceiver
import com.example.platform.ui.appwidgets.glance.layout.text.LongTextAppWidgetReceiver
import com.example.platform.ui.appwidgets.glance.layout.text.TextWithImageAppWidgetReceiver
import com.example.platform.ui.appwidgets.glance.layout.text.FullBleedImageAppWidgetReceiver
import com.example.platform.ui.appwidgets.glance.layout.toolbars.ExpressiveToolbarAppWidgetReceiver
import com.example.platform.ui.appwidgets.glance.layout.toolbars.SearchToolBarAppWidgetReceiver
import com.example.platform.ui.appwidgets.glance.layout.toolbars.ToolBarAppWidgetReceiver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

import androidx.lifecycle.lifecycleScope

class CanonicalLayoutActivity : ComponentActivity() {

    companion object {
        fun start(context: Context) {
            val intent = Intent(context, CanonicalLayoutActivity::class.java)
            context.startActivity(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()

        super.onCreate(savedInstanceState)

        // Publish Generated Widget Previews
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            lifecycleScope.launch {
                try {
                    val context = this@CanonicalLayoutActivity
                    val receiver = FullBleedImageAppWidgetReceiver::class.java
                    val glanceAppWidgetManager = GlanceAppWidgetManager(context)
                    val appWidgetManager = context.getSystemService(AppWidgetManager::class.java)

                    val providerInfo = appWidgetManager?.installedProviders?.firstOrNull {
                        it.provider.className == receiver.name
                    }

                    if (providerInfo?.generatedPreviewCategories == 0) {
                        val result = glanceAppWidgetManager.setWidgetPreviews(FullBleedImageAppWidgetReceiver::class)
                        val status = when (result) {
                            GlanceAppWidgetManager.SET_WIDGET_PREVIEWS_RESULT_SUCCESS -> "Success"
                            GlanceAppWidgetManager.SET_WIDGET_PREVIEWS_RESULT_RATE_LIMITED -> "Rate-Limited"
                            else -> "Error ($result)"
                        }
                        Log.i("CanonicalLayoutActivity", "Published previews for ${receiver.simpleName}: $status")
                    }
                } catch (e: Exception) {
                    Log.e("CanonicalLayoutActivity", "Failed to set widget previews", e)
                }
            }
        }

        setContent {
            MaterialTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Log.i("~~~", innerPadding.toString())
                    Content(
                        modifier = Modifier,
                    )
                }
            }
        }
    }
}

@Composable
private fun Content(modifier: Modifier) {
    LazyColumn(modifier = modifier.fillMaxSize()) {

        item {
            Header(modifier = Modifier.fillMaxWidth())
        }

        items(items = canonicalLayoutWidgets) { rowData: CanonicalLayoutRowData ->
            CanonicalLayoutRow(
                widget = rowData,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 54.dp, start = 8.dp, end = 8.dp),
            )
        }

        item {
            Spacer(modifier = Modifier.size(24.dp))
        }
    }
}

@Composable
private fun Header(modifier: Modifier) {
    @Composable
    fun Title(modifier: Modifier = Modifier) {
        Text("Layouts", style = MaterialTheme.typography.displayMedium, modifier = modifier)
    }

    @Composable
    fun HeroImage(modifier: Modifier = Modifier) {
        Image(
            painterResource(id = R.drawable.cl_activity_row_hero_image),
            contentDescription = null,
            contentScale = ContentScale.FillHeight,
            modifier = modifier,
        )
    }

    @Composable
    fun Description(modifier: Modifier = Modifier) {
        val linkColor = MaterialTheme.colorScheme.primary
        val url = "https://developer.android.com/design/ui/mobile/guides/widgets/layouts"

        val annotatedText = remember {
            val part1 =
                "Explore these widget layouts, showcasing common design patterns. Add them to your home screen for a hands-on experience or dive into "
            val toLinkify = "detailed design guidelines."

            buildAnnotatedString {
                append(part1)
                pushStringAnnotation(
                    tag = "URL",
                    annotation = "url",
                )
                withStyle(
                    style = SpanStyle(
                        color = linkColor,
                    ),
                ) {
                    append(toLinkify)
                }
                pop()
            }
        }

        ClickableText(
            text = annotatedText,

            style = MaterialTheme.typography.bodyMedium.copy(textAlign = TextAlign.Center),
            modifier = modifier,
            onClick = { offset ->
                annotatedText.getStringAnnotations(
                    tag = "URL", start = offset, end = offset,
                ).firstOrNull()?.let { annotation ->
                    Intent(Intent.ACTION_VIEW, Uri.parse(url))
                }
            },
        )
    }

    Column(
        modifier = modifier.background(MaterialTheme.colorScheme.primaryContainer),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        Spacer(Modifier.size(24.dp))
        Title(Modifier.align(Alignment.CenterHorizontally))
        HeroImage()
        Description(Modifier.padding(start = 16.dp, end = 16.dp, bottom = 24.dp))
    }
}

@Composable
private fun CanonicalLayoutRow(widget: CanonicalLayoutRowData, modifier: Modifier) {
    @Composable
    fun Title(modifier: Modifier = Modifier) {
        Text(
            text = stringResource(widget.rowTitle),
            style = MaterialTheme.typography.titleMedium,
            modifier = modifier,
        )
    }

    @Composable
    fun RowImage(modifier: Modifier = Modifier) {
        Image(
            painter = painterResource(id = widget.imageRes),
            contentDescription = "Screenshot of ${widget.rowTitle}",
            contentScale = ContentScale.FillWidth,
            modifier = modifier.fillMaxWidth(),
        )
    }

    @Composable
    fun BodyText(modifier: Modifier = Modifier) {
        Text(
            text = stringResource(widget.rowDescription),
            style = MaterialTheme.typography.bodyMedium,
            modifier = modifier,
        )
    }


    @Composable
    fun PinWidgetButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
        Button(modifier = modifier, onClick = onClick) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Add, contentDescription = null)
                Text("Add to home")
            }
        }
    }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    Column(modifier, Arrangement.spacedBy(24.dp)) {
        RowImage()
        Title()
        BodyText()
        PinWidgetButton(
            modifier = Modifier.align(Alignment.CenterHorizontally),
            onClick = { requestPin(widget, context, scope) },
        )
    }
}

private fun requestPin(
    widget: CanonicalLayoutRowData,
    context: Context,
    coroutineScope: CoroutineScope,
) {
    val glanceManager = GlanceAppWidgetManager(context)
    val receiverClass = widget.receiver
    val receiver = receiverClass.getDeclaredConstructor()
        .newInstance() as GlanceAppWidgetReceiver

    coroutineScope.launch {
        glanceManager.requestPinGlanceAppWidget(receiver::class.java)
    }
}


private data class CanonicalLayoutRowData(
    @StringRes val rowTitle: Int,
    @StringRes val rowDescription: Int,
    @DrawableRes val imageRes: Int,
    val receiver: Class<out GlanceAppWidgetReceiver>,
)

private val canonicalLayoutWidgets = listOf(
    CanonicalLayoutRowData(
        rowTitle = R.string.cl_title_checklist,
        rowDescription = R.string.cl_description_checklist,
        imageRes = R.drawable.cl_activity_row_checklist,
        receiver = CheckListAppWidgetReceiver::class.java,
    ),
    CanonicalLayoutRowData(
        rowTitle = R.string.cl_title_long_text,
        rowDescription = R.string.cl_description_long_text,
        imageRes = R.drawable.cl_activity_row_long_text,
        receiver = LongTextAppWidgetReceiver::class.java,
    ),
    CanonicalLayoutRowData(
        rowTitle = R.string.cl_title_action_list,
        rowDescription = R.string.cl_description_action_list,
        imageRes = R.drawable.cl_activity_row_action_list,
        receiver = ActionListAppWidgetAppWidgetReceiver::class.java,
    ),
    CanonicalLayoutRowData(
        rowTitle = R.string.cl_title_image_text_list,
        rowDescription = R.string.cl_description_image_text_list,
        imageRes = R.drawable.cl_activity_row_image_text_list,
        receiver = ImageTextListAppWidgetReceiver::class.java,
    ),
    CanonicalLayoutRowData(
        rowTitle = R.string.cl_title_text_and_image,
        rowDescription = R.string.cl_description_text_and_image,
        imageRes = R.drawable.cl_activity_row_text_image,
        receiver = TextWithImageAppWidgetReceiver::class.java,
    ),
    CanonicalLayoutRowData(
        rowTitle = R.string.cl_title_full_bleed_image,
        rowDescription = R.string.cl_description_full_bleed_image,
        imageRes = R.drawable.cl_activity_row_full_bleed_image,
        receiver = FullBleedImageAppWidgetReceiver::class.java,
    ),
    CanonicalLayoutRowData(
        rowTitle = R.string.cl_title_grid,
        rowDescription = R.string.cl_description_grid,
        imageRes = R.drawable.cl_activity_row_image_grid,
        receiver = ImageGridAppWidgetReceiver::class.java,
    ),
    CanonicalLayoutRowData(
        rowTitle = R.string.cl_title_toolbar,
        rowDescription = R.string.cl_description_toolbar,
        imageRes = R.drawable.cl_activity_row_toolbar,
        receiver = ToolBarAppWidgetReceiver::class.java,
    ),
    CanonicalLayoutRowData(
        rowTitle = R.string.cl_title_search_toolbar,
        rowDescription = R.string.cl_description_search_toolbar,
        imageRes = R.drawable.cl_activity_row_search_toolbar,
        receiver = SearchToolBarAppWidgetReceiver::class.java,
    ),
    CanonicalLayoutRowData(
        rowTitle = R.string.cl_title_expressive_toolbar,
        rowDescription = R.string.cl_description_expressive_toolbar,
        imageRes = R.drawable.cl_activity_row_expressive_toolbar,
        receiver = ExpressiveToolbarAppWidgetReceiver::class.java,
    ),
)
