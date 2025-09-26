package ufrn.pd.utils;

import ufrn.pd.utils.Generation;

import java.util.Optional;

public record PrepareResponse(
        String acceptorHost,
        int acceptorPort,
        boolean promise,
        Optional<String> acceptedValue,
        Optional<Generation> acceptedGeneration
) {
}
