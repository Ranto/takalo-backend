package ara.project.takalo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing(auditorAwareRef = "auditorAware")
public class TakaloApplication {

	public static void main(String[] args) {
		SpringApplication.run(TakaloApplication.class, args);
	}

}
