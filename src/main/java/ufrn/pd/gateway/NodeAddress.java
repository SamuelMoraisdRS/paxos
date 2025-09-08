package ufrn.pd.gateway;

public record NodeAddress(String ip, int port) {
    @Override
    public String toString() {
        return String.format("%s:%d", ip, port);
    }
    public static NodeAddress fromString(String address) {
        String [] addressParts = address.split(":");
        return new NodeAddress(addressParts[0], Integer.parseInt(addressParts[1]));
    }
}
