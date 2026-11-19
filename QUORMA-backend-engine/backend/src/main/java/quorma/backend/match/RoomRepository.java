package quorma.backend.match;

import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RoomRepository extends JpaRepository<Room, UUID> {

    /** SELECT ... FOR UPDATE: two players joining the same room are served one after the other. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from Room r where r.code = :code")
    Optional<Room> findByCodeForUpdate(@Param("code") String code);

    boolean existsByCode(String code);

    Optional<Room> findByCreatorIdAndStatus(UUID creatorId, RoomStatus status);

    /** The most recent room a player opened, whatever happened to it. */
    Optional<Room> findFirstByCreatorIdOrderByCreatedAtDesc(UUID creatorId);

    /** Open rooms that have not expired and were not opened by {@code userId}, oldest first. */
    @Query("select r from Room r where r.status = quorma.backend.match.RoomStatus.OPEN and r.expiresAt > :now and r.creatorId <> :userId order by r.createdAt")
    List<Room> findJoinable(@Param("userId") UUID userId, @Param("now") Instant now);
}
