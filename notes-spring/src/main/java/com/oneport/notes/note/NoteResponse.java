package com.oneport.notes.note;

import java.time.OffsetDateTime;

public record NoteResponse(long id, String title, OffsetDateTime createdAt) {

	static NoteResponse from(Note note) {
		return new NoteResponse(note.getId(), note.getTitle(), note.getCreatedAt());
	}

}
