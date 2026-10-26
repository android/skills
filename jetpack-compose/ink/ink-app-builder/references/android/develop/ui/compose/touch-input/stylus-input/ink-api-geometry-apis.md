The Geometry APIs let you create interactive tools such as selection
mechanisms and erasers.

This section shows how to use the Geometry APIs to implement an eraser.

    private fun eraseIntersectingStrokes(
      currentX: Float,
      currentY: Float,
      currentStrokes: MutableList<Stroke>,
      ): Unit {
        val prev = previousPoint
        previousPoint = MutableVec(currentX, currentY)
        if (prev == null) return

        val segment = MutableSegment(prev, MutableVec(currentX, currentY))
        val parallelogram = MutableParallelogram().populateFromSegmentAndPadding(
          segment,
          eraserPadding
        )
        currentStrokes.removeAll {
            it.shape.intersects(parallelogram, AffineTransform.IDENTITY)
        }
    }

> [!NOTE]
> **Note:** You can implement an eraser that only removes the parts of strokes it touches by checking if a stroke intersects with individual line segments of a [`StrokeInputBatch`](https://developer.android.com/reference/kotlin/androidx/ink/strokes/StrokeInputBatch).  
>
> Then, create new `StrokeInputBatch` and [`Stroke`](https://developer.android.com/reference/kotlin/androidx/ink/strokes/Stroke) objects from the line segments that weren't intersected.