package com.oneport.notes.note;

import jakarta.validation.constraints.NotNull;

public record NoteUpdateRequest(@NotNull Boolean done) {

}
