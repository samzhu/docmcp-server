package io.github.samzhu.docmcp;

import org.springframework.boot.SpringApplication;

public class TestDocMcpServerApplication {

	public static void main(String[] args) {
		SpringApplication.from(DocMcpServerApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
