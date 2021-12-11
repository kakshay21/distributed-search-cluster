package network;

import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

public class WebSever {
    private static final String ENDPOINT = "/status";
    private final int port;
    private HttpServer server;
    private final OnRequestCallback onRequestCallback;

    public WebSever(int port, OnRequestCallback onRequestCallback) {
        this.port = port;
        this.onRequestCallback = onRequestCallback;
    }

    public void startServer() {
        try {
            this.server = HttpServer.create(new InetSocketAddress(port), 0);
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        HttpContext statusContext = server.createContext(ENDPOINT);
        HttpContext taskContext = server.createContext(onRequestCallback.getEndpoint());

        statusContext.setHandler(this::handleStatusCheckRequest);
        taskContext.setHandler(this::handleTaskRequest);

        server.setExecutor(Executors.newFixedThreadPool(8));
        server.start();
    }

    public void close() {
        server.stop(0);
    }

    private void handleStatusCheckRequest(HttpExchange exchange) throws IOException {
        if (!exchange.getRequestMethod().equalsIgnoreCase("GET")) {
            exchange.close();
            return;
        }

        String responseMessage = "Server is alive\n";
        sendResponse(responseMessage.getBytes(), exchange);
    }

    private void handleTaskRequest(HttpExchange exchange) throws IOException {
        if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) {
            exchange.close();
        }

        byte[] response = onRequestCallback.handleRequest(IOUtils.toByteArray(exchange.getRequestBody()));
        sendResponse(response, exchange);
    }

    private void sendResponse(byte[] responseBytes, com.sun.net.httpserver.HttpExchange exchange) throws IOException {
        exchange.sendResponseHeaders(200, responseBytes.length);
        OutputStream outputStream = exchange.getResponseBody();
        outputStream.write(responseBytes);
        outputStream.flush();
        outputStream.close();
    }
}
