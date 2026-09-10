package live.toon.api.repository;

import live.toon.api.entity.MarriageProposal;
import live.toon.api.entity.MarriageProposalStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MarriageProposalRepository extends JpaRepository<MarriageProposal, Long> {

    List<MarriageProposal> findByToUserIdAndStatus(UUID toUserId, MarriageProposalStatus status);

    List<MarriageProposal> findByFromUserIdAndStatus(UUID fromUserId, MarriageProposalStatus status);

    /** Is this ring already promised to someone (a PENDING proposal already references it)? */
    Optional<MarriageProposal> findByRingUserItemIdAndStatus(Long ringUserItemId, MarriageProposalStatus status);

    /** Most recently accepted marriage site-wide — for the home page "just married" panel. */
    Optional<MarriageProposal> findFirstByStatusOrderByResolvedAtDesc(MarriageProposalStatus status);

    /**
     * Every PENDING proposal touching either side of a pair — used to auto-decline
     * everything else the instant one proposal is accepted (sent or received,
     * by either newlywed).
     */
    @Query("""
        SELECT p FROM MarriageProposal p
        WHERE p.status = live.toon.api.entity.MarriageProposalStatus.PENDING
          AND (p.fromUserId IN :userIds OR p.toUserId IN :userIds)
        """)
    List<MarriageProposal> findAllPendingInvolvingAny(@Param("userIds") List<UUID> userIds);
}
