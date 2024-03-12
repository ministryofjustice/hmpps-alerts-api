plugins {
  id("uk.gov.justice.hmpps.gradle-spring-boot") version "5.15.3"
  kotlin("plugin.spring") version "1.9.23"
  kotlin("plugin.jpa") version "1.9.23"
  jacoco
}

configurations {
  testImplementation { exclude(group = "org.junit.vintage") }
}

dependencies {
  // Spring boot dependencies
  implementation("org.springframework.boot:spring-boot-starter-webflux")
  implementation("org.springframework.boot:spring-boot-starter-data-jpa")
  implementation("uk.gov.justice.service.hmpps:hmpps-kotlin-spring-boot-starter:0.2.2")

  // Database dependencies
  runtimeOnly("org.flywaydb:flyway-core")
  runtimeOnly("org.postgresql:postgresql")
  implementation("io.hypersistence:hypersistence-utils-hibernate-62:3.7.3")

  // AWS
  implementation("uk.gov.justice.service.hmpps:hmpps-sqs-spring-boot-starter:3.1.1")

  // OpenAPI
  implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.3.0")

  // Test dependencies
  testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
  testRuntimeOnly("org.junit.platform:junit-platform-launcher")
  testImplementation("org.wiremock:wiremock-standalone:3.4.2")
  testImplementation("org.testcontainers:postgresql:1.19.7")
  testImplementation("org.testcontainers:localstack:1.19.7")
  testImplementation("org.awaitility:awaitility-kotlin:4.2.0")
  testImplementation("io.jsonwebtoken:jjwt-impl:0.12.5")
  testImplementation("io.jsonwebtoken:jjwt-jackson:0.12.5")
}

kotlin {
  jvmToolchain(21)
}

tasks {
  withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
      jvmTarget = "21"
    }
  }
}

// Jacoco code coverage
tasks.named("test") {
  finalizedBy("jacocoTestReport")
}

tasks.named<JacocoReport>("jacocoTestReport") {
  reports {
    html.required.set(true)
    xml.required.set(true)
  }
}
