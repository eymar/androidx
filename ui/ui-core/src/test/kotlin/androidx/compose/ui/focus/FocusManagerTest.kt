/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.compose.ui.focus

import androidx.compose.runtime.collection.ExperimentalCollectionApi
import androidx.compose.ui.FocusModifier2
import androidx.compose.ui.focus.FocusState2.Active
import androidx.compose.ui.focus.FocusState2.ActiveParent
import androidx.compose.ui.focus.FocusState2.Captured
import androidx.compose.ui.focus.FocusState2.Disabled
import androidx.compose.ui.focus.FocusState2.Inactive
import androidx.compose.ui.node.ExperimentalLayoutNodeApi
import androidx.compose.ui.node.InnerPlaceable
import androidx.compose.ui.node.LayoutNode
import androidx.compose.ui.node.ModifiedFocusNode2
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import kotlin.jvm.JvmStatic

@OptIn(
    ExperimentalCollectionApi::class,
    ExperimentalFocus::class,
    ExperimentalLayoutNodeApi::class
)
@RunWith(Parameterized::class)
class FocusManagerTest(val initialFocusState: FocusState2) {
    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "rootInitialFocus = {0}")
        fun initParameters() = FocusState2.values()
    }

    private val focusManager = FocusManager()
    private val focusModifier: FocusModifier2 = focusManager.modifier as FocusModifier2

    @Before
    fun setup() {
        val innerPlaceable = InnerPlaceable(LayoutNode())
        focusModifier.focusNode = ModifiedFocusNode2(innerPlaceable, focusModifier)
    }

    @Test
    fun defaultFocusState() {
        assertThat(focusModifier.focusState).isEqualTo(Inactive)
    }

    @Test
    fun takeFocus_onlyInactiveChangesState() {
        // Arrange.
        focusModifier.focusState = initialFocusState

        // Act.
        focusManager.takeFocus()

        // Assert.
        assertThat(focusModifier.focusState).isEqualTo(
            when (initialFocusState) {
                Inactive -> Active
                Active, ActiveParent, Captured, Disabled -> initialFocusState
            }
        )
    }

    @Test
    fun clearFocus_changesStateToInactive() {
        // Arrange.
        focusModifier.focusState = initialFocusState
        if (initialFocusState == ActiveParent) {
            val childLayoutNode = LayoutNode()
            val child = ModifiedFocusNode2(InnerPlaceable(childLayoutNode), FocusModifier2(Active))
            focusModifier.focusNode.layoutNode._children.add(childLayoutNode)
            focusModifier.focusedChild = child
        }

        // Act.
        focusManager.releaseFocus()

        // Assert.
        assertThat(focusModifier.focusState).isEqualTo(
            when (initialFocusState) {
                Active, ActiveParent, Captured, Inactive -> Inactive
                Disabled -> initialFocusState
            }
        )
    }

    @Test
    fun clearFocus_childIsCaptured() {
        // Arrange.
        focusModifier.focusState = ActiveParent
        val childLayoutNode = LayoutNode()
        val child = ModifiedFocusNode2(InnerPlaceable(childLayoutNode), FocusModifier2(Captured))
        focusModifier.focusNode.layoutNode._children.add(childLayoutNode)
        focusModifier.focusedChild = child

        // Act.
        focusManager.clearFocus()

        // Assert.
        assertThat(focusModifier.focusState).isEqualTo(ActiveParent)
    }
}
