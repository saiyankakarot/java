import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class Simpleserver {
    public static void main(String[] args) throws IOException {
        int port = 8000;
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/", new RootHandler());
        server.createContext("/api/search", new SearchHandler());
        server.setExecutor(null);
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
                    <title>Simple Java Search</title>
                    <style>
                        body { font-family: Arial, sans-serif; margin: 40px; }
                        input[type=text] {
                            width: 300px;
                            padding: 10px;
                            font-size: 16px;
                            border: 1px solid #ccc;
                            border-radius: 4px;
                        }
                        button {
                            padding: 10px 15px;
                            font-size: 16px;
                            background-color: #4CAF50;
                            color: white;
                            border: none;
                            border-radius: 4px;
                            cursor: pointer;
                        }
                        button:hover {
                            background-color: #45a049;
                        }
                        #results {
                            margin-top: 30px;
                        }
                        .result-item {
                            padding: 10px;
                            border-bottom: 1px solid #ddd;
                        }
                        .result-title {
                            font-weight: bold;
                            font-size: 18px;
                            color: #1a0dab;
                            cursor: pointer;
                        }
                        .result-snippet {
                            font-size: 14px;
                            color: #545454;
                        }
                    </style>
                </head>
                <body>
                    <h1>Simple Java Search Engine</h1>
                    <input type="text" id="query" placeholder="Type your search..." />
                    <button onclick="search()">Search</button>
                    <div id="results"></div>

                    <script>
                        function search() {
                            const query = document.getElementById('query').value.trim();
                            if (!query) {
                                alert('Please enter a search term.');
                                return;
                            }

                            fetch('/api/search', {
                                method: 'POST',
                                headers: { 'Content-Type': 'application/json' },
                                body: JSON.stringify({ query: query })
                            })
                            .then(response => response.json())
                            .then(data => {
                                const resultsDiv = document.getElementById('results');
                                resultsDiv.innerHTML = '';

                                if (data.results.length === 0) {
                                    resultsDiv.textContent = 'No results found.';
                                    return;
                                }

                                data.results.forEach(item => {
                                    const div = document.createElement('div');
                                    div.className = 'result-item';

                                    const title = document.createElement('div');
                                    title.className = 'result-title';
                                    title.textContent = item.title;
                                    title.onclick = () => window.open(item.url, '_blank');

                                    const snippet = document.createElement('div');
                                    snippet.className = 'result-snippet';
                                    snippet.textContent = item.snippet;

                                    div.appendChild(title);
                                    div.appendChild(snippet);
                                    resultsDiv.appendChild(div);
                                });
                            })
                            .catch(err => {
                                alert('Error: ' + err);
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

    static class SearchHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) {
                exchange.sendResponseHeaders(405, -1); // Method Not Allowed
                return;
            }

            InputStream is = exchange.getRequestBody();
            String requestBody = new String(is.readAllBytes(), StandardCharsets.UTF_8);

            // Simple parsing to extract the query value from JSON:
            String query = extractJsonValue(requestBody, "query").toLowerCase();

            // Dummy search database (list of items)
            List<SearchResult> database = List.of(
                new SearchResult("Java Tutorial", "https://example.com/java-tutorial", "Learn Java programming with this easy tutorial."),
                new SearchResult("HTTP Server in Java", "https://example.com/java-http-server", "Build a simple HTTP server using Java's built-in classes."),
                new SearchResult("JavaScript Basics", "https://example.com/js-basics", "Understand the basics of JavaScript programming."),
                new SearchResult("CSS Styling Tips", "https://example.com/css-tips", "Improve your website's look with CSS tips and tricks."),
                new SearchResult("Advanced Java Programming", "https://example.com/advanced-java", "Deep dive into advanced concepts in Java.")
            );

            // Find matching results
            List<SearchResult> results = new ArrayList<>();
            for (SearchResult item : database) {
                if (item.title.toLowerCase().contains(query) || item.snippet.toLowerCase().contains(query)) {
                    results.add(item);
                }
            }

            // Build JSON response manually
            StringBuilder jsonResponse = new StringBuilder();
            jsonResponse.append("{\"results\":[");

            for (int i = 0; i < results.size(); i++) {
                SearchResult r = results.get(i);
                jsonResponse.append("{");
                jsonResponse.append("\"title\":\"").append(escapeJson(r.title)).append("\",");
                jsonResponse.append("\"url\":\"").append(escapeJson(r.url)).append("\",");
                jsonResponse.append("\"snippet\":\"").append(escapeJson(r.snippet)).append("\"");
                jsonResponse.append("}");
                if (i != results.size() - 1) jsonResponse.append(",");
            }
            jsonResponse.append("]}");

            exchange.getResponseHeaders().add("Content-Type", "application/json; charset=UTF-8");
            exchange.sendResponseHeaders(200, jsonResponse.toString().getBytes(StandardCharsets.UTF_8).length);
            OutputStream os = exchange.getResponseBody();
            os.write(jsonResponse.toString().getBytes(StandardCharsets.UTF_8));
            os.close();
        }

        // Helper method to extract a simple string value from JSON { "query": "something" }
        private String extractJsonValue(String json, String key) {
            String pattern = "\"" + key + "\"";
            int index = json.indexOf(pattern);
            if (index == -1) return "";
            int colon = json.indexOf(":", index);
            if (colon == -1) return "";
            int startQuote = json.indexOf("\"", colon);
            int endQuote = json.indexOf("\"", startQuote + 1);
            if (startQuote == -1 || endQuote == -1) return "";
            return json.substring(startQuote + 1, endQuote);
        }

        // Escape characters for JSON string (minimal)
        private String escapeJson(String s) {
            return s.replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r");
        }
    }

    static class SearchResult {
        String title;
        String url;
        String snippet;

        SearchResult(String title, String url, String snippet) {
            this.title = title;
            this.url = url;
            this.snippet = snippet;
        }
    }
}
