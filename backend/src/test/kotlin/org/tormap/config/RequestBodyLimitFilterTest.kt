package org.tormap.config

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import jakarta.servlet.FilterChain
import jakarta.servlet.ReadListener
import jakarta.servlet.ServletRequest
import jakarta.servlet.http.HttpServletRequest
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.web.server.ContentTooLargeException
import java.io.UnsupportedEncodingException

class RequestBodyLimitFilterTest : StringSpec({
    val filter = RequestBodyLimitFilter(maxBodyBytes = 10)

    fun request(body: String, encoding: String? = null) = MockHttpServletRequest("POST", "/relay/details/relay/identifiers").apply {
        setContent(body.toByteArray())
        if (encoding != null) characterEncoding = encoding
    }

    fun filterWith(request: HttpServletRequest, read: (ServletRequest) -> Unit): MockHttpServletResponse {
        val response = MockHttpServletResponse()
        filter.doFilter(request, response, FilterChain { wrapped, _ -> read(wrapped) })
        return response
    }

    "a declared Content-Length above the limit is rejected before the body is read" {
        var called = false
        val response = filterWith(request("x".repeat(11))) { called = true }
        response.status shouldBe 413
        called shouldBe false
    }

    "a body within the limit can be read as stream and as text" {
        filterWith(request("0123456789")) { it.inputStream.readAllBytes().decodeToString() shouldBe "0123456789" }
        filterWith(request("äöü", "UTF-8")) { it.reader.readText() shouldBe "äöü" }
        // Without an encoding the servlet default ISO-8859-1 applies
        filterWith(request("abc")) { it.reader.readText() shouldBe "abc" }
    }

    "reading beyond the limit fails, byte by byte as well as in blocks" {
        // Without a known length, as with chunked transfer encoding
        fun unknownLength(body: String) = object : MockHttpServletRequest("POST", "/") {
            override fun getContentLengthLong() = -1L
        }.apply { setContent(body.toByteArray()) }

        filterWith(unknownLength("x".repeat(20))) { request ->
            shouldThrow<ContentTooLargeException> { request.inputStream.readAllBytes() }
        }
        filterWith(unknownLength("x".repeat(11))) { request ->
            val stream = request.inputStream
            repeat(10) { stream.read() shouldBe 'x'.code }
            shouldThrow<ContentTooLargeException> { stream.read() }
        }
        filterWith(unknownLength("")) { it.inputStream.read() shouldBe -1 }
    }

    "an unknown character encoding is reported like by the servlet API" {
        filterWith(request("abc", "no-such-charset")) {
            shouldThrow<UnsupportedEncodingException> { it.reader }
        }
    }

    "the stream state and read listener are those of the request" {
        filterWith(request("abc")) {
            val stream = it.inputStream
            stream.isFinished shouldBe false
            stream.isReady shouldBe true
            stream.readAllBytes()
            stream.isFinished shouldBe true
            // MockHttpServletRequest does not support async IO, which shows the call is delegated
            shouldThrow<UnsupportedOperationException> {
                stream.setReadListener(object : ReadListener {
                    override fun onDataAvailable() = Unit
                    override fun onAllDataRead() = Unit
                    override fun onError(t: Throwable) = Unit
                })
            }
        }
    }
})
