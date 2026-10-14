package com.example.platform.ui.appwidgets.glance.layout.text.data

import androidx.glance.GlanceId
import com.example.platform.ui.appwidgets.glance.layout.text.layout.LongTextLayoutData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import com.example.platform.ui.appwidgets.glance.layout.computeIfAbsent as computeIfAbsentExt
/**
 * An fake in-memory repository to provide data for displaying different demo samples in
 * [com.example.platform.ui.appwidgets.glance.layout.text.layout.LongTextLayout]
 */
class FakeLongTextRepository {
  private var itemIndex: Int = 0
  private var itemsCount: Int = 0
  private val data = MutableStateFlow(demoItems[0])

  fun data(): Flow<LongTextLayoutData> = data

  /**
   * Mimics refresh by returning a different data item from the demo data list.
   *
   * This allows us to try the layout with various texts pertaining to a specific kind of data.
   */
  fun refresh() {
    itemIndex = (itemIndex + 1) % itemsCount

    this.load()
  }

  /** Loads the data and updates the flow */
  fun load(): LongTextLayoutData {
    itemsCount = demoItems.size
    data.value = demoItems[itemIndex]

    return data.value
  }

  companion object {
    private val repositories = mutableMapOf<GlanceId, FakeLongTextRepository>()

    /**
     * Returns the repository instance for the given widget represented by [glanceId].
     */
    fun getRepo(glanceId: GlanceId): FakeLongTextRepository {
      return synchronized(repositories) {
        repositories.computeIfAbsentExt(glanceId) {
          FakeLongTextRepository()
        }!!
      }
    }

    /**
     * Cleans up local data associated with the provided [glanceId].
     */
    fun cleanUp(glanceId: GlanceId) {
      synchronized(repositories) {
        repositories.remove(glanceId)
      }
    }

    // Lorem ipsum text generated with https://loremipsum.io/
    val demoItems = listOf(
      LongTextLayoutData(
        key = "item 0",
        text = "This is allows for a longer text string. Specifically because the focus in this, layout is on the primary text.",
        caption = "Caption",
      ),
      LongTextLayoutData(
        key = "item 1",
        text = "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua.",
        caption = "Ut mollis",
      ),
      LongTextLayoutData(
        key = "item 2",
        text = "Cursus mattis molestie a iaculis at erat pellentesque adipiscing commodo elit at imperdiet dui accumsan sit amet.",
        caption = "Ipsum faucibus",
      ),
      LongTextLayoutData(
        key = "item 3",
        text = "Tellus orci ac auctor augue mauris augue neque gravida in fermentum et sollicitudin ac orci",
        caption = "Amet cursus"
      ),
      LongTextLayoutData(
        key = "item 4",
        text = "Dolor sit amet consectetur adipiscing elit duis tristique sollicitudin nibh sit amet commodo nulla facilisi nullam.",
        caption = "Amet cursus"
      ),
    )
  }
}
