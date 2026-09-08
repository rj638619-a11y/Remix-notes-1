package com.example.ui.animations

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.ui.viewmodel.NotesViewModel

/**
 * 1. Modifier.sharedNoteKey - Shared element with key "note-$noteId", boundsTransform uses spring(0.2, 350).
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun Modifier.sharedNoteKey(
    sharedTransitionScope: SharedTransitionScope,
    noteId: String,
    animatedVisibilityScope: AnimatedVisibilityScope
): Modifier = with(sharedTransitionScope) {
    this@sharedNoteKey.sharedElement(
        state = rememberSharedContentState(key = "note-$noteId"),
        animatedVisibilityScope = animatedVisibilityScope,
        boundsTransform = { _, _ ->
            spring(dampingRatio = 0.2f, stiffness = 350f)
        }
    )
}

/**
 * 2. Modifier.sharedVaultAppKey - Shared element with key "vault-app-$appName", boundsTransform uses spring(0.2, 350).
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun Modifier.sharedVaultAppKey(
    sharedTransitionScope: SharedTransitionScope,
    appName: String,
    animatedVisibilityScope: AnimatedVisibilityScope
): Modifier = with(sharedTransitionScope) {
    this@sharedVaultAppKey.sharedElement(
        state = rememberSharedContentState(key = "vault-app-$appName"),
        animatedVisibilityScope = animatedVisibilityScope,
        boundsTransform = { _, _ ->
            spring(dampingRatio = 0.2f, stiffness = 350f)
        }
    )
}

/**
 * 3. GlassNotesNavHost - Wraps content in SharedTransitionLayout, NavHost with routes "notes" and "editor/{noteId}".
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun GlassNotesNavHost(
    navController: NavHostController,
    viewModel: NotesViewModel,
    modifier: Modifier = Modifier,
    notesContent: @Composable (SharedTransitionScope, AnimatedVisibilityScope) -> Unit = { _, _ -> },
    editorContent: @Composable (SharedTransitionScope, AnimatedVisibilityScope, String) -> Unit = { _, _, _ -> }
) {
    SharedTransitionLayout(modifier = modifier.fillMaxSize()) {
        NavHost(
            navController = navController,
            startDestination = "notes"
        ) {
            composable("notes") {
                notesContent(this@SharedTransitionLayout, this@composable)
            }
            composable("editor/{noteId}") { backStackEntry ->
                val noteId = backStackEntry.arguments?.getString("noteId") ?: ""
                editorContent(this@SharedTransitionLayout, this@composable, noteId)
            }
        }
    }
}
