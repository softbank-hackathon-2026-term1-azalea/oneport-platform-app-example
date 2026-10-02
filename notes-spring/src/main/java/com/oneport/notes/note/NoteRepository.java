package com.oneport.notes.note;

import java.util.List;

import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NoteRepository extends JpaRepository<Note, Long> {

	List<Note> findAllByOrderByCreatedAtDescIdDesc(Limit limit);

	@Modifying(flushAutomatically = true, clearAutomatically = true)
	@Query("delete from Note n where n.id = :id")
	int deleteByIdReturningCount(@Param("id") long id);

}
