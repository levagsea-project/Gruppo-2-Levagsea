package quorma.backend.match;

import java.util.UUID;
import quorma.backend.catalogue.Faction;

/**
 * A deck frozen at the moment it was put up: the pool snapshot it uses and the engine's {@code player.deck} array
 * (full card definitions) as JSON. The backend stores and passes it on without interpreting it.
 */
public record LockedDeck(Faction faction, UUID poolSnapshotId, String deck) {
}
