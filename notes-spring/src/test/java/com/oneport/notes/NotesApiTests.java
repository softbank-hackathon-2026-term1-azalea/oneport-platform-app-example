package com.oneport.notes;

import java.util.List;
import java.util.Map;

import com.oneport.notes.note.NoteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.client.RestTestClient;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
		properties = { "APP_COLOR=#22c55e", "LOG_FORMAT=text" })
@Import(TestcontainersConfiguration.class)
@AutoConfigureRestTestClient
class NotesApiTests {

	private static final ParameterizedTypeReference<Map<String, Object>> MAP = new ParameterizedTypeReference<>() {
	};

	private static final ParameterizedTypeReference<List<Map<String, Object>>> LIST = new ParameterizedTypeReference<>() {
	};

	@Autowired
	private RestTestClient client;

	@Autowired
	private NoteRepository repository;

	@BeforeEach
	void cleanTable() {
		repository.deleteAll();
	}

	@Test
	void healthIsOkWithoutDatabase() {
		client.get().uri("/health").exchange().expectStatus().isOk().expectBody().jsonPath("$.status").isEqualTo("ok");
	}

	@Test
	void readyChecksDatabase() {
		client.get().uri("/ready").exchange().expectStatus().isOk().expectBody().jsonPath("$.database").isEqualTo("up");
	}

	@Test
	void versionExposesReleaseInfo() {
		Map<String, Object> body = client.get()
			.uri("/version")
			.exchange()
			.expectStatus()
			.isOk()
			.expectBody(MAP)
			.returnResult()
			.getResponseBody();
		assertThat(body).containsEntry("app", "notes").containsEntry("version", "1.1.0");
		assertThat(body).containsEntry("color", "#22c55e");
		assertThat(body).containsOnlyKeys("app", "version", "git_sha", "built_at", "color", "hostname", "started_at");
	}

	@Test
	void failureAlways503() {
		client.get().uri("/failure").exchange().expectStatus().isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
	}

	@Test
	void requestIdHeaderIsEchoedWhenValid() {
		String requestId = "2b1c0b8e-7d3a-4f1e-9c0a-5f6d7e8a9b0c";
		client.get()
			.uri("/version")
			.header("X-Request-ID", requestId)
			.exchange()
			.expectHeader()
			.valueEquals("X-Request-ID", requestId);

		String issued = client.get()
			.uri("/version")
			.header("X-Request-ID", "not-a-uuid")
			.exchange()
			.returnResult(String.class)
			.getResponseHeaders()
			.getFirst("X-Request-ID");
		assertThat(issued).isNotEqualTo("not-a-uuid").hasSize(36);
	}

	@Test
	void indexServesPage() {
		String page = client.get().uri("/").exchange().expectStatus().isOk().expectBody(String.class).returnResult().getResponseBody();
		assertThat(page).contains("Oneport Notes");
	}

	@Test
	void createAndListNotes() {
		create("first").expectStatus().isCreated();
		create("  second  ").expectStatus().isCreated().expectBody().jsonPath("$.title").isEqualTo("second")
			.jsonPath("$.id").exists().jsonPath("$.created_at").exists();

		List<Map<String, Object>> notes = client.get()
			.uri("/notes")
			.exchange()
			.expectStatus()
			.isOk()
			.expectBody(LIST)
			.returnResult()
			.getResponseBody();
		assertThat(notes).extracting((note) -> note.get("title")).containsExactly("second", "first");
	}

	@Test
	void blankOrLongTitleIsRejectedAsProblemDetail() {
		create("   ").expectStatus().isBadRequest().expectHeader().contentType(MediaType.APPLICATION_PROBLEM_JSON);
		create("x".repeat(201)).expectStatus().isBadRequest();
		create("x".repeat(200)).expectStatus().isCreated();
	}

	@Test
	void listLimitIsValidated() {
		client.get().uri("/notes?limit=0").exchange().expectStatus().isBadRequest();
		client.get().uri("/notes?limit=201").exchange().expectStatus().isBadRequest();
	}

	@Test
	void deleteNote() {
		Object id = create("to delete").expectBody(MAP).returnResult().getResponseBody().get("id");
		client.delete().uri("/notes/" + id).exchange().expectStatus().isNoContent();
		assertThat(client.get().uri("/notes").exchange().expectBody(LIST).returnResult().getResponseBody()).isEmpty();
	}

	@Test
	void deleteMissingNoteReturnsProblemDetail() {
		client.delete()
			.uri("/notes/999999")
			.exchange()
			.expectStatus()
			.isNotFound()
			.expectHeader()
			.contentType(MediaType.APPLICATION_PROBLEM_JSON)
			.expectBody()
			.jsonPath("$.note_id")
			.isEqualTo(999999);
	}

	@Test
	void newNoteIsNotDone() {
		create("fresh").expectStatus().isCreated().expectBody().jsonPath("$.done").isEqualTo(false);
	}

	@Test
	void patchTogglesDone() {
		Object id = create("toggle").expectBody(MAP).returnResult().getResponseBody().get("id");

		patch(id, Map.of("done", true)).expectStatus().isOk().expectBody().jsonPath("$.done").isEqualTo(true);
		client.get().uri("/notes").exchange().expectBody().jsonPath("$[0].done").isEqualTo(true);
		patch(id, Map.of("done", false)).expectStatus().isOk().expectBody().jsonPath("$.done").isEqualTo(false);
	}

	@Test
	void patchMissingNoteReturnsProblemDetail() {
		patch(999999, Map.of("done", true)).expectStatus()
			.isNotFound()
			.expectBody()
			.jsonPath("$.note_id")
			.isEqualTo(999999);
	}

	@Test
	void patchRequiresDone() {
		Object id = create("strict").expectBody(MAP).returnResult().getResponseBody().get("id");
		patch(id, Map.of()).expectStatus().isBadRequest();
	}

	private RestTestClient.ResponseSpec patch(Object id, Map<String, Object> body) {
		return client.patch().uri("/notes/" + id).contentType(MediaType.APPLICATION_JSON).body(body).exchange();
	}

	private RestTestClient.ResponseSpec create(String title) {
		return client.post().uri("/notes").contentType(MediaType.APPLICATION_JSON).body(Map.of("title", title)).exchange();
	}

}
