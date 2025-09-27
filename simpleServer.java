import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

public class simpleServer {
    public static void main(String[] args) throws IOException {
        int port = 8000; // localhost:8000
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/", new RootHandler());
        server.createContext("/api/data", new ApiDataHandler());
        server.setExecutor(null); // default executor
        System.out.println("Server started at http://localhost:" + port);
        server.start();
    }

    static class RootHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String htmlResponse = """
                <!DOCTYPE html>
                <html lang="en">
                <head>
                    <meta charset="UTF-8" />
                    <meta name="viewport" content="width=device-width, initial-scale=1" />
                    <title>Java Localhost Server with POST</title>
                    <style>
                        body { font-family: Arial, sans-serif; margin: 30px; }
                        button { padding: 10px 20px; font-size: 16px; cursor: pointer; }
                        #message { margin-top: 20px; font-weight: bold; }
                    </style>
                </head>
                <body>
                    <h1>Send JSON to Java Server!</h1>
                    <button onclick="sendData()">Send Data to Server</button>
                    <div id="message"></div>

                    <script>
                        function sendData() {
                            fetch('/api/data', {
                                method: 'POST',
                                headers: {
                                    'Content-Type': 'application/json'
                                },
                                body: JSON.stringify({ message: 'Hello from client!' })
                            })
                            .then(response => response.json())
                            .then(data => {
                                document.getElementById('message').textContent = 'Server replied: ' + data.reply;
                            })
                            .catch(err => {
                                document.getElementById('message').textContent = 'Error: ' + err;
                            });
                        }
                    </script>
                </body>
                </html>
                """;

            exchange.getResponseHeaders().add("Content-Type", "text/html; charset=UTF-8");
            exchange.sendResponseHeaders(200, htmlResponse.getBytes(StandardCharsets.UTF_8).length);
            OutputStream os = exchange.getResponseBody();
            os.write(htmlResponse.getBytes(StandardCharsets.UTF_8));
            os.close();
        }
    }

    static class ApiDataHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) {
                exchange.sendResponseHeaders(405, -1); // Method Not Allowed
                return;
            }

            InputStream is = exchange.getRequestBody();
            String requestBody = new String(is.readAllBytes(), StandardCharsets.UTF_8);

            // Very simple JSON parsing (since we avoid external libs)
            // Expects {"message": "..."}
            String clientMessage = "unknown";
            if (requestBody.contains("\"message\"")) {
                int start = requestBody.indexOf(":") + 2;
                int end = requestBody.lastIndexOf("\"");
                if (start < end) {
                    clientMessage = requestBody.substring(start, end);
                }
            }

            String jsonResponse = "{\"reply\": \"Got your message: '" + clientMessage + "'\"}";

            exchange.getResponseHeaders().add("Content-Type", "application/json; charset=UTF-8");
            exchange.sendResponseHeaders(200, jsonResponse.getBytes(StandardCharsets.UTF_8).length);
            OutputStream os = exchange.getResponseBody();
            os.write(jsonResponse.getBytes(StandardCharsets.UTF_8));
            os.close();
        }
    }
}
