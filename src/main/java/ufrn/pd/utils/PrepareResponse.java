package ufrn.pd.utils;

public record PrepareResponse(
        boolean promise,
        Optional<AcceptedValue> acceptedValue,
        Optional<AcceptedGeneration> acceptedGeneration
) {
}
