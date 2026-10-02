package com.oneport.notes.visit;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/visits")
class VisitController {

	private final VisitCounter counter;

	VisitController(VisitCounter counter) {
		this.counter = counter;
	}

	@GetMapping
	VisitCount count() {
		return new VisitCount(counter.count());
	}

	@PostMapping
	VisitCount record() {
		return new VisitCount(counter.record());
	}

}
