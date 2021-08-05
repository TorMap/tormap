package org.tormap.adapter.controller.exception

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus

@ResponseStatus(code = HttpStatus.NOT_FOUND, reason = "The requested node was not found.")
class NodeNotFoundException: RuntimeException()
