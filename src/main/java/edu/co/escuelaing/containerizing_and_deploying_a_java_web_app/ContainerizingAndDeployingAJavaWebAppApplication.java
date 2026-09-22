package edu.co.escuelaing.containerizing_and_deploying_a_java_web_app;

import java.util.Map;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ContainerizingAndDeployingAJavaWebAppApplication {

	public static void main(String[] args) {
        SpringApplication application =
                new SpringApplication(ContainerizingAndDeployingAJavaWebAppApplication.class);

        application.setDefaultProperties(
                Map.of("server.port",
                        System.getenv().getOrDefault("PORT", "8000")));

        application.run(args);
    }

}
