package com.torusage.adapter.exception

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus
import java.lang.RuntimeException

@ResponseStatus(code = HttpStatus.NOT_FOUND, reason = "The requested node was not found.")
class NodeNotFoundException: RuntimeException()
