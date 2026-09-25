When creating a Glimmer Card component, refer to the following source code in
`Card.kt`:


```kotlin
/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.xr.glimmer

import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

/**
 * Card is a component used to group related information into a single digestible unit. A card can
 * adapt to display a wide range of content, from simple text blurbs to more complex summaries with
 * multiple elements. A card contains text [content], and may also have any combination of [title],
 * [subtitle], [leadingIcon], and [trailingIcon]. If specified, [title] is placed on top of the
 * [subtitle], which is placed on top of the [content]. A card fills the maximum width available by
 * default.
 *
 * This Card is focusable - see the other [Card] overload for a clickable Card.
 *
 * See [ActionCard] for a card with a primary action, and [ImageCard] for a card with an image.
 *
 * Cards can also be combined with a [TitleChip]. See the documentation for [TitleChip] for more
 * information / sample code.
 *
 * A simple Card with just text:
 *
 * @sample androidx.xr.glimmer.samples.CardSample
 *
 * A Card with a trailing icon:
 *
 * @sample androidx.xr.glimmer.samples.CardWithTrailingIconSample
 *
 * A Card with a title, subtitle, and a leading icon:
 *
 * @sample androidx.xr.glimmer.samples.CardWithTitleAndSubtitleAndLeadingIconSample
 * @param modifier the [Modifier] to be applied to this card
 * @param title optional title to be placed above [subtitle] and [content]
 * @param subtitle optional subtitle to be placed above [content], below [title]
 * @param leadingIcon optional leading icon to be placed before [content]. This is typically an
 *   [Icon] tinted with [contentColor] by default.
 * @param trailingIcon optional trailing icon to be placed after [content]. This is typically an
 *   [Icon] tinted with [contentColor] by default.
 * @param shape the [Shape] used to clip this card, and also used to draw the background and border
 * @param color background color of this card
 * @param contentColor content color used by components inside [content], [title], [subtitle],
 *   [leadingIcon], and [trailingIcon].
 * @param contentPadding the spacing values to apply internally between the container and the
 *   content. Note that there is additional padding applied around the content / text / icons inside
 *   a card, this only affects the outermost content padding.
 * @param interactionSource an optional hoisted [MutableInteractionSource] for observing and
 *   emitting Interactions for this card. You can use this to change the card's appearance or
 *   preview the card in different states. Note that if `null` is provided, interactions will still
 *   happen internally.
 * @param content the main content / body text to display inside this card. This is recommended to
 *   be limited to 10 lines of text.
 */
@Composable
public fun Card(
    modifier: Modifier = Modifier,
    title: @Composable (() -> Unit)? = null,
    subtitle: @Composable (() -> Unit)? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    shape: Shape = CardDefaults.shape,
    color: Color = CardDefaults.color,
    contentColor: Color = CardDefaults.contentColor(color),
    contentPadding: PaddingValues = CardDefaults.contentPadding,
    interactionSource: MutableInteractionSource? = null,
    content: @Composable () -> Unit,
) {
    val internalInteractionSource = interactionSource ?: remember { MutableInteractionSource() }
    CardImpl(
        modifier =
            modifier
                .surface(
                    shape = shape,
                    color = color,
                    contentColor = contentColor,
                    interactionSource = internalInteractionSource,
                )
                .focusable(interactionSource = internalInteractionSource),
        title = title,
        subtitle = subtitle,
        image = null,
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        action = null,
        contentPadding = contentPadding,
        content = content,
    )
}

/**
 * Card is a component used to group related information into a single digestible unit. A card can
 * adapt to display a wide range of content, from simple text blurbs to more complex summaries with
 * multiple elements. A card contains text [content], and may also have any combination of [title],
 * [subtitle], [leadingIcon], and [trailingIcon]. If specified, [title] is placed on top of the
 * [subtitle], which is placed on top of the [content]. A card fills the maximum width available by
 * default.
 *
 * This Card is focusable and clickable - see the other [Card] overload for a Card that is only
 * focusable.
 *
 * See [ActionCard] for a card with a primary action, and [ImageCard] for a card with an image.
 *
 * Cards can also be combined with a [TitleChip]. See the documentation for [TitleChip] for more
 * information / sample code.
 *
 * A simple clickable Card with just text:
 *
 * @sample androidx.xr.glimmer.samples.ClickableCardSample
 *
 * A clickable Card with a trailing icon:
 *
 * @sample androidx.xr.glimmer.samples.ClickableCardWithTrailingIconSample
 *
 * A clickable Card with a title, subtitle, and a leading icon:
 *
 * @sample androidx.xr.glimmer.samples.ClickableCardWithTitleAndSubtitleAndLeadingIconSample
 * @param onClick called when this card item is clicked
 * @param modifier the [Modifier] to be applied to this card
 * @param title optional title to be placed above [subtitle] and [content]
 * @param subtitle optional subtitle to be placed above [content], below [title]
 * @param leadingIcon optional leading icon to be placed before [content]. This is typically an
 *   [Icon] tinted with [contentColor] by default.
 * @param trailingIcon optional trailing icon to be placed after [content]. This is typically an
 *   [Icon] tinted with [contentColor] by default.
 * @param shape the [Shape] used to clip this card, and also used to draw the background and border
 * @param color background color of this card
 * @param contentColor content color used by components inside [content], [title], [subtitle],
 *   [leadingIcon], and [trailingIcon].
 * @param contentPadding the spacing values to apply internally between the container and the
 *   content. Note that there is additional padding applied around the content / text / icons inside
 *   a card, this only affects the outermost content padding.
 * @param interactionSource an optional hoisted [MutableInteractionSource] for observing and
 *   emitting Interactions for this card. You can use this to change the card's appearance or
 *   preview the card in different states. Note that if `null` is provided, interactions will still
 *   happen internally.
 * @param content the main content / body text to display inside this card. This is recommended to
 *   be limited to 10 lines of text.
 */
@Composable
public fun Card(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    title: @Composable (() -> Unit)? = null,
    subtitle: @Composable (() -> Unit)? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    shape: Shape = CardDefaults.shape,
    color: Color = CardDefaults.color,
    contentColor: Color = CardDefaults.contentColor(color),
    contentPadding: PaddingValues = CardDefaults.contentPadding,
    interactionSource: MutableInteractionSource? = null,
    content: @Composable () -> Unit,
) {
    val internalInteractionSource = interactionSource ?: remember { MutableInteractionSource() }
    CardImpl(
        modifier =
            modifier
                .surface(
                    shape = shape,
                    color = color,
                    contentColor = contentColor,
                    interactionSource = internalInteractionSource,
                )
                .clickable(interactionSource = internalInteractionSource, onClick = onClick),
        title = title,
        subtitle = subtitle,
        image = null,
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        action = null,
        contentPadding = contentPadding,
        content = content,
    )
}

/**
 * ActionCard is a version of a card that contains a primary [action] that is placed inside the card
 * along the bottom edge of the card. The action should be a [Button], and represents the action
 * that will be performed when this card is interacted with. The main card itself is not focusable -
 * the [action] takes the focus instead.
 *
 * ActionCard is a component used to group related information into a single digestible unit. An
 * action card can adapt to display a wide range of content, from simple text blurbs to more complex
 * summaries with multiple elements. An action card contains text [content], and may also have any
 * combination of [title], [subtitle], [leadingIcon], and [trailingIcon]. If specified, [title] is
 * placed on top of the [subtitle], which is placed on top of the [content]. An action card fills
 * the maximum width available by default.
 *
 * For more documentation and samples of the other cards, see [Card].
 *
 * @sample androidx.xr.glimmer.samples.ActionCardWithTitleSample
 * @param action the action for this card. This should be a [Button], and represents the action
 *   performed when a user interacts with this card. The action is placed inside the card along the
 *   bottom, and fills up the width of the [ActionCard].
 * @param modifier the [Modifier] to be applied to the outer layout containing the card and action
 * @param title optional title to be placed above [subtitle] and [content]
 * @param subtitle optional subtitle to be placed above [content], below [title]
 * @param leadingIcon optional leading icon to be placed before [content]. This is typically an
 *   [Icon] tinted with [contentColor] by default.
 * @param trailingIcon optional trailing icon to be placed after [content]. This is typically an
 *   [Icon] tinted with [contentColor] by default.
 * @param shape the [Shape] used to clip this card, and also used to draw the background and border
 * @param color background color of this card
 * @param contentColor content color used by components inside [content], [title], [subtitle],
 *   [leadingIcon], and [trailingIcon].
 * @param contentPadding the spacing values to apply internally between the container and the
 *   content. Note that there is additional padding applied around the content / text / icons inside
 *   a card, this only affects the outermost content padding.
 * @param content the main content / body text to display inside this card. This is recommended to
 *   be limited to 10 lines of text.
 */
@Composable
public fun ActionCard(
    action: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    title: @Composable (() -> Unit)? = null,
    subtitle: @Composable (() -> Unit)? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    shape: Shape = ActionCardDefaults.shape,
    color: Color = ActionCardDefaults.color,
    contentColor: Color = ActionCardDefaults.contentColor(color),
    contentPadding: PaddingValues = ActionCardDefaults.contentPadding,
    content: @Composable () -> Unit,
) {
    CardImpl(
        modifier = modifier.surface(shape = shape, color = color, contentColor = contentColor),
        title = title,
        subtitle = subtitle,
        image = null,
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        action = action,
        contentPadding = contentPadding,
        content = content,
    )
}

/**
 * ImageCard is a version of a card that contains a primary [image] that is placed at the top of the
 * card.
 *
 * ImageCard is a component used to group related information into a single digestible unit. An
 * image card can adapt to display a wide range of content, from simple text blurbs to more complex
 * summaries with multiple elements. An image card contains an [image] and text [content], and may
 * also have any combination of [title], [subtitle], [leadingIcon], and [trailingIcon]. If
 * specified, [title] is placed on top of the [subtitle], which is placed on top of the [content],
 * below [image]. An image card fills the maximum width available by default.
 *
 * This ImageCard is focusable - see the other [ImageCard] overload for a clickable ImageCard.
 *
 * For more documentation and samples of the other cards, see [Card].
 *
 * A simple ImageCard with just text:
 *
 * @sample androidx.xr.glimmer.samples.ImageCardSample
 *
 * An ImageCard with a title, subtitle, and a leading icon:
 *
 * @sample androidx.xr.glimmer.samples.ImageCardWithTitleAndSubtitleAndLeadingIconSample
 * @param image image to be placed at the top of the card. This image should typically fill the max
 *   width available, for example using [androidx.compose.ui.layout.ContentScale.FillWidth]. Images
 *   are constrained to a maximum aspect ratio (1.6) to avoid taking up too much vertical space, so
 *   using a modifier such as [androidx.compose.foundation.layout.fillMaxSize] will result in an
 *   image that fills the maximum aspect ratio.
 * @param modifier the [Modifier] to be applied to this card
 * @param title optional title to be placed above [subtitle] and [content], below [image]
 * @param subtitle optional subtitle to be placed above [content], below [title]
 * @param leadingIcon optional leading icon to be placed before [content]. This is typically an
 *   [Icon] tinted with [contentColor] by default.
 * @param trailingIcon optional trailing icon to be placed after [content]. This is typically an
 *   [Icon] tinted with [contentColor] by default.
 * @param shape the [Shape] used to clip this card, and also used to draw the background and border
 * @param color background color of this card
 * @param contentColor content color used by components inside [content], [title], [subtitle],
 *   [leadingIcon], and [trailingIcon].
 * @param contentPadding the spacing values to apply internally between the container and the
 *   content. Note that there is additional padding applied around the content / text / icons inside
 *   a card, this only affects the outermost content padding.
 * @param interactionSource an optional hoisted [MutableInteractionSource] for observing and
 *   emitting Interactions for this card. You can use this to change the card's appearance or
 *   preview the card in different states. Note that if `null` is provided, interactions will still
 *   happen internally.
 * @param content the main content / body text to display inside this card. This is recommended to
 *   be limited to 10 lines of text.
 */
@Composable
public fun ImageCard(
    image: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    title: @Composable (() -> Unit)? = null,
    subtitle: @Composable (() -> Unit)? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    shape: Shape = ImageCardDefaults.shape,
    color: Color = ImageCardDefaults.color,
    contentColor: Color = ImageCardDefaults.contentColor(color),
    contentPadding: PaddingValues = ImageCardDefaults.contentPadding,
    interactionSource: MutableInteractionSource? = null,
    content: @Composable () -> Unit,
) {
    val internalInteractionSource = interactionSource ?: remember { MutableInteractionSource() }
    CardImpl(
        modifier =
            modifier
                .surface(
                    shape = shape,
                    color = color,
                    contentColor = contentColor,
                    interactionSource = internalInteractionSource,
                )
                .focusable(interactionSource = internalInteractionSource),
        title = title,
        subtitle = subtitle,
        image = image,
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        action = null,
        contentPadding = contentPadding,
        content = content,
    )
}

/**
 * ImageCard is a version of a card that contains a primary [image] that is placed at the top of the
 * card.
 *
 * ImageCard is a component used to group related information into a single digestible unit. An
 * image card can adapt to display a wide range of content, from simple text blurbs to more complex
 * summaries with multiple elements. An image card contains an [image] and text [content], and may
 * also have any combination of [title], [subtitle], [leadingIcon], and [trailingIcon]. If
 * specified, [title] is placed on top of the [subtitle], which is placed on top of the [content],
 * below [image]. An image card fills the maximum width available by default.
 *
 * This ImageCard is focusable and clickable - see the other [ImageCard] overload for an ImageCard
 * that is only focusable.
 *
 * For more documentation and samples of the other cards, see [Card].
 *
 * A simple clickable ImageCard with just text:
 *
 * @sample androidx.xr.glimmer.samples.ClickableImageCardSample
 *
 * A clickable ImageCard with a title, subtitle, and a leading icon:
 *
 * @sample androidx.xr.glimmer.samples.ClickableImageCardWithTitleAndSubtitleAndLeadingIconSample
 * @param onClick called when this card item is clicked
 * @param image image to be placed at the top of the card. This image should typically fill the max
 *   width available, for example using [androidx.compose.ui.layout.ContentScale.FillWidth]. Images
 *   are constrained to a maximum aspect ratio (1.6) to avoid taking up too much vertical space, so
 *   using a modifier such as [androidx.compose.foundation.layout.fillMaxSize] will result in an
 *   image that fills the maximum aspect ratio.
 * @param modifier the [Modifier] to be applied to this card
 * @param title optional title to be placed above [subtitle] and [content], below [image]
 * @param subtitle optional subtitle to be placed above [content], below [title]
 * @param leadingIcon optional leading icon to be placed before [content]. This is typically an
 *   [Icon] tinted with [contentColor] by default.
 * @param trailingIcon optional trailing icon to be placed after [content]. This is typically an
 *   [Icon] tinted with [contentColor] by default.
 * @param shape the [Shape] used to clip this card, and also used to draw the background and border
 * @param color background color of this card
 * @param contentColor content color used by components inside [content], [title], [subtitle],
 *   [leadingIcon], and [trailingIcon].
 * @param contentPadding the spacing values to apply internally between the container and the
 *   content. Note that there is additional padding applied around the content / text / icons inside
 *   a card, this only affects the outermost content padding.
 * @param interactionSource an optional hoisted [MutableInteractionSource] for observing and
 *   emitting Interactions for this card. You can use this to change the card's appearance or
 *   preview the card in different states. Note that if `null` is provided, interactions will still
 *   happen internally.
 * @param content the main content / body text to display inside this card. This is recommended to
 *   be limited to 10 lines of text.
 */
@Composable
public fun ImageCard(
    onClick: () -> Unit,
    image: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    title: @Composable (() -> Unit)? = null,
    subtitle: @Composable (() -> Unit)? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    shape: Shape = ImageCardDefaults.shape,
    color: Color = ImageCardDefaults.color,
    contentColor: Color = ImageCardDefaults.contentColor(color),
    contentPadding: PaddingValues = ImageCardDefaults.contentPadding,
    interactionSource: MutableInteractionSource? = null,
    content: @Composable () -> Unit,
) {
    val internalInteractionSource = interactionSource ?: remember { MutableInteractionSource() }
    CardImpl(
        modifier =
            modifier
                .surface(
                    shape = shape,
                    color = color,
                    contentColor = contentColor,
                    interactionSource = internalInteractionSource,
                )
                .clickable(interactionSource = internalInteractionSource, onClick = onClick),
        title = title,
        subtitle = subtitle,
        image = image,
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        action = null,
        contentPadding = contentPadding,
        content = content,
    )
}

/**
 * LeadingImageCard is a component used to group related information into a single digestible unit,
 * featuring a leading [image]. A leading image card contains text [content], and may also have any
 * combination of [title] and [subtitle]. If specified, [title] is placed on top of the [subtitle],
 * which is placed on top of the [content]. A leading image card fills the maximum width available
 * by default.
 *
 * This LeadingImageCard is focusable - see the other [LeadingImageCard] overload for a clickable
 * LeadingImageCard.
 *
 * @param image the image to be placed before the text content. This image is allocated 30% of the
 *   card width.
 * @param modifier the [Modifier] to be applied to this card
 * @param title optional title to be placed above [subtitle] and [content]
 * @param subtitle optional subtitle to be placed above [content], below [title]
 * @param shape the [Shape] used to clip this card, and also used to draw the background and border
 * @param color background color of this card
 * @param contentColor content color used by components inside [content], [title], and [subtitle]
 * @param contentPadding the spacing values to apply internally between the container and the
 *   content. Note that there is additional padding applied around the content / text / image inside
 *   a card, this only affects the outermost content padding.
 * @param interactionSource an optional hoisted [MutableInteractionSource] for observing and
 *   emitting Interactions for this card. You can use this to change the card's appearance or
 *   preview the card in different states. Note that if `null` is provided, interactions will still
 *   happen internally.
 * @param content the main content / body text to display inside this card. This is recommended to
 *   be limited to 10 lines of text.
 */
@Composable
public fun LeadingImageCard(
    image: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    title: @Composable (() -> Unit)? = null,
    subtitle: @Composable (() -> Unit)? = null,
    shape: Shape = LeadingImageCardDefaults.shape,
    color: Color = LeadingImageCardDefaults.color,
    contentColor: Color = LeadingImageCardDefaults.contentColor(color),
    contentPadding: PaddingValues = LeadingImageCardDefaults.contentPadding,
    interactionSource: MutableInteractionSource? = null,
    content: @Composable () -> Unit,
) {
    val internalInteractionSource = interactionSource ?: remember { MutableInteractionSource() }
    val isLtr = LocalLayoutDirection.current == LayoutDirection.Ltr
    SmallImageCardImpl(
        modifier =
            modifier
                .surface(
                    shape = shape,
                    color = color,
                    contentColor = contentColor,
                    interactionSource = internalInteractionSource,
                )
                .focusable(interactionSource = internalInteractionSource),
        image = image,
        title = title,
        subtitle = subtitle,
        contentPadding = contentPadding,
        isLeading = isLtr,
        content = content,
    )
}

/**
 * LeadingImageCard is a component used to group related information into a single digestible unit,
 * featuring a leading [image]. A leading image card contains text [content], and may also have any
 * combination of [title] and [subtitle]. If specified, [title] is placed on top of the [subtitle],
 * which is placed on top of the [content]. A leading image card fills the maximum width available
 * by default.
 *
 * This LeadingImageCard is focusable and clickable - see the other [LeadingImageCard] overload for
 * a LeadingImageCard that is only focusable.
 *
 * @param onClick called when this card item is clicked
 * @param image the image to be placed before the text content. This image is allocated 30% of the
 *   card width.
 * @param modifier the [Modifier] to be applied to this card
 * @param title optional title to be placed above [subtitle] and [content]
 * @param subtitle optional subtitle to be placed above [content], below [title]
 * @param shape the [Shape] used to clip this card, and also used to draw the background and border
 * @param color background color of this card
 * @param contentColor content color used by components inside [content], [title], and [subtitle]
 * @param contentPadding the spacing values to apply internally between the container and the
 *   content. Note that there is additional padding applied around the content / text / image inside
 *   a card, this only affects the outermost content padding.
 * @param interactionSource an optional hoisted [MutableInteractionSource] for observing and
 *   emitting Interactions for this card. You can use this to change the card's appearance or
 *   preview the card in different states. Note that if `null` is provided, interactions will still
 *   happen internally.
 * @param content the main content / body text to display inside this card. This is recommended to
 *   be limited to 10 lines of text.
 */
@Composable
public fun LeadingImageCard(
    onClick: () -> Unit,
    image: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    title: @Composable (() -> Unit)? = null,
    subtitle: @Composable (() -> Unit)? = null,
    shape: Shape = LeadingImageCardDefaults.shape,
    color: Color = LeadingImageCardDefaults.color,
    contentColor: Color = LeadingImageCardDefaults.contentColor(color),
    contentPadding: PaddingValues = LeadingImageCardDefaults.contentPadding,
    interactionSource: MutableInteractionSource? = null,
    content: @Composable () -> Unit,
) {
    val internalInteractionSource = interactionSource ?: remember { MutableInteractionSource() }
    val isLtr = LocalLayoutDirection.current == LayoutDirection.Ltr
    SmallImageCardImpl(
        modifier =
            modifier
                .surface(
                    shape = shape,
                    color = color,
                    contentColor = contentColor,
                    interactionSource = internalInteractionSource,
                )
                .clickable(interactionSource = internalInteractionSource, onClick = onClick),
        image = image,
        title = title,
        subtitle = subtitle,
        contentPadding = contentPadding,
        isLeading = isLtr,
        content = content,
    )
}

/**
 * TrailingImageCard is a component used to group related information into a single digestible unit,
 * featuring a trailing [image]. A trailing image card contains text [content], and may also have
 * any combination of [title] and [subtitle]. If specified, [title] is placed on top of the
 * [subtitle], which is placed on top of the [content]. A trailing image card fills the maximum
 * width available by default.
 *
 * This TrailingImageCard is focusable - see the other [TrailingImageCard] overload for a clickable
 * TrailingImageCard.
 *
 * @param image the image to be placed after the text content. This image is allocated 30% of the
 *   card width.
 * @param modifier the [Modifier] to be applied to this card
 * @param title optional title to be placed above [subtitle] and [content]
 * @param subtitle optional subtitle to be placed above [content], below [title]
 * @param shape the [Shape] used to clip this card, and also used to draw the background and border
 * @param color background color of this card
 * @param contentColor content color used by components inside [content], [title], and [subtitle]
 * @param contentPadding the spacing values to apply internally between the container and the
 *   content. Note that there is additional padding applied around the content / text / image inside
 *   a card, this only affects the outermost content padding.
 * @param interactionSource an optional hoisted [MutableInteractionSource] for observing and
 *   emitting Interactions for this card. You can use this to change the card's appearance or
 *   preview the card in different states. Note that if `null` is provided, interactions will still
 *   happen internally.
 * @param content the main content / body text to display inside this card. This is recommended to
 *   be limited to 10 lines of text.
 */
@Composable
public fun TrailingImageCard(
    image: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    title: @Composable (() -> Unit)? = null,
    subtitle: @Composable (() -> Unit)? = null,
    shape: Shape = TrailingImageCardDefaults.shape,
    color: Color = TrailingImageCardDefaults.color,
    contentColor: Color = TrailingImageCardDefaults.contentColor(color),
    contentPadding: PaddingValues = TrailingImageCardDefaults.contentPadding,
    interactionSource: MutableInteractionSource? = null,
    content: @Composable () -> Unit,
) {
    val internalInteractionSource = interactionSource ?: remember { MutableInteractionSource() }
    val isLtr = LocalLayoutDirection.current == LayoutDirection.Ltr
    SmallImageCardImpl(
        modifier =
            modifier
                .surface(
                    shape = shape,
                    color = color,
                    contentColor = contentColor,
                    interactionSource = internalInteractionSource,
                )
                .focusable(interactionSource = internalInteractionSource),
        image = image,
        title = title,
        subtitle = subtitle,
        contentPadding = contentPadding,
        isLeading = !isLtr,
        content = content,
    )
}

/**
 * TrailingImageCard is a component used to group related information into a single digestible unit,
 * featuring a trailing [image]. A trailing image card contains text [content], and may also have
 * any combination of [title] and [subtitle]. If specified, [title] is placed on top of the
 * [subtitle], which is placed on top of the [content]. A trailing image card fills the maximum
 * width available by default.
 *
 * This TrailingImageCard is focusable and clickable - see the other [TrailingImageCard] overload
 * for a TrailingImageCard that is only focusable.
 *
 * @param onClick called when this card item is clicked
 * @param image the image to be placed after the text content. This image is allocated 30% of the
 *   card width.
 * @param modifier the [Modifier] to be applied to this card
 * @param title optional title to be placed above [subtitle] and [content]
 * @param subtitle optional subtitle to be placed above [content], below [title]
 * @param shape the [Shape] used to clip this card, and also used to draw the background and border
 * @param color background color of this card
 * @param contentColor content color used by components inside [content], [title], and [subtitle]
 * @param contentPadding the spacing values to apply internally between the container and the
 *   content. Note that there is additional padding applied around the content / text / image inside
 *   a card, this only affects the outermost content padding.
 * @param interactionSource an optional hoisted [MutableInteractionSource] for observing and
 *   emitting Interactions for this card. You can use this to change the card's appearance or
 *   preview the card in different states. Note that if `null` is provided, interactions will still
 *   happen internally.
 * @param content the main content / body text to display inside this card. This is recommended to
 *   be limited to 10 lines of text.
 */
@Composable
public fun TrailingImageCard(
    onClick: () -> Unit,
    image: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    title: @Composable (() -> Unit)? = null,
    subtitle: @Composable (() -> Unit)? = null,
    shape: Shape = TrailingImageCardDefaults.shape,
    color: Color = TrailingImageCardDefaults.color,
    contentColor: Color = TrailingImageCardDefaults.contentColor(color),
    contentPadding: PaddingValues = TrailingImageCardDefaults.contentPadding,
    interactionSource: MutableInteractionSource? = null,
    content: @Composable () -> Unit,
) {
    val internalInteractionSource = interactionSource ?: remember { MutableInteractionSource() }
    val isLtr = LocalLayoutDirection.current == LayoutDirection.Ltr
    SmallImageCardImpl(
        modifier =
            modifier
                .surface(
                    shape = shape,
                    color = color,
                    contentColor = contentColor,
                    interactionSource = internalInteractionSource,
                )
                .clickable(interactionSource = internalInteractionSource, onClick = onClick),
        image = image,
        title = title,
        subtitle = subtitle,
        contentPadding = contentPadding,
        isLeading = !isLtr,
        content = content,
    )
}

@Composable
private fun CardImpl(
    modifier: Modifier,
    title: @Composable (() -> Unit)?,
    subtitle: @Composable (() -> Unit)?,
    image: @Composable (() -> Unit)?,
    leadingIcon: @Composable (() -> Unit)?,
    trailingIcon: @Composable (() -> Unit)?,
    action: @Composable (() -> Unit)?,
    contentPadding: PaddingValues,
    content: @Composable () -> Unit,
) {
    val iconSize = GlimmerTheme.iconSizes.large
    val typography = GlimmerTheme.typography
    val componentSpacingValues = GlimmerTheme.componentSpacingValues
    val innerPadding = componentSpacingValues.small
    val iconSpacing = componentSpacingValues.medium

    Column(
        modifier = modifier.defaultMinSize(minHeight = MinimumHeight).padding(contentPadding),
        verticalArrangement = CardVerticalArrangement,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            image?.let {
                Box(
                    Modifier.constrainHeightToAspectRatio(HeaderImageMaximumAspectRatio)
                        .clip(ImageShape),
                    contentAlignment = Alignment.Center,
                ) {
                    it()
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(innerPadding),
                verticalAlignment = CenterVertically,
            ) {
                if (leadingIcon != null) {
                    Box(
                        modifier = Modifier.align(Alignment.Top).padding(end = iconSpacing),
                        contentAlignment = Alignment.TopStart,
                    ) {
                        CompositionLocalProvider(
                            LocalIconSize provides iconSize,
                            content = leadingIcon,
                        )
                    }
                }
                Column(
                    Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(TextVerticalSpacing),
                ) {
                    if (title != null) {
                        CompositionLocalProvider(
                            LocalTextStyle provides typography.bodyMedium,
                            content = title,
                        )
                    }

                    if (subtitle != null) {
                        CompositionLocalProvider(
                            LocalTextStyle provides typography.caption,
                            content = subtitle,
                        )
                    }

                    CompositionLocalProvider(
                        LocalTextStyle provides typography.bodySmall,
                        content = content,
                    )
                }
                if (trailingIcon != null) {
                    Box(
                        modifier = Modifier.align(Alignment.Top).padding(start = iconSpacing),
                        contentAlignment = Alignment.TopEnd,
                    ) {
                        CompositionLocalProvider(
                            LocalIconSize provides iconSize,
                            content = trailingIcon,
                        )
                    }
                }
            }
        }
        // b/436852852 - in a list the button won't be focused until it crosses the focus line.
        action?.let {
            Box(modifier = Modifier.fillMaxWidth(), propagateMinConstraints = true) { it() }
        }
    }
}

@Composable
private fun SmallImageCardImpl(
    modifier: Modifier,
    image: @Composable () -> Unit,
    title: @Composable (() -> Unit)?,
    subtitle: @Composable (() -> Unit)?,
    contentPadding: PaddingValues,
    isLeading: Boolean,
    content: @Composable () -> Unit,
) {
    val typography = GlimmerTheme.typography
    val componentSpacingValues = GlimmerTheme.componentSpacingValues
    val innerPadding = componentSpacingValues.small
    val imageSpacing = componentSpacingValues.medium

    Row(
        modifier =
            modifier
                .defaultMinSize(minHeight = MinimumHeight)
                .fillMaxWidth()
                .padding(contentPadding),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (isLeading) {
            Box(
                modifier = Modifier.weight(ImageWidthFraction).clip(ImageShape),
                contentAlignment = Alignment.Center,
            ) {
                image()
            }
        }

        Column(
            modifier =
                Modifier.weight(1f - ImageWidthFraction)
                    .padding(
                        start = if (isLeading) imageSpacing else innerPadding,
                        end = if (isLeading) innerPadding else imageSpacing,
                    ),
            verticalArrangement = Arrangement.spacedBy(TextVerticalSpacing),
        ) {
            if (title != null) {
                CompositionLocalProvider(
                    LocalTextStyle provides typography.bodyMedium,
                    content = title,
                )
            }

            if (subtitle != null) {
                CompositionLocalProvider(
                    LocalTextStyle provides typography.caption,
                    content = subtitle,
                )
            }

            CompositionLocalProvider(
                LocalTextStyle provides typography.bodySmall,
                content = content,
            )
        }

        if (!isLeading) {
            Box(
                modifier = Modifier.weight(ImageWidthFraction).clip(ImageShape),
                contentAlignment = Alignment.Center,
            ) {
                image()
            }
        }
    }
}

/**
 * A custom [Arrangement.Vertical] used by [CardImpl] that pins the action to the bottom of the card
 * and centers the body content in the remaining space above it.
 *
 * For regular cards (single child), it vertically centers the body within the card. For action
 * cards (two children), it places the action at the bottom of the available space and centers the
 * body in the remaining space above the action.
 */
private object CardVerticalArrangement : Arrangement.Vertical {
    override fun Density.arrange(totalSize: Int, sizes: IntArray, outPositions: IntArray) {
        if (sizes.isEmpty()) return
        if (sizes.size == 1) {
            outPositions[0] = CenterVertically.align(sizes[0], totalSize)
            return
        }

        val bodyHeight = sizes[0]
        val actionHeight = sizes[1]
        val availableBodyHeight = totalSize - actionHeight

        // Sets outPositions[0] using CenterVertically on the available space
        outPositions[0] = CenterVertically.align(size = bodyHeight, space = availableBodyHeight)
        outPositions[1] = totalSize - actionHeight
    }
}

/**
 * Constrains the content's height to a maximum aspect ratio, based on the maximum width.
 *
 * This modifier is similar to [androidx.compose.foundation.layout.aspectRatio], but it only
 * enforces a maximum size, allowing the content to be smaller than the bounds defined by the aspect
 * ratio. It also only constrains the height based on the width, it does not constrain the width
 * based on the height.
 *
 * @param widthToHeightRatio the maximum aspect ratio allowed for the height. This is defined as the
 *   ratio of width / height
 */
private fun Modifier.constrainHeightToAspectRatio(widthToHeightRatio: Float): Modifier {
    require(widthToHeightRatio > 0) { "Ratio must be positive" }
    return this.layout { measurable, constraints ->
        // We only want to constrain height, based on width. If width is unbounded and there is a
        // bounded height, we don't want to constrain the width based on height. So do nothing if
        // we don't have a constrained width
        if (!constraints.hasBoundedWidth) {
            val placeable = measurable.measure(constraints)
            return@layout layout(placeable.width, placeable.height) {
                placeable.placeRelative(0, 0)
            }
        }

        val height =
            (constraints.maxWidth / widthToHeightRatio)
                .roundToInt()
                // Handle the case where the width is more than ratio times larger than available
                // height
                .coerceAtMost(constraints.maxHeight)

        val newConstraints =
            constraints.copy(
                // Relax minimum height to let the content be smaller than constraints.minHeight if
                // the aspect ratio results in a height smaller than min height
                minHeight = 0,
                maxHeight = height,
            )

        val placeable = measurable.measure(newConstraints)

        // We relaxed the constraints earlier, but we still need to respect the incoming constraints
        // ourselves.
        val layoutHeight = placeable.height.coerceIn(constraints.minHeight, constraints.maxHeight)

        layout(placeable.width, layoutHeight) {
            // Center the content within the final layout height if needed
            val y = (layoutHeight - placeable.height) / 2
            placeable.placeRelative(0, y)
        }
    }
}

/** Default values used for [Card] */
public object CardDefaults {
    /**
     * Default content padding used for a [Card]
     *
     * This affects the outermost content padding applied around the content container. Note that
     * there is additional padding applied around the content / text / icons inside a card, this
     * only represents the outer padding for the entire content.
     */
    public val contentPadding: PaddingValues
        @Composable get() = PaddingValues(GlimmerTheme.componentSpacingValues.medium)

    /** The default shape of [Card], which determines its corner radius. */
    public val shape: Shape
        @Composable get() = GlimmerTheme.shapes.medium

    /** The default background color of [Card]. */
    public val color: Color
        @Composable get() = GlimmerTheme.colors.surface

    /**
     * Calculates the default content color for a [Card] based on the provided [color].
     *
     * @param color the background color of the card
     * @return the calculated content color
     */
    @Composable
    public fun contentColor(color: Color = CardDefaults.color): Color {
        return calculateContentColor(color)
    }
}

/** Default values used for [ActionCard] */
public object ActionCardDefaults {
    /**
     * Default content padding used for an [ActionCard]
     *
     * This affects the outermost content padding applied around the content container. Note that
     * there is additional padding applied around the content / text / icons inside an action card,
     * this only represents the outer padding for the entire content.
     */
    public val contentPadding: PaddingValues
        @Composable get() = CardDefaults.contentPadding

    /** The default shape of [ActionCard], which determines its corner radius. */
    public val shape: Shape
        @Composable get() = CardDefaults.shape

    /** The default background color of [ActionCard]. */
    public val color: Color
        @Composable get() = CardDefaults.color

    /**
     * Calculates the default content color for an [ActionCard] based on the provided [color].
     *
     * @param color the background color of the action card
     * @return the calculated content color
     */
    @Composable
    public fun contentColor(color: Color = ActionCardDefaults.color): Color {
        return CardDefaults.contentColor(color)
    }
}

/** Default values used for [ImageCard] */
public object ImageCardDefaults {
    /**
     * Default content padding used for an [ImageCard]
     *
     * This affects the outermost content padding applied around images and the content container.
     * Note that there is additional padding applied around the content / text / icons inside an
     * image card, this only represents the outer padding for the entire content.
     */
    public val contentPadding: PaddingValues
        @Composable get() = CardDefaults.contentPadding

    /** The default shape of [ImageCard], which determines its corner radius. */
    public val shape: Shape
        @Composable get() = CardDefaults.shape

    /** The default background color of [ImageCard]. */
    public val color: Color
        @Composable get() = CardDefaults.color

    /**
     * Calculates the default content color for an [ImageCard] based on the provided [color].
     *
     * @param color the background color of the image card
     * @return the calculated content color
     */
    @Composable
    public fun contentColor(color: Color = ImageCardDefaults.color): Color {
        return CardDefaults.contentColor(color)
    }
}

/** Default values used for [LeadingImageCard] */
public object LeadingImageCardDefaults {
    /**
     * Default content padding used for a [LeadingImageCard]
     *
     * This affects the outermost content padding applied around the image and the content
     * container. Note that there is additional padding applied around the content / text / image
     * inside a leading image card, this only represents the outer padding for the entire content.
     */
    public val contentPadding: PaddingValues
        @Composable get() = CardDefaults.contentPadding

    /** The default shape of [LeadingImageCard], which determines its corner radius. */
    public val shape: Shape
        @Composable get() = CardDefaults.shape

    /** The default background color of [LeadingImageCard]. */
    public val color: Color
        @Composable get() = CardDefaults.color

    /**
     * Calculates the default content color for a [LeadingImageCard] based on the provided [color].
     *
     * @param color the background color of the leading image card
     * @return the calculated content color
     */
    @Composable
    public fun contentColor(color: Color = LeadingImageCardDefaults.color): Color {
        return CardDefaults.contentColor(color)
    }
}

/** Default values used for [TrailingImageCard] */
public object TrailingImageCardDefaults {
    /**
     * Default content padding used for a [TrailingImageCard]
     *
     * This affects the outermost content padding applied around the image and the content
     * container. Note that there is additional padding applied around the content / text / image
     * inside a trailing image card, this only represents the outer padding for the entire content.
     */
    public val contentPadding: PaddingValues
        @Composable get() = CardDefaults.contentPadding

    /** The default shape of [TrailingImageCard], which determines its corner radius. */
    public val shape: Shape
        @Composable get() = CardDefaults.shape

    /** The default background color of [TrailingImageCard]. */
    public val color: Color
        @Composable get() = CardDefaults.color

    /**
     * Calculates the default content color for a [TrailingImageCard] based on the provided [color].
     *
     * @param color the background color of the trailing image card
     * @return the calculated content color
     */
    @Composable
    public fun contentColor(color: Color = TrailingImageCardDefaults.color): Color {
        return CardDefaults.contentColor(color)
    }
}

/** Default minimum height for a [Card] */
private val MinimumHeight = 80.dp

/** Spacing between title / subtitle / body text */
private val TextVerticalSpacing = 3.dp

/** Shape used to clip the image in a card */
private val ImageShape = RoundedCornerShape(24.dp)

/**
 * Width / height aspect ratio for the image used in [ImageCard], to prevent it from taking up too
 * much vertical space
 */
private const val HeaderImageMaximumAspectRatio = 1.6f

/** Maximum width fraction for leading and trailing images in a card */
private const val ImageWidthFraction = 0.3f
```

<br />