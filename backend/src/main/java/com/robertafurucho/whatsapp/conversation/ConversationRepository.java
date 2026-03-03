package com.robertafurucho.whatsapp.conversation;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * JPA repository for WhatsApp conversation states.
 */
public interface ConversationRepository extends JpaRepository<ConversationState, Long> {

    /**
     * Finds the active (non-completed, non-expired) conversation for a WhatsApp ID.
     * Uses native query with LIMIT 1 to prevent IncorrectResultSizeDataAccessException
     * when duplicate active rows exist (e.g. from a race condition).
     */
    @Query(value = "SELECT * FROM conversation_states WHERE wa_id = :waId " +
           "AND current_step NOT IN ('COMPLETED', 'EXPIRED') " +
           "AND expired = false ORDER BY created_at DESC LIMIT 1",
           nativeQuery = true)
    Optional<ConversationState> findActiveByWaId(@Param("waId") String waId);

    /**
     * Finds conversations that have timed out (no activity for given duration).
     */
    @Query("SELECT c FROM ConversationState c WHERE c.updatedAt < :cutoff " +
           "AND c.currentStep NOT IN ('COMPLETED', 'EXPIRED') " +
           "AND c.expired = false")
    List<ConversationState> findExpiredConversations(@Param("cutoff") LocalDateTime cutoff);

    /**
     * Finds the most recent completed conversation for a WhatsApp ID.
     * Uses native query with LIMIT 1 to prevent IncorrectResultSizeDataAccessException
     * when a returning customer has multiple completed conversations.
     */
    @Query(value = "SELECT * FROM conversation_states WHERE wa_id = :waId " +
           "AND current_step = 'COMPLETED' ORDER BY completed_at DESC LIMIT 1",
           nativeQuery = true)
    Optional<ConversationState> findLastCompletedByWaId(@Param("waId") String waId);
}
