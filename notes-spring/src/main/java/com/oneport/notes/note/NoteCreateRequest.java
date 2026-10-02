package com.oneport.notes.note;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record NoteCreateRequest(@NotBlank @Size(max = 200) String title) {

	public NoteCreateRequest {
		title = (title != null) ? title.strip() : null;
	}

}
