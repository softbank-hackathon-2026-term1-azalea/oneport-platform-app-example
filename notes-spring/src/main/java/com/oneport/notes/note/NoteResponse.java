package com.oneport.notes.note;

import java.time.OffsetDateTime;

public record NoteResponse(long id, String title, OffsetDateTime createdAt, boolean done) {

	static NoteResponse from(Note note) {
		return new NoteResponse(note.getId(), note.getTitle(), note.getCreatedAt(), note.isDone());
	}

}
