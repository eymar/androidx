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

package androidx.compose.js

import androidx.compose.js.events.EventModifier
import androidx.compose.runtime.AbstractApplier
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ComposeNode
import androidx.compose.runtime.Composition
import androidx.compose.runtime.ControlledComposition
import androidx.compose.runtime.DefaultMonotonicFrameClock
import androidx.compose.runtime.Recomposer
import kotlinx.browser.document
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.dom.clear
import org.w3c.dom.Element
import org.w3c.dom.HTMLElement
import org.w3c.dom.Node
import org.w3c.dom.Text
import org.w3c.dom.get

fun renderComposable(root: Element, content: @Composable () -> Unit): Composition {
    GlobalSnapshotManager.ensureStarted()

    val context = DefaultMonotonicFrameClock + Dispatchers.Main
    val recomposer = Recomposer(context)
    val composition = ControlledComposition(
        applier = DomApplier(DomNodeWrapper(root)),
        parent = recomposer
    )
    composition.setContent(content)

    CoroutineScope(context).launch(start = CoroutineStart.UNDISPATCHED) {
        recomposer.runRecomposeAndApplyChanges()
    }
    return composition
}

class DomApplier(
    root: DomNodeWrapper
) : AbstractApplier<DomNodeWrapper>(root) {

    override fun insertTopDown(index: Int, instance: DomNodeWrapper) {
        // ignored. Building tree bottom-up
    }

    override fun insertBottomUp(index: Int, instance: DomNodeWrapper) {
        current.insert(index, instance)
    }

    override fun remove(index: Int, count: Int) {
        current.remove(index, count)
    }

    override fun move(from: Int, to: Int, count: Int) {
        current.move(from, to, count)
    }

    override fun onClear() {
        // or current.node.clear()?; in all examples it calls 'clear' on the root
        root.node.clear()
    }
}

class DomNodeWrapper(val node: Node) {

    constructor(tag: String) : this(document.createElement(tag))

    private var currentModifier: DomModifier = DomModifier

    private fun HTMLElement.updateInlineStyles(
        old: InlineStylesModifier?,
        new: InlineStylesModifier?
    ) {
        if (old?.styles == new?.styles) {
            return
        }
        style.cssText = new?.styles ?: ""
    }

    fun updateModifier(modifier: DomModifier) {
        val htmlElement = node as? HTMLElement ?: return

        var currentStyles: InlineStylesModifier? = null
        var newStyles: InlineStylesModifier? = null

        currentModifier.foldOut(Unit) { mod, _ ->
            when (mod) {
                is EventModifier ->
                    htmlElement.removeEventListener(mod.eventName, mod.listener)
                is AttributesModifier -> {
                    with(mutableMapOf<String, String>()) {
                        mod.configure(this)
                        this.forEach { htmlElement.removeAttribute(it.key) }
                    }
                }
                is AttributeModifier -> {
                    htmlElement.removeAttribute(mod.attrName)
                }
                is InlineStylesModifier -> currentStyles = mod
            }
        }

        currentModifier = modifier
        currentModifier.foldOut(Unit) { mod, _ ->
            when (mod) {
                is CssModifier -> htmlElement.style.apply(mod.configure)
                is EventModifier -> htmlElement.addEventListener(mod.eventName, mod.listener)
                is AttributesModifier -> htmlElement.apply {
                    with(mutableMapOf<String, String>()) {
                        mod.configure(this)
                        this.forEach { htmlElement.setAttribute(it.key, it.value) }
                    }
                }
                is InlineStylesModifier -> newStyles = mod
                is ClassModifier -> htmlElement.setAttribute("class", mod.classes)
                is AttributeModifier -> htmlElement.setAttribute(mod.attrName, mod.attrValue)
            }
        }

        htmlElement.updateInlineStyles(currentStyles, newStyles)
    }

    fun insert(index: Int, nodeWrapper: DomNodeWrapper) {
        val length = node.childNodes.length
        if (index < length) {
            node.insertBefore(nodeWrapper.node, node.childNodes[index]!!)
        } else {
            node.appendChild(nodeWrapper.node)
        }
    }

    fun remove(index: Int, count: Int) {
        repeat(count) {
            node.removeChild(node.childNodes[index]!!)
        }
    }

    fun move(from: Int, to: Int, count: Int) {
        if (from == to) {
            return // nothing to do
        }

        for (i in 0 until count) {
            // if "from" is after "to," the from index moves because we're inserting before it
            val fromIndex = if (from > to) from + i else from
            val toIndex = if (from > to) to + i else to + count - 2

            val child = node.removeChild(node.childNodes[fromIndex]!!)
            node.insertBefore(child, node.childNodes[toIndex]!!)
        }
    }
}

@Composable
fun Text(value: String) {
    ComposeNode<DomNodeWrapper, DomApplier>(
        factory = { DomNodeWrapper(document.createTextNode("")) },
        update = {
            set(value) { value -> (node as Text).data = value }
        },
    )
}

@Composable
fun Element(tagName: String, modifier: DomModifier = DomModifier, content: @Composable () -> Unit) {
    ComposeNode<DomNodeWrapper, DomApplier>(
        factory = { DomNodeWrapper(document.createElement(tagName)) },
        update = {
            set(modifier) { value -> updateModifier(value) }
        },
        content = content
    )
}

@Composable
fun div(modifier: DomModifier = DomModifier, content: @Composable () -> Unit) {
    Element(tagName = "div", modifier = modifier, content = content)
}

/*
  classes vs inline

  inline styles -> string (or as objects?)
  styles as Composables?


 */