package uk.gov.justice.digital.hmpps.hmppsalertsapi.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.hmppsalertsapi.entity.Comment

@Repository
interface CommentRepository : JpaRepository<Comment, Long> {
  fun findCommentsByAlertAlertIdInOrderByCreatedAtDesc(alertIds: Collection<Long>): Collection<Comment>
}
