package com.oneport.notes.note;

import java.util.List;

import com.oneport.notes.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import jakarta.persistence.EntityManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Limit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfiguration.class)
class NoteRepositoryTests {

	@Autowired
	private NoteRepository repository;

	@Autowired
	private EntityManager entityManager;

	@Test
	void listsNewestFirstAndRespectsLimit() {
		repository.saveAllAndFlush(List.of(new Note("a"), new Note("b"), new Note("c")));

		assertThat(repository.findAllByOrderByCreatedAtDescIdDesc(Limit.of(2))).extracting(Note::getTitle)
			.containsExactly("c", "b");
	}

	@Test
	void createdAtIsAssignedByDatabase() {
		Note saved = repository.saveAndFlush(new Note("timestamped"));

		assertThat(saved.getId()).isNotNull();
		assertThat(saved.getCreatedAt()).isNotNull();
	}

	@Test
	void newNoteIsNotDone() {
		assertThat(repository.saveAndFlush(new Note("fresh")).isDone()).isFalse();
	}

	@Test
	void rowWrittenByPreviousReleaseReadsAsNotDone() {
		entityManager.createNativeQuery("INSERT INTO notes (title) VALUES ('from 1.0.0')").executeUpdate();
		entityManager.clear();

		assertThat(repository.findAllByOrderByCreatedAtDescIdDesc(Limit.of(1))).singleElement()
			.satisfies((note) -> assertThat(note.isDone()).isFalse());
	}

	@Test
	void deleteReturnsZeroForUnknownId() {
		Note saved = repository.saveAndFlush(new Note("x"));

		assertThat(repository.deleteByIdReturningCount(saved.getId())).isEqualTo(1);
		assertThat(repository.deleteByIdReturningCount(saved.getId())).isZero();
	}

	@Test
	void blankTitleIsRejectedByDatabaseConstraint() {
		assertThatThrownBy(() -> repository.saveAndFlush(new Note("   ")))
			.isInstanceOf(DataIntegrityViolationException.class);
	}

}
