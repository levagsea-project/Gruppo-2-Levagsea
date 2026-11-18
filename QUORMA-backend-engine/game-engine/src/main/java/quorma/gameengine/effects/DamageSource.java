package quorma.gameengine.effects;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/** Where E58 takes the amount it scales from. The IDs are the values of the {@code source} param in the data. */
enum DamageSource {

    /** The ATK of the creature whose ability it is. */
    OWN_ATK("own_atk"),
    /** The damage the creature took (T13). */
    DAMAGE_TAKEN("damage_taken"),
    /** The damage the creature dealt (T38). */
    DAMAGE_DEALT("damage_dealt");

    private final String id;

    DamageSource(String id) {
        this.id = id;
    }

    String id() {
        return id;
    }

    /** The IDs the data may use. */
    static Set<String> ids() {
        return Arrays.stream(values()).map(DamageSource::id).collect(Collectors.toUnmodifiableSet());
    }

    static DamageSource of(String id) {
        return Arrays.stream(values()).filter(s -> s.id.equals(id)).findFirst()
                .orElseThrow(() -> new IllegalStateException("unknown damage source " + id));
    }
}
