package com.oneport.notes.note;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class NoteService {

	private static final Logger log = LoggerFactory.getLogger(NoteService.class);

	private final NoteRepository repository;

	NoteService(NoteRepository repository) {
		this.repository = repository;
	}

	public List<Note> list(int limit) {
		return repository.findAllByOrderByCreatedAtDescIdDesc(Limit.of(limit));
	}

	@Transactional
	public Note create(NoteCreateRequest request) {
		Note note = repository.save(new Note(request.title()));
		log.atInfo().addKeyValue("note_id", note.getId()).log("note created");
		return note;
	}

	@Transactional
	public void delete(long id) {
		if (repository.deleteByIdReturningCount(id) == 0) {
			throw new NoteNotFoundException(id);
		}
		log.atInfo().addKeyValue("note_id", id).log("note deleted");
	}

}
