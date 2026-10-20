package org.tormap.config

import jakarta.servlet.FilterChain
import jakarta.servlet.ReadListener
import jakarta.servlet.ServletInputStream
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletRequestWrapper
import jakarta.servlet.http.HttpServletResponse
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.web.servlet.FilterRegistrationBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.filter.OncePerRequestFilter
import org.springframework.web.server.ContentTooLargeException
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.UnsupportedEncodingException
import java.nio.charset.Charset

/**
 * Limits the size of request bodies.
 *
 * Tomcat only limits form and multipart bodies (`server.tomcat.max-http-form-post-size`, `spring.servlet.multipart.*`),
 * so JSON bodies are limited here, independent of any limit at the reverse proxy. Jackson additionally limits the
 * length and token count of the documents it reads (`spring.jackson.factory.constraints.read.*`).
 */
@Configuration
class RequestBodyLimitConfig(
    @param:Value("\${app.http.request.max-body-bytes:1048576}")
    private val maxBodyBytes: Long,
) {

    @Bean
    fun requestBodyLimitFilterRegistration(): FilterRegistrationBean<RequestBodyLimitFilter> =
        FilterRegistrationBean(RequestBodyLimitFilter(maxBodyBytes)).apply {
            addUrlPatterns("/*")
        }
}

/**
 * Rejects requests with a declared `Content-Length` above [maxBodyBytes] with 413, and stops reading bodies without one
 * (chunked transfer encoding) once they exceed it.
 */
class RequestBodyLimitFilter(private val maxBodyBytes: Long) : OncePerRequestFilter() {

    override fun doFilterInternal(request: HttpServletRequest, response: HttpServletResponse, filterChain: FilterChain) {
        if (request.contentLengthLong > maxBodyBytes) {
            response.sendError(HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE)
            return
        }
        filterChain.doFilter(LimitedBodyRequest(request, maxBodyBytes), response)
    }

    private class LimitedBodyRequest(request: HttpServletRequest, private val maxBodyBytes: Long) :
        HttpServletRequestWrapper(request) {
        private val inputStream by lazy { LimitedServletInputStream(super.getInputStream(), maxBodyBytes) }

        override fun getInputStream(): ServletInputStream = inputStream

        override fun getReader(): BufferedReader {
            val charset = try {
                characterEncoding?.let { Charset.forName(it) } ?: Charsets.ISO_8859_1
            } catch (_: IllegalArgumentException) {
                // The encoding comes from the request, the servlet API reports an unknown one like this
                throw UnsupportedEncodingException(characterEncoding)
            }
            return BufferedReader(InputStreamReader(inputStream, charset))
        }
    }

    private class LimitedServletInputStream(private val delegate: ServletInputStream, private val maxBytes: Long) :
        ServletInputStream() {
        private var bytesRead = 0L

        override fun read(): Int = delegate.read().also { if (it != -1) count(1) }

        override fun read(b: ByteArray, off: Int, len: Int): Int = delegate.read(b, off, len).also { if (it > 0) count(it) }

        override fun isFinished() = delegate.isFinished

        override fun isReady() = delegate.isReady

        override fun setReadListener(readListener: ReadListener) = delegate.setReadListener(readListener)

        // Resolved to 413 by Spring MVC (ResponseStatusException)
        private fun count(bytes: Int) {
            bytesRead += bytes
            if (bytesRead > maxBytes) throw ContentTooLargeException(null)
        }
    }
}
