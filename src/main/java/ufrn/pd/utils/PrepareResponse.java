package ufrn.pd.utils;

import java.util.Optional;

public record PrepareResponse implements Comparable<Generation> (
        boolean promise,
        Optional<String> acceptedValue,
        Optional<Generation> acceptedGeneration
) {
        @Override
        public int compareTo(Generation g) {
                if
                int comp = acceptedGeneration.get().generationNumber().compareTo(g.generationNumber());
                if (comp != 0) {
                        return comp;
                }

                // Use
                return acceptedGeneration.compareTo(g);
        }
}
