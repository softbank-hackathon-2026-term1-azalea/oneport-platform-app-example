package com.oneport.notes.note;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = NoteController.class)
class NoteExceptionHandler {

	@ExceptionHandler(NoteNotFoundException.class)
	ProblemDetail handleNoteNotFound(NoteNotFoundException ex) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
		problem.setProperty("note_id", ex.getNoteId());
		return problem;
	}

}
