package com.oneport.notes.visit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = VisitController.class)
class VisitExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(VisitExceptionHandler.class);

	@ExceptionHandler(CacheNotConfiguredException.class)
	ProblemDetail handleNotConfigured(CacheNotConfiguredException ex) {
		return ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE, ex.getMessage());
	}

	@ExceptionHandler(DataAccessException.class)
	ProblemDetail handleUnavailable(DataAccessException ex) {
		log.warn("cache request failed: {}", ex.getClass().getSimpleName());
		return ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE, "cache unavailable");
	}

}
