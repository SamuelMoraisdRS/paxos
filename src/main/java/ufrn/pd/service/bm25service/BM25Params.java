package ufrn.pd.service.bm25service;

import java.util.List;
import java.util.Map;

public record BM25Params(
        List<String> corpus,
        Map<String, Integer> documentFrequency,
        double k1,
        double b,
        double avgDocLength
) {
}
