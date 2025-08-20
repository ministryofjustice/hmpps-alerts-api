import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
  id("uk.gov.justice.hmpps.gradle-spring-boot") version "8.3.6"
  kotlin("plugin.spring") version "2.2.10"
  kotlin("plugin.jpa") version "2.2.10"
  jacoco
}

configurations {
  testImplementation { exclude(group = "org.junit.vintage") }
}

dependencies {
  // Spring boot dependencies
  implementation("org.springframework.boot:spring-boot-starter-webflux")
  implementation("org.springframework.boot:spring-boot-starter-data-jpa")
  implementation("uk.gov.justice.service.hmpps:hmpps-kotlin-spring-boot-starter:1.5.0")
  implementation("io.sentry:sentry-spring-boot-starter-jakarta:8.19.1")
  implementation("com.fasterxml.uuid:java-uuid-generator:5.1.0")

  // Database dependencies
  runtimeOnly("org.flywaydb:flyway-core")
  runtimeOnly("org.flywaydb:flyway-database-postgresql")
  runtimeOnly("org.postgresql:postgresql")
  implementation("io.hypersistence:hypersistence-utils-hibernate-63:3.10.3")

  // AWS
  implementation("uk.gov.justice.service.hmpps:hmpps-sqs-spring-boot-starter:5.4.10")

  // OpenAPI
  implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.8.10")

  // Test dependencies
  testImplementation("org.junit.jupiter:junit-jupiter:5.13.4")
  testRuntimeOnly("org.junit.platform:junit-platform-launcher")
  testImplementation("org.wiremock:wiremock-standalone:3.13.1")
  testImplementation("org.testcontainers:postgresql:1.21.3")
  testImplementation("org.testcontainers:localstack:1.21.3")
  testImplementation("org.awaitility:awaitility-kotlin:4.3.0")
  testImplementation("io.jsonwebtoken:jjwt-impl:0.12.7")
  testImplementation("io.jsonwebtoken:jjwt-jackson:0.12.7")
}

kotlin {
  jvmToolchain(21)
}

tasks {
  withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    compilerOptions {
      jvmTarget = JvmTarget.JVM_21
      freeCompilerArgs.add("-Xwhen-guards")
    }
  }

  register("initialiseDatabase", Test::class) {
    include("**/InitialiseDatabase.class")
  }

  test {
    exclude("**/InitialiseDatabase.class")
  }

  getByName("initialiseDatabase") {
    onlyIf { gradle.startParameter.taskNames.contains("initialiseDatabase") }
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
