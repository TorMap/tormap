package org.tormap.lite

import org.springframework.web.util.HtmlUtils
import java.nio.charset.StandardCharsets

/**
 * Escapes a value for HTML text and double-quoted attribute values with Spring's [HtmlUtils] (`&`, `<`, `>`, `"`, `'`).
 * Relay data (nicknames, contact info, platforms) is published by relay operators and must always pass through here.
 * It is not safe for other contexts such as URLs in `href`, scripts or styles, so untrusted data must never go there.
 */
fun escapeHtml(value: String): String = HtmlUtils.htmlEscape(value, StandardCharsets.UTF_8.name())

private val namePattern = Regex("^[a-z][a-z0-9-]*$")
private val voidElements = setOf("br", "img", "input", "link", "meta")

/**
 * A minimal HTML builder for the Lite pages. Text and attribute values are always escaped, element and attribute
 * names must be lowercase literals. There is no way to write raw markup, so untrusted data can not inject any.
 */
class Html private constructor() {
    private val out = StringBuilder()

    /** Writes escaped text. */
    fun text(value: String) {
        out.append(escapeHtml(value))
    }

    /**
     * Writes an element. Attributes with a null or false value are left out, true writes a bare attribute.
     * Void elements (e.g. input) must not have a body.
     */
    fun tag(name: String, vararg attributes: Pair<String, Any?>, body: (Html.() -> Unit)? = null) {
        require(namePattern.matches(name)) { "Invalid element name: $name" }
        out.append('<').append(name)
        for ((attribute, value) in attributes) {
            require(namePattern.matches(attribute)) { "Invalid attribute name: $attribute" }
            when (value) {
                null, false -> continue
                true -> out.append(' ').append(attribute)
                else -> out.append(' ').append(attribute).append("=\"").append(escapeHtml(value.toString())).append('"')
            }
        }
        out.append('>')
        if (name in voidElements) {
            require(body == null) { "<$name> can not have a body" }
            return
        }
        body?.invoke(this)
        out.append("</").append(name).append('>')
    }

    /** Writes an element that only contains text. */
    fun tag(name: String, text: String, vararg attributes: Pair<String, Any?>) = tag(name, *attributes) { text(text) }

    companion object {
        /** Builds a whole HTML document. */
        fun document(body: Html.() -> Unit): String {
            val html = Html()
            html.out.append("<!DOCTYPE html>")
            html.body()
            return html.out.toString()
        }
    }
}
