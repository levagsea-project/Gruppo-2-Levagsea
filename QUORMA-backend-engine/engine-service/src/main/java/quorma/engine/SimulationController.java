package quorma.engine;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1")
public class SimulationController {

    private final SimulationService simulation;

    public SimulationController(SimulationService simulation) {
        this.simulation = simulation;
    }

    @PostMapping(value = "/simulate", consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> simulate(@RequestBody String inputJson) {
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(simulation.simulate(inputJson));
    }
}
