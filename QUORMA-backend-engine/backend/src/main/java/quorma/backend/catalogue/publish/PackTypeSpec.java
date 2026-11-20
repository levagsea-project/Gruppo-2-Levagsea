package quorma.backend.catalogue.publish;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;
import org.springframework.http.HttpStatus;
import quorma.backend.common.ErrorCodes;
import tools.jackson.databind.JsonNode;

/**
 * A pack type as a batch, a draft or the admin API writes it, checked once for all of them: its code, the one set it
 * belongs to, the price, how many creatures of each Tier it holds and how many spells or equipment ({@code support}).
 * Whether the pool of the set can fill the slots is a different question, which the engine answers when the type is
 * published.
 *
 * @param code     a-z, 0-9, _ (1 to 32 characters)
 * @param setCode  the set the pack draws from
 * @param price    credits, above 0
 * @param slots    creatures per Tier, {@code TIER_1}..{@code TIER_4} (0 when absent)
 * @param support  spells and equipment, no Tier
 * @param featured the shop highlights it
 */
public record PackTypeSpec(String code, String setCode, long price, Map<String, Integer> slots, int support, boolean featured) {

    private static final Pattern CODE = Pattern.compile("^[a-z0-9_]{1,32}$");
    public static final java.util.List<String> TIERS = java.util.List.of("TIER_1", "TIER_2", "TIER_3", "TIER_4");

    /**
     * @param code the code from the path, or null to read it from the body
     * @param body {@code {code?, set, price, slots: {TIER_n: n}, support_slots, featured}}
     * @throws CatalogueException 400 {@code catalogue.pack_type_invalid}, with the reason
     */
    public static PackTypeSpec parse(String code, JsonNode body) {
        if (body == null || !body.isObject()) {
            throw invalid("a pack type is an object");
        }
        for (String key : body.propertyNames()) {
            if (!java.util.Set.of("code", "set", "price", "slots", "support_slots", "featured").contains(key)) {
                throw invalid("unknown key '" + key + "'");
            }
        }
        String bodyCode = body.has("code") && body.get("code").isString() ? body.get("code").asString() : null;
        String c = code != null ? code : bodyCode;
        if (c == null || !CODE.matcher(c).matches()) {
            throw invalid("code is a-z, 0-9, _ (1 to 32 characters)");
        }
        if (bodyCode != null && !bodyCode.equals(c)) {
            throw invalid("code in the body is not the one in the path");
        }
        String set = body.has("set") && body.get("set").isString() ? body.get("set").asString() : null;
        if (set == null || set.isBlank()) {
            throw invalid("set is the code of the set the pack draws from");
        }
        JsonNode price = body.get("price");
        if (price == null || !price.isIntegralNumber() || price.asLong() <= 0) {
            throw invalid("price is a whole number above 0");
        }
        Map<String, Integer> slots = new LinkedHashMap<>();
        JsonNode slotNode = body.get("slots");
        if (slotNode != null && !slotNode.isObject()) {
            throw invalid("slots is an object of TIER_1..TIER_4");
        }
        if (slotNode != null) {
            for (String tier : slotNode.propertyNames()) {
                if (!TIERS.contains(tier)) {
                    throw invalid("slots." + tier + ": a Tier is TIER_1, TIER_2, TIER_3 or TIER_4");
                }
            }
        }
        int total = 0;
        for (String tier : TIERS) {
            JsonNode n = slotNode == null ? null : slotNode.get(tier);
            int v = n == null ? 0 : n.isIntegralNumber() ? n.asInt() : -1;
            if (v < 0 || v > 1000) {
                throw invalid("slots." + tier + " is a whole number from 0 to 1000");
            }
            slots.put(tier, v);
            total += v;
        }
        JsonNode support = body.get("support_slots");
        int supportSlots = support == null ? 0 : support.isIntegralNumber() ? support.asInt() : -1;
        if (supportSlots < 0 || supportSlots > 1000) {
            throw invalid("support_slots is a whole number from 0 to 1000");
        }
        if (total + supportSlots < 1) {
            throw invalid("a pack holds at least one card");
        }
        JsonNode featured = body.get("featured");
        if (featured != null && !featured.isBoolean()) {
            throw invalid("featured is true or false");
        }
        return new PackTypeSpec(c, set, price.asLong(), slots, supportSlots, featured != null && featured.asBoolean());
    }

    private static CatalogueException invalid(String reason) {
        return new CatalogueException(HttpStatus.BAD_REQUEST, ErrorCodes.CATALOGUE_PACK_TYPE_INVALID, reason);
    }
}
