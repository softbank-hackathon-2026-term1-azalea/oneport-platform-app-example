package com.oneport.notes;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.client.RestTestClient;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
		properties = { "LAUNCHPAD_RELEASE_ID=rel-123", "LOG_FORMAT=text" })
@Import(TestcontainersConfiguration.class)
@AutoConfigureRestTestClient
class ReleaseHeaderTests {

	@Autowired
	private RestTestClient client;

	@Test
	void everyResponseCarriesTheReleaseId() {
		for (String path : new String[] { "/health", "/version", "/notes" }) {
			client.get().uri(path).exchange().expectHeader().valueEquals("X-Launchpad-Release", "rel-123");
		}
	}

}
