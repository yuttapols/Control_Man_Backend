package com.controlm;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/** Requires a reachable PostgreSQL: run with {@code ./mvnw verify -Pdb}. */
@Tag("db")
@SpringBootTest
class ControlMBackendApplicationTests {

	@Test
	void contextLoads() {
	}

}
