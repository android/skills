To help you author strokes in an idiomatic Compose way, the Ink API provides
Compose interoperability modules for authoring, brush, and geometry.

To draw a stroke in Compose, use the
[`InProgressStrokes`](https://developer.android.com/reference/kotlin/androidx/ink/authoring/compose/InProgressStrokes.composable#InProgressStrokes(androidx.ink.brush.Brush,kotlin.Function0,androidx.compose.ui.graphics.Matrix,androidx.compose.ui.graphics.Matrix,androidx.compose.ui.graphics.Path,androidx.ink.brush.TextureBitmapStore,kotlin.Function1)) composable that requires a
default brush instance, a way to override the default brush, and a callback
that handles finished strokes.

1. Set up UI component

       InProgressStrokes(
         defaultBrush = currentBrush,
         nextBrush = onGetNextBrush,
         onStrokesFinished = onStrokesFinished,
       )

2. Handle finished strokes

   When wet strokes become dry, they are passed to the application through the
   `onStrokesFinished` callback argument of `InProgressStrokes`.

   Your app must pass the finished strokes to another Composable within the same
   UI thread to commit them to the screen.

       @Composable
       fun DrawingScreen(
         finishedStrokes: List<Strokes>,
         onStrokesFinished: (List<Stroke>) -> Unit,
         currentBrush: Brush,
         onGetNextBrush: () -> Brush,
         modifier: Modifier = Modifier
       ) {
         val canvasStrokeRenderer = remember { CanvasStrokeRenderer.create() }

         Box(modifier = Modifier.fillMaxSize()) {
             // The Canvas for drawing the permanent, dry strokes.
             Canvas(modifier = Modifier.fillMaxSize()) {
                 finishedStrokes.forEach { stroke ->
                     canvasStrokeRenderer.draw(
                         stroke = stroke,
                         canvas = this,
                         strokeToScreenTransform = Matrix()
                     )
                 }
             }

             //The wet ink layer for live drawing.
             // The InProgressStrokes composable for the wet ink layer goes here.
         }
       }