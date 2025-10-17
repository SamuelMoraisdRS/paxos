package ufrn.pd.service.bm25service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;

public class BM25ParamsMapper {

    public static String serializeBM25Params(BM25Params params) {
        // Usando Jackson, mas nao para serializar/deserializar em JSON, mas apenas para obter as instancias dos objetos a partir da representacao
        // em string
        ObjectMapper mapper = new ObjectMapper();
        String corpusString = null;
        try {
            corpusString = mapper.writeValueAsString(params.corpus());
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        String docFreqString = null;
        try {
            docFreqString = mapper.writeValueAsString(params.documentFrequency());
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        return String.join("|||",
                corpusString,
                docFreqString,
                String.valueOf(params.k1()),
                String.valueOf(params.b()),
                String.valueOf(params.avgDocLength())
        );
    }

    public static BM25Params deserializeBM25Params(String payload) {
        // TODO : Isso deveria estar na camada de protocolo
        String[] parts = payload.split("\\|\\|\\|");
        if (parts.length != 5) {
            throw new IllegalArgumentException("Formato inválido para BM25Params: " + payload);
        }

        ObjectMapper mapper = new ObjectMapper();

        List<String> corpus = null;
        try {
            corpus = mapper.readValue(parts[0], new TypeReference<>() {});
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        Map<String, Integer> docFreq = null;
        try {
            docFreq = mapper.readValue(parts[1], new TypeReference<>() {});
        } catch (JsonProcessingException e) {
            System.out.println("BM25ParamsMapper - payload : " + payload);
            throw new RuntimeException(e);
        }
        double k1 = Double.parseDouble(parts[2]);
        double b = Double.parseDouble(parts[3]);
        double avgDocLength = Double.parseDouble(parts[4]);

        return new BM25Params(corpus, docFreq, k1, b, avgDocLength);
    }
}
