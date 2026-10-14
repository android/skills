package com.example.platform.ui.appwidgets.glance.layout.collections.data

import androidx.glance.GlanceId
import com.example.platform.ui.appwidgets.glance.layout.collections.layout.CheckListItem
import com.example.platform.ui.appwidgets.glance.layout.computeIfAbsent as computeIfAbsentExt
import com.example.platform.ui.appwidgets.glance.layout.removeIf as removeIfExt
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * A fake in-memory implementation of a repository that produces a list of [CheckListItem]s.
 */
class FakeCheckListDataRepository {
  private val items = MutableStateFlow<List<CheckListItem>>(listOf())
  private val checkedItems = MutableStateFlow<List<String>>(listOf())

  /**
   * Flow of [CheckListItem]s that can be listened to during a Glance session.
   */
  fun items(): Flow<List<CheckListItem>> = items

  /**
   * Flow of keys of [CheckListItem]s that are checked. This flow can be listened to during a
   * Glance session.
   */
  fun checkedItems(): Flow<List<String>> = checkedItems

  @OptIn(DelicateCoroutinesApi::class)
  fun checkItem(key: String) {
    GlobalScope.launch {
      withContext(Dispatchers.IO) {
        checkedItems.value = checkedItems.value.toMutableList().apply { add(key) }

        // Mimics backend processing that removes the item from backend database.
        // Until it's removed from the backend, because it's in checkedItems, it appears as
        // checked on the screen. Then, once the backend is updated, the items list won't contain it and
        // it's removed from the UI.
        delay(500)

        items.value = items.value.toMutableList().apply {
          removeIfExt { item ->
            item.key == key
          }
        }
        checkedItems.value = checkedItems.value.toMutableList().apply { remove(key) }
      }
    }
  }

  /**
   * Loads the [CheckListItem]s from the selected data source.
   */
  fun load(): List<CheckListItem> {
    items.value = demoData
    checkedItems.value = listOf()

    return items.value
  }

  companion object {
    private val repositories = mutableMapOf<GlanceId, FakeCheckListDataRepository>()

    val demoData = listOf(
      CheckListItem(
        key = "0",
        title = "Pay electricity bill",
        supportingText = "Due in 10 days",
      ),
      CheckListItem(
        key = "1",
        title = "Prepare for the meeting",
        supportingText = "Due tomorrow"
      ),
      CheckListItem(
        key = "2",
        title = "Renew lease",
        supportingText = "Due in 1 month",
      ),
      CheckListItem(
        key = "3",
        title = "Plan the trip",
        supportingText = "Due tomorrow",
      ),
      CheckListItem(
        key = "4",
        title = "Call plumber",
        supportingText = "Due today",
      ),
      CheckListItem(
        key = "5",
        title = "Dentist appointment",
        supportingText = "Due in 1 week",
      ),
      CheckListItem(
        key = "6",
        title = "Eye appointment",
        supportingText = "Due in 1 month",
      ),
    )

    /**
     * Returns the repository instance for the given widget represented by [glanceId].
     */
    fun getCheckListDataRepo(glanceId: GlanceId): FakeCheckListDataRepository {
      return synchronized(repositories) {
          repositories.computeIfAbsentExt(glanceId) { FakeCheckListDataRepository() }!!
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
  }
}
