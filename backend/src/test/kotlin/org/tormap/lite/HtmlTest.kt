package org.tormap.lite

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class HtmlTest : StringSpec({
    "text and attribute values are escaped" {
        val html = Html.document {
            tag("p", "title" to "\"><script>alert(1)</script>") { text("<b>Tom & 'Jerry'</b>") }
        }
        html shouldBe "<!DOCTYPE html><p title=\"&quot;&gt;&lt;script&gt;alert(1)&lt;/script&gt;\">&lt;b&gt;Tom &amp; &#39;Jerry&#39;&lt;/b&gt;</p>"
    }

    "non-ASCII text is kept as it is" {
        escapeHtml("Relay äöü ☃") shouldBe "Relay äöü ☃"
    }

    "null and false attributes are left out, true is written bare" {
        Html.document { tag("input", "type" to "checkbox", "checked" to true, "disabled" to false, "value" to null) } shouldBe
            "<!DOCTYPE html><input type=\"checkbox\" checked>"
    }

    "invalid element and attribute names are rejected" {
        shouldThrow<IllegalArgumentException> { Html.document { tag("p onclick=x") } }
        shouldThrow<IllegalArgumentException> { Html.document { tag("p", "onclick=\"x\" a" to "b") } }
    }

    "void elements can not have a body" {
        shouldThrow<IllegalArgumentException> { Html.document { tag("input") { text("x") } } }
    }
})
