/*
 * Copyright 2023 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.platform.ui.appwidgets.glance.layout

/**
 * This function should be removed when API level is raised to 24. It exists to provide removeIf
 * from java.util.Collection
 */
fun <T> MutableList<T>.removeIf(filter: (T) -> Boolean): Boolean {

    var removed = false
    var toRemove = mutableListOf<T>()

    forEach { item ->
        if (filter(item)) {
            removed = true
            toRemove.add(item)
        }
    }

    removeAll(toRemove)
    return removed

}

fun <K, V> MutableMap<K,V>.computeIfAbsent(key : K, calculation :(K)->V):V? {
    var value = get(key)

    if (value == null) {
        value = calculation(key)
        put(key, value)
    }
    return value
}
