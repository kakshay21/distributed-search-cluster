package network;

public interface OnRequestCallback {
    byte[] handleRequest(byte[] requestPayload);

    String getEndpoint();
}
