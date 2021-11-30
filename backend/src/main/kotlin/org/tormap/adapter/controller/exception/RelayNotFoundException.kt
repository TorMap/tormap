package org.tormap.adapter.controller.exception

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus

@ResponseStatus(code = HttpStatus.NOT_FOUND, reason = "The requested relay was not found.")
class RelayNotFoundException: RuntimeException()
