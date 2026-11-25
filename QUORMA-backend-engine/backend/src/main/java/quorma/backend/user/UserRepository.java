package quorma.backend.user;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByUsernameIgnoreCase(String username);

    Optional<User> findByEmailIgnoreCase(String email);

    boolean existsByUsernameIgnoreCase(String username);

    boolean existsByEmailIgnoreCase(String email);

    /**
     * Records a login with a plain UPDATE: saving the entity would bump {@code users.version}, which guards the
     * credit balance, and make a login race with a pack purchase.
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = "update users set last_login_at = now() where id = :id", nativeQuery = true)
    void touchLogin(@Param("id") UUID id);

    /**
     * Changes the profile with a plain UPDATE for the same reason as {@link #touchLogin}: saving the entity would bump
     * {@code users.version}, which guards the credit balance, and a profile change would race with a pack purchase.
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = "update users set display_name = :displayName, avatar = :avatar where id = :id", nativeQuery = true)
    void updateProfile(@Param("id") UUID id, @Param("displayName") String displayName, @Param("avatar") String avatar);

    /** A plain UPDATE for the same reason as {@link #updateProfile}: it must not bump {@code users.version}. */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = "update users set email = :email where id = :id", nativeQuery = true)
    void updateEmail(@Param("id") UUID id, @Param("email") String email);

    /** A plain UPDATE for the same reason as {@link #updateProfile}: it must not bump {@code users.version}. */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = "update users set password_hash = :hash where id = :id", nativeQuery = true)
    void updatePasswordHash(@Param("id") UUID id, @Param("hash") String hash);

    /** Random players (not {@code me}) who logged in since {@code since}: the pool for matchmaking. */
    @Query(value = "select id from users where role = 'PLAYER' and id <> :me and last_login_at >= :since"
            + " order by random() limit :max", nativeQuery = true)
    List<UUID> findRecentPlayers(@Param("me") UUID me, @Param("since") Instant since, @Param("max") int max);

    /** SELECT ... FOR UPDATE: blocks other transactions on this row until the caller's transaction ends. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from User u where u.id = :id")
    Optional<User> findByIdForUpdate(@Param("id") UUID id);
}
