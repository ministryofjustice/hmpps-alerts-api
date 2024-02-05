package uk.gov.justice.digital.hmpps.hmppsalertsapi.model

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate
import java.time.LocalDateTime

/*create table OFFENDER_ALERTS
(
    ALERT_DATE                    DATE              default SYSDATE      not null,
    OFFENDER_BOOK_ID              NUMBER(10)                             not null
        constraint OFF_ALERT_OFF_BKG_F1
            references OFFENDER_BOOKINGS,
    ROOT_OFFENDER_ID              NUMBER(10),
    ALERT_SEQ                     NUMBER(6)                              not null,
    ALERT_TYPE                    VARCHAR2(12 char)                      not null,
    ALERT_CODE                    VARCHAR2(12 char)                      not null,
    AUTHORIZE_PERSON_TEXT         VARCHAR2(40 char),
    CREATE_DATE                   DATE,
    ALERT_STATUS                  VARCHAR2(12 char)                      not null,
    VERIFIED_FLAG                 VARCHAR2(1 char)  default 'N'          not null,
    EXPIRY_DATE                   DATE,
    COMMENT_TEXT                  VARCHAR2(4000 char),
    CASELOAD_ID                   VARCHAR2(6 char),
    MODIFY_USER_ID                VARCHAR2(32 char),
    MODIFY_DATETIME               TIMESTAMP(9),
    CASELOAD_TYPE                 VARCHAR2(12 char),
    CREATE_DATETIME               TIMESTAMP(9)      default systimestamp not null,
    CREATE_USER_ID                VARCHAR2(32 char) default USER         not null,
    AUDIT_TIMESTAMP               TIMESTAMP(9),
    AUDIT_USER_ID                 VARCHAR2(32 char),
    AUDIT_MODULE_NAME             VARCHAR2(65 char),
    AUDIT_CLIENT_USER_ID          VARCHAR2(64 char),
    AUDIT_CLIENT_IP_ADDRESS       VARCHAR2(39 char),
    AUDIT_CLIENT_WORKSTATION_NAME VARCHAR2(64 char),
    AUDIT_ADDITIONAL_INFO         VARCHAR2(256 char),
    constraint ALERTS_PK
        primary key (OFFENDER_BOOK_ID, ALERT_SEQ)
)*/

data class NomisAlert(
  @Schema(
    description = "The date the alert came into effect. Maps to the ALERT_DATE column in the NOMIS database.",
    example = "2021-09-27",
  )
  val alertDate: LocalDate = LocalDate.now(),

  val offenderBookId: Long,

  val rootOffenderId: Long?,

  val alertSeq: Int,

  val alertType: String,

  val alertCode: String,

  val authorizePersonText: String?,

  val createDate: LocalDate?,

  val alertStatus: String,

  val verifiedFlag: Boolean = false,

  val expiryDate: LocalDate?,

  val commentText: String?,

  val caseloadId: String?,

  val modifyUserId: String?,

  val modifyDatetime: LocalDateTime?,

  val caseloadType: String?,

  val createDatetime: LocalDateTime = LocalDateTime.now(),

  val createUserId: String?,

  val auditTimestamp: LocalDateTime?,

  val auditUserId: String?,

  val auditModuleName: String?,

  val auditClientUserId: String?,

  val auditClientIpAddress: String?,

  val auditClientWorkstationName: String?,

  val auditAdditionalInfo: String?,
)
