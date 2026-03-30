package uk.gov.justice.digital.hmpps.hmppsalertsapi.config

import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.Operation
import io.swagger.v3.oas.models.info.Contact
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.security.SecurityRequirement
import io.swagger.v3.oas.models.security.SecurityScheme
import io.swagger.v3.oas.models.tags.Tag
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
import uk.gov.justice.digital.hmpps.hmppsalertsapi.resource.ROLE_PRISONER_ALERTS__PRISONER_ALERTS_ADMINISTRATION_UI

const val RO_OPERATIONS = "RO Operations"
const val RW_OPERATIONS = "RW Operations"
const val ADMIN_ONLY = "Admin Only"
const val ADMIN_UI_ONLY = "Admin UI Only"
const val NOMIS_SYNC_ONLY = "Nomis Sync Only"

@Configuration
class OpenApiConfiguration(buildProperties: BuildProperties) {
  private val version: String = buildProperties.version ?: "unknown"

  @Autowired
  private lateinit var context: ApplicationContext

  @Bean
  fun customOpenAPI(): OpenAPI? = OpenAPI()
    .info(
      Info()
        .title("Prisoner Alerts API")
        .version(version)
        .description(
          """
            |API for retrieving and managing prisoner alerts relating to a person.
            |
            |## Authentication
            |
            |This API uses OAuth2 with JWTs. You will need to pass the JWT in the `Authorization` header using the `Bearer` scheme.
            |All endpoints are designed to work with client tokens and user tokens should not be used with this service.
            |
            |## Authorisation
            |
            |The API uses roles to control access to the endpoints. The roles required for each endpoint are documented in the endpoint descriptions.
            |Services integrating with the API should request one of the two following roles depending on their needs:
            | 1. ROLE_PRISONER_ALERTS__RO - Grants read only access to the API e.g. retrieving alerts for a prisoner
            | 2. ROLE_PRISONER_ALERTS__RW - Grants read/write access to the API e.g. creating and expiring alerts
            |
            |## Identifying the user
            |
            |The majority of the endpoints in this API require the user to be identified via their username. This is to correctly populate the change history of alerts e.g. who created or updated an alert and for auditing purposes. The username is required when the service is called directly by a user or when another service is acting on behalf of a user. The following methods for supplying the username are supported to cater for these scenarios:
            |
            |1. **Token claim** - The username can be passed in via a `subject` claim in the JWT
            |
            |### 4XX response codes related to username:
            |
            |- A 400 Bad Request response will be returned if the username cannot be found via the above method.
            |- A 400 Bad Request response will be returned if the username cannot be found in the user management service.
            |- A 403 Forbidden response will also be returned if the user identified by the username does not have access to the caseload associated with the person.
            |
          """,
        ).contact(
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
    .addTagsItem(
      Tag().name(RO_OPERATIONS).description("Endpoints for read operations - accepts both RO and RW roles"),
    )
    .addTagsItem(Tag().name(RW_OPERATIONS).description("Endpoints for write operations - must have RW role"))
    .addTagsItem(
      Tag().name("No Further Operations")
        .description("Endpoints below this point are for special and explicit usage and should not be used under any circumstances without prior consultation with the team maintaining this API."),
    )
    .addTagsItem(
      Tag().name(NOMIS_SYNC_ONLY).description("Endpoints for nomis sync only - not to be use by any other client"),
    )
    .addTagsItem(
      Tag().name(ADMIN_UI_ONLY)
        .description("Endpoints for alerts admin ui only - Not to be used by any service other than the alert admin ui"),
    )
    .addTagsItem(
      Tag().name(ADMIN_ONLY)
        .description("Endpoints for alerts admins only - Not to be used by any service - for manual use by admin team members only"),
    )

  @Bean
  fun preAuthorizeCustomizer(): OperationCustomizer = OperationCustomizer { operation: Operation, handlerMethod: HandlerMethod ->
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
        val filteredRoles = roles.filter { r -> r != ROLE_PRISONER_ALERTS__PRISONER_ALERTS_ADMINISTRATION_UI }
        operation.description = "${operation.description ?: ""}\n\n" +
          if (filteredRoles.isEmpty()) {
            ""
          } else {
            "Requires one of the following roles:\n" +
              filteredRoles.joinToString(prefix = "* ", separator = "\n* ")
          }
      }
    }

    operation
  }

  private fun HandlerMethod.preAuthorizeForMethodOrClass() = getMethodAnnotation(PreAuthorize::class.java)?.value
    ?: beanType.getAnnotation(PreAuthorize::class.java)?.value
}
