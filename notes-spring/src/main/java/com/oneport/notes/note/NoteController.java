package com.oneport.notes.note;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/notes")
class NoteController {

	private final NoteService service;

	NoteController(NoteService service) {
		this.service = service;
	}

	@GetMapping
	List<NoteResponse> list(@RequestParam(defaultValue = "50") @Min(1) @Max(200) int limit) {
		return service.list(limit).stream().map(NoteResponse::from).toList();
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	NoteResponse create(@Valid @RequestBody NoteCreateRequest request) {
		return NoteResponse.from(service.create(request));
	}

	@PatchMapping("/{noteId}")
	NoteResponse update(@PathVariable long noteId, @Valid @RequestBody NoteUpdateRequest request) {
		return NoteResponse.from(service.update(noteId, request));
	}

	@DeleteMapping("/{noteId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	void delete(@PathVariable long noteId) {
		service.delete(noteId);
	}

}
