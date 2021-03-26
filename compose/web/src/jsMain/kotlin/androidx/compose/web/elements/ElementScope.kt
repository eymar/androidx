/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.compose.web.elements

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.DisposableEffectResult
import androidx.compose.runtime.DisposableEffectScope
import androidx.compose.runtime.NonRestartableComposable
import org.w3c.dom.HTMLElement

interface ElementScope {

    @Composable
    @NonRestartableComposable
    fun DisposableRefEffect(
        key: Any?,
        effect: DisposableEffectScope.(HTMLElement) -> DisposableEffectResult
    )

    @Composable
    @NonRestartableComposable
    fun DisposableRefEffect(
        effect: DisposableEffectScope.(HTMLElement) -> DisposableEffectResult
    ) {
        DisposableRefEffect(null, effect)
    }
}

class ElementScopeImpl : ElementScope {
    lateinit var element: HTMLElement

    @Composable
    @NonRestartableComposable
    override fun DisposableRefEffect(
        key: Any?,
        effect: DisposableEffectScope.(HTMLElement) -> DisposableEffectResult
    ) {
        DisposableEffect(key) { effect(element) }
    }
}