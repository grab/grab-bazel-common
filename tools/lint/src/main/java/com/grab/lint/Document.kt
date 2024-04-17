package com.grab.lint

import org.w3c.dom.Element
import org.w3c.dom.NodeList

fun NodeList.elements() = (0 until length).map { item(it) as Element }

operator fun Element.get(name: String): String = getAttribute(name)
operator fun Element.set(name: String, value: String) = setAttribute(name, value)