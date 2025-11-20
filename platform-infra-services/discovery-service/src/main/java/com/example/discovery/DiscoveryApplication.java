package com.example.discovery;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;
import org.springframework.core.env.Environment;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * Discovery Service Application
 * Netflix Eureka Server for service registry and discovery
 */
@Slf4j
@EnableEurekaServer
@SpringBootApplication
public class DiscoveryApplication {

	public static void main(String[] args) {
		SpringApplication app = new SpringApplication(DiscoveryApplication.class);
		Environment env = app.run(args).getEnvironment();
		
		logApplicationStartup(env);
	}

	private static void logApplicationStartup(Environment env) {
		String protocol = "http";
		String serverPort = env.getProperty("server.port", "8761");
		String contextPath = env.getProperty("server.servlet.context-path", "/");
		String hostAddress = "localhost";
		
		try {
			hostAddress = InetAddress.getLocalHost().getHostAddress();
		} catch (UnknownHostException e) {
			log.warn("Unable to determine host address", e);
		}
		
		log.info("""
			
			----------------------------------------------------------
			Application '{}' is running!
			Access URLs:
			\tLocal:      {}://localhost:{}{}
			\tExternal:   {}://{}:{}{}
			\tDashboard:  {}://localhost:{}/
			\tActuator:   {}://localhost:{}/actuator
			Profile(s): {}
			----------------------------------------------------------
			""",
			env.getProperty("spring.application.name"),
			protocol, serverPort, contextPath,
			protocol, hostAddress, serverPort, contextPath,
			protocol, serverPort,
			protocol, serverPort,
			env.getActiveProfiles().length == 0 ? env.getDefaultProfiles() : env.getActiveProfiles()
		);
	}
}
