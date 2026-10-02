package com.oneport.notes.note;

import java.time.OffsetDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;

@WebMvcTest(NoteController.class)
class NoteControllerTests {

	@Autowired
	private MockMvcTester mvc;

	@MockitoBean
	private NoteService service;

	@Test
	void listReturnsSnakeCaseFields() {
		Note note = note(1L, "hello");
		given(service.list(anyInt())).willReturn(List.of(note));

		assertThat(mvc.get().uri("/notes")).hasStatusOk()
			.bodyJson()
			.extractingPath("$[0]")
			.asMap()
			.containsKeys("id", "title", "created_at", "done")
			.containsEntry("title", "hello");
	}

	@Test
	void createTrimsTitleAndReturns201() {
		Note note = note(2L, "hello");
		given(service.create(any())).willReturn(note);

		assertThat(mvc.post().uri("/notes").contentType(MediaType.APPLICATION_JSON).content("{\"title\":\"  hello \"}"))
			.hasStatus(HttpStatus.CREATED)
			.bodyJson()
			.extractingPath("$.title")
			.isEqualTo("hello");
	}

	@Test
	void patchUpdatesDone() {
		Note note = note(3L, "hello");
		given(note.isDone()).willReturn(true);
		given(service.update(eq(3L), any())).willReturn(note);

		assertThat(mvc.patch().uri("/notes/3").contentType(MediaType.APPLICATION_JSON).content("{\"done\":true}"))
			.hasStatusOk()
			.bodyJson()
			.extractingPath("$.done")
			.isEqualTo(true);
	}

	@Test
	void blankTitleIsBadRequest() {
		assertThat(mvc.post().uri("/notes").contentType(MediaType.APPLICATION_JSON).content("{\"title\":\"   \"}"))
			.hasStatus(HttpStatus.BAD_REQUEST)
			.hasContentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON);
	}

	@Test
	void missingNoteIsProblemDetail() {
		willThrow(new NoteNotFoundException(42)).given(service).delete(42);

		assertThat(mvc.delete().uri("/notes/42")).hasStatus(HttpStatus.NOT_FOUND)
			.bodyJson()
			.extractingPath("$.note_id")
			.isEqualTo(42);
	}

	private static Note note(long id, String title) {
		Note note = mock(Note.class);
		given(note.getId()).willReturn(id);
		given(note.getTitle()).willReturn(title);
		given(note.getCreatedAt()).willReturn(OffsetDateTime.now());
		given(note.isDone()).willReturn(false);
		return note;
	}

}
