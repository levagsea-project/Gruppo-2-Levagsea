package quorma.backend.telemetry;

import java.util.UUID;
import tools.jackson.databind.JsonNode;

/** One game as the series runner played it: the stored match and the engine's log (which is not stored). */
public record GameLog(UUID matchId, UUID snapshotIdA, UUID snapshotIdB, JsonNode log) {
}
