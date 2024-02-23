package uk.gov.justice.digital.hmpps.hmppsalertsapi.config

import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.Operation
import io.swagger.v3.oas.models.info.Contact
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.security.SecurityRequirement
import io.swagger.v3.oas.models.security.SecurityScheme
import org.springdoc.core.customizers.OperationCustomizer
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.info.BuildProperties
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.expression.BeanFactoryResolver
import org.springframework.expression.spel.SpelEvaluationException
import org.springframework.expression.spel.standard.SpelExpressionParser
import org.springframework.expression.spel.support.StandardEvaluationContext
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.method.HandlerMethod

@Configuration
class OpenApiConfiguration(buildProperties: BuildProperties) {
  private val version: String = buildProperties.version

  @Autowired
  private lateinit var context: ApplicationContext

  @Bean
  fun customOpenAPI(): OpenAPI? = OpenAPI()
    .info(
      Info()
        .title("Alerts API")
        .version(version)
        .description(
          "API for retrieving and managing alerts relating to a person.\n\n" +
            "## Authentication\n\n" +
            "This API uses OAuth2 with JWTs. " +
            "You will need to pass the JWT in the `Authorization` header using the `Bearer` scheme.\n\n" +
            "## Authorisation\n\n" +
            "The API uses roles to control access to the endpoints. " +
            "The roles required for each endpoint are documented in the endpoint descriptions.\n\n" +
            "## Identifying the user\n\n" +
            "The majority of the endpoints in this API require the user to be identified via their username. " +
            "This is to correctly populate the change history of alerts e.g. who created or updated an alert and for auditing purposes. " +
            "The username is required when the service is called directly by a user or when another service is acting on behalf of a user. " +
            "The following methods for supplying the username are supported to cater for these scenarios:\n\n" +
            "1. **Token claim** - The username can be passed in via a `user_name` or `username` claim in the JWT.\n" +
            "2. **Username header** - The username can be passed in via a `Username` header in the request.\n\n" +
            "The username is taken in priority order from the `user_name` or `username` claim in the JWT if present, followed by the `Username` header if no claim is found.\n\n" +
            "### 4XX response codes related to username:\n\n" +
            "- A 400 Bad Request response will be returned if the username cannot be found via any of the above methods.\n" +
            "- A 403 Forbidden response will be returned if the username cannot be found in the user management service.\n" +
            "- A 403 Forbidden response will also be returned if the user identified by the username does not have " +
            "access to the caseload associated with the person.",
        )
        .contact(
          Contact()
            .name("HMPPS Digital Studio")
            .email("feedback@digital.justice.gov.uk"),
        ),
    )
    .components(
      Components().addSecuritySchemes(
        "bearer-jwt",
        SecurityScheme()
          .type(SecurityScheme.Type.HTTP)
          .scheme("bearer")
          .bearerFormat("JWT")
          .`in`(SecurityScheme.In.HEADER)
          .name("Authorization"),
      ),
    )
    .addSecurityItem(SecurityRequirement().addList("bearer-jwt", listOf("read", "write")))

  @Bean
  fun preAuthorizeCustomizer(): OperationCustomizer {
    return OperationCustomizer { operation: Operation, handlerMethod: HandlerMethod ->
      handlerMethod.preAuthorizeForMethodOrClass()?.let {
        val preAuthExp = SpelExpressionParser().parseExpression(it)
        val evalContext = StandardEvaluationContext()
        evalContext.beanResolver = BeanFactoryResolver(context)
        evalContext.setRootObject(
          object {
            fun hasRole(role: String) = listOf(role)
            fun hasAnyRole(vararg roles: String) = roles.toList()
          },
        )

        val roles = try {
          (preAuthExp.getValue(evalContext) as List<*>).filterIsInstance<String>()
        } catch (e: SpelEvaluationException) {
          emptyList()
        }
        if (roles.isNotEmpty()) {
          operation.description = "${operation.description ?: ""}\n\n" +
            "Requires one of the following roles:\n" +
            roles.joinToString(prefix = "* ", separator = "\n* ")
        }
      }

      operation
    }
  }

  private fun HandlerMethod.preAuthorizeForMethodOrClass() =
    getMethodAnnotation(PreAuthorize::class.java)?.value
      ?: beanType.getAnnotation(PreAuthorize::class.java)?.value
}
