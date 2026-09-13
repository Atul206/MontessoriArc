package com.calmcoloring.app.navigation

/**
 * The app's two-screen navigation graph, represented as a plain sealed
 * interface rather than `androidx.navigation3`'s `NavKey`. Navigation 3
 * (`org.jetbrains.androidx.navigation3`) is ruled out for this project: its
 * iOS klibs require Kotlin 2.3.20's compiler ABI, while this project is
 * pinned to Kotlin 2.2.0 (see Task 0's report for the full story). A hand
 * rolled back stack is a better fit anyway for an app with exactly two
 * routes.
 *
 * No `kotlinx.serialization` here either: `@Serializable` on the library's
 * `NavKey` existed to support its deep-link/process-death state restoration
 * machinery, which this hand rolled stack does not use. If process-death
 * restoration is wanted later, it would need its own mechanism (e.g. a
 * `rememberSaveable` string encoding of the back stack) independent of this
 * type.
 */
sealed interface Route {
    data object Gallery : Route
    data class Coloring(val templateId: String) : Route
}
