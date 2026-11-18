package quorma.gameengine;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/** The engine's own version, from the Maven build ({@code engine.properties}), recorded in every MatchLog. */
public final class EngineVersion {

    /** For example {@code 1.0.0-SNAPSHOT}. */
    public static final String VERSION = load();

    private EngineVersion() {}

    private static String load() {
        try (InputStream in = EngineVersion.class.getResourceAsStream("engine.properties")) {
            if (in == null) {
                throw new IllegalStateException("engine.properties is missing: build the engine with Maven");
            }
            Properties p = new Properties();
            p.load(in);
            String version = p.getProperty("engine.version", "");
            if (version.isBlank() || version.startsWith("${")) {
                throw new IllegalStateException("engine.properties was not filtered by Maven: " + version);
            }
            return version;
        } catch (IOException e) {
            throw new IllegalStateException("cannot read engine.properties", e);
        }
    }
}
