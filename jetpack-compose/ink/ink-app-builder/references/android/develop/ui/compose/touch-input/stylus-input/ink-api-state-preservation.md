State preservation and persistent storage are non-trivial aspects of inking
apps, especially in Compose.
The core data objects, such as brush properties and the points that form a
stroke, are complex and don't automatically persist.
This requires a deliberate strategy for saving state during scenarios like
configuration changes and permanently saving a user's drawings to a database.

## State preservation

In Jetpack Compose, UI state is typically managed using
[`remember`](https://developer.android.com/reference/kotlin/androidx/compose/runtime/package-summary#remember)
and
[`rememberSaveable`](https://developer.android.com/reference/kotlin/androidx/compose/runtime/saveable/rememberSaveable.composable#rememberSaveable(kotlin.Array,androidx.compose.runtime.saveable.Saver,kotlin.String,kotlin.Function0)).
While
[`rememberSaveable`](https://developer.android.com/reference/kotlin/androidx/compose/runtime/saveable/rememberSaveable.composable#rememberSaveable(kotlin.Array,androidx.compose.runtime.saveable.Saver,kotlin.String,kotlin.Function0))
offers automatic state preservation across configuration changes, its built-in
capabilities are limited to primitive data types and objects that implement
[`Parcelable`](https://developer.android.com/reference/kotlin/android/os/Parcelable) or
[`Serializable`](https://developer.android.com/reference/java/io/Serializable).

For custom objects that contain complex properties, such as
[`Brush`](https://developer.android.com/reference/kotlin/androidx/ink/brush/Brush), you must define explicit
serialization and deserialization mechanisms using a custom state saver. By
defining a custom [`Saver`](https://developer.android.com/reference/kotlin/androidx/compose/runtime/saveable/Saver)
for the `Brush` object, you can preserve the brush's essential attributes when
configuration changes occur, as shown in the following `brushStateSaver`
example.

    fun brushStateSaver(converters: Converters): Saver<MutableState<Brush>, SerializedBrush> = Saver(
        save = { converters.serializeBrush(it.value) },
        restore = { mutableStateOf(converters.deserializeBrush(it)) },
    )

You can then use the custom [`Saver`](https://developer.android.com/reference/kotlin/androidx/compose/runtime/saveable/Saver) to
preserve the selected brush state:

    val currentBrush = rememberSaveable(saver = brushStateSaver(Converters())) { mutableStateOf(defaultBrush) }

## Persistent Storage

To enable features such as document saving, loading, and potential real-time
collaboration, store strokes and associated data in a serialized format. For the
Ink API, manual serialization and deserialization are necessary.

To accurately restore a stroke, save its `Brush` and `StrokeInputBatch`.

- [`Brush`](https://developer.android.com/reference/kotlin/androidx/ink/brush/Brush): Includes numeric fields (size, epsilon), color, and [`BrushFamily`](https://developer.android.com/reference/kotlin/androidx/ink/brush/BrushFamily).
- [`StrokeInputBatch`](https://developer.android.com/reference/kotlin/androidx/ink/strokes/StrokeInputBatch): A list of input points with numeric fields.

The Storage module simplifies compactly serializing the most complex part: the
[`StrokeInputBatch`](https://developer.android.com/reference/kotlin/androidx/ink/strokes/StrokeInputBatch).

> [!NOTE]
> **Note:** See data transfer objects such as `SerializedBrush` and `SerializedStroke` as well as the `Converters` helper class in the [*Data object and converter
> helpers* section](https://developer.android.com/develop/ui/compose/touch-input/stylus-input/ink-api-state-preservation#data_object_and_converter_helpers).

To save a stroke:

- Serialize the `StrokeInputBatch` using the storage module's encode function. Store the resulting binary data.
- Separately save the essential properties of the stroke's Brush:
  - The enum that represents the brush family \&mdash Although the instance can be serialized, this is not efficient for apps that use a limited selection of brush families
  - `colorLong`
  - `size`
  - `epsilon`

    fun serializeStroke(stroke: Stroke): SerializedStroke {
      val serializedBrush = serializeBrush(stroke.brush)
      val encodedSerializedInputs = ByteArrayOutputStream().use
        {
          stroke.inputs.encode(it)
          it.toByteArray()
        }

      return SerializedStroke(
        inputs = encodedSerializedInputs,
        brush = serializedBrush
      )
    }

To load a stroke object:

- Retrieve the saved binary data for the `StrokeInputBatch` and deserialize it using the storage module's [decode()](https://developer.android.com/reference/kotlin/androidx/ink/strokes/StrokeInputBatch.Companion#(androidx.ink.strokes.StrokeInputBatch.Companion).decode(java.io.InputStream)) function.
- Retrieve the saved `Brush` properties and create the brush.
- Create the final stroke using the recreated brush and the deserialized
  [`StrokeInputBatch`](https://developer.android.com/reference/kotlin/androidx/ink/strokes/StrokeInputBatch).

      fun deserializeStroke(serializedStroke: SerializedStroke): Stroke {
        val inputs = ByteArrayInputStream(serializedStroke.inputs).use {
          StrokeInputBatch.decode(it)
        }
        val brush = deserializeBrush(serializedStroke.brush)
        return Stroke(brush = brush, inputs = inputs)
      }

#### Handle zoom, pan, and rotation

If your app supports zooming, panning, or rotation, you must provide the current
transformation to `InProgressStrokes`. This helps newly drawn strokes match the
position and scale of your existing strokes.

You do this by passing a `Matrix` to the `pointerEventToWorldTransform`
parameter. The matrix should represent the inverse of the transformation you
apply to your finished strokes canvas.

    @Composable
    fun ZoomableDrawingScreen(...) {
        // 1. Manage your zoom/pan state (e.g., using detectTransformGestures).
        var zoom by remember { mutableStateOf(1f) }
        var pan by remember { mutableStateOf(Offset.Zero) }

        // 2. Create the Matrix.
        val pointerEventToWorldTransform = remember(zoom, pan) {
            android.graphics.Matrix().apply {
                // Apply the inverse of your rendering transforms
                postTranslate(-pan.x, -pan.y)
                postScale(1 / zoom, 1 / zoom)
            }
        }

        Box(modifier = Modifier.fillMaxSize()) {
            // ...Your finished strokes Canvas, with regular transform applied

            // 3. Pass the matrix to InProgressStrokes.
            InProgressStrokes(
                modifier = Modifier.fillMaxSize(),
                pointerEventToWorldTransform = pointerEventToWorldTransform,
                defaultBrush = currentBrush,
                nextBrush = onGetNextBrush,
                onStrokesFinished = onStrokesFinished
            )
        }
    }

> [!NOTE]
> **Note:** For details on implementing the gestures themselves, refer to the Jetpack Compose documentation for [detectTransformGestures](https://developer.android.com/reference/kotlin/androidx/compose/foundation/gestures/package-summary#(androidx.compose.ui.input.pointer.PointerInputScope).detectTransformGestures(kotlin.Boolean,kotlin.Function4)).

#### Export strokes

You might need to export your stroke scene as a static image file. This is
useful for sharing the drawing with other applications, generating thumbnails,
or saving a final, uneditable version of the content.

To export a scene, you can render your strokes to an offscreen bitmap instead of
directly to the screen. Use
[`Android's Picture API`](https://developer.android.com/reference/android/graphics/Picture), which lets you record drawings on a canvas without
needing a visible UI component.

The process involves creating a `Picture` instance, calling `beginRecording()`
to get a `Canvas`, and then using your existing `CanvasStrokeRenderer` to draw
each stroke onto that `Canvas`. After you record all the drawing commands, you
can use the `Picture` to create a [`Bitmap`](https://developer.android.com/reference/android/graphics/Bitmap),
which you can then compress and save to a file.

    fun exportDocumentAsImage() {
      val picture = Picture()
      val canvas = picture.beginRecording(bitmapWidth, bitmapHeight)

      // The following is similar logic that you'd use in your custom View.onDraw or Compose Canvas.
      for (item in myDocument) {
        when (item) {
          is Stroke -> {
            canvasStrokeRenderer.draw(canvas, stroke, worldToScreenTransform)
          }
          // Draw your other types of items to the canvas.
        }
      }

      // Create a Bitmap from the Picture and write it to a file.
      val bitmap = Bitmap.createBitmap(picture)
      val outstream = FileOutputStream(filename)
      bitmap.compress(Bitmap.CompressFormat.PNG, 100, outstream)
    }

#### Data object and converter helpers

Define a serialization object structure that mirrors needed Ink API objects.

Use the Ink API's storage module to encode and decode `StrokeInputBatch`.

##### Data transfer objects

    @Parcelize
    @Serializable
    data class SerializedStroke(
      val inputs: ByteArray,
      val brush: SerializedBrush
    ) : Parcelable {
      override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SerializedStroke) return false
        if (!inputs.contentEquals(other.inputs)) return false
        if (brush != other.brush) return false
        return true
      }

      override fun hashCode(): Int {
        var result = inputs.contentHashCode()
        result = 31 * result + brush.hashCode()
        return result
      }
    }

    @Parcelize
    @Serializable
    data class SerializedBrush(
      val size: Float,
      val color: Long,
      val epsilon: Float,
      val stockBrush: SerializedStockBrush,
      val clientBrushFamilyId: String? = null
    ) : Parcelable

    enum class SerializedStockBrush {
      Marker,
      PressurePen,
      Highlighter,
      DashedLine,
    }

##### Converters

    object Converters {
      private val stockBrushToEnumValues = mapOf(
        StockBrushes.marker() to SerializedStockBrush.Marker,
        StockBrushes.pressurePen() to SerializedStockBrush.PressurePen,
        StockBrushes.highlighter() to SerializedStockBrush.Highlighter,
        StockBrushes.dashedLine() to SerializedStockBrush.DashedLine,
      )

      private val enumToStockBrush =
        stockBrushToEnumValues.entries.associate { (key, value) -> value to key
      }

      private fun serializeBrush(brush: Brush): SerializedBrush {
        return SerializedBrush(
          size = brush.size,
          color = brush.colorLong,
          epsilon = brush.epsilon,
          stockBrush = stockBrushToEnumValues[brush.family] ?: SerializedStockBrush.Marker,
        )
      }

      fun serializeStroke(stroke: Stroke): SerializedStroke {
        val serializedBrush = serializeBrush(stroke.brush)
        val encodedSerializedInputs = ByteArrayOutputStream().use { outputStream ->
          stroke.inputs.encode(outputStream)
          outputStream.toByteArray()
        }

        return SerializedStroke(
          inputs = encodedSerializedInputs,
          brush = serializedBrush
        )
      }

      private fun deserializeStroke(
        serializedStroke: SerializedStroke,
      ): Stroke? {
        val inputs = ByteArrayInputStream(serializedStroke.inputs).use { inputStream ->
            StrokeInputBatch.decode(inputStream)
        }
        val brush = deserializeBrush(serializedStroke.brush, customBrushes)
        return Stroke(brush = brush, inputs = inputs)
      }

      private fun deserializeBrush(
        serializedBrush: SerializedBrush,
      ): Brush {
        val stockBrushFamily = enumToStockBrush[serializedBrush.stockBrush]
        val brushFamily = customBrush?.brushFamily ?: stockBrushFamily ?: StockBrushes.marker()

        return Brush.createWithColorLong(
          family = brushFamily,
          colorLong = serializedBrush.color,
          size = serializedBrush.size,
          epsilon = serializedBrush.epsilon,
        )
      }
    }