package quorma.backend.engine;

/** Where the identity of the running engine comes from ({@link HttpGameEngine} asks the engine service). */
public interface EngineInfoSource {

    /**
     * @throws EngineUnavailableException if the engine service cannot be reached or does not answer properly
     */
    EngineInfo info();
}
