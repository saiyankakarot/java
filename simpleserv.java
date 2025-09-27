import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

public class simpleserv {
    private static List<SearchResult> database;

    public static void main(String[] args) throws IOException {
        // Load data from data.json file into database list
        ObjectMapper mapper = new ObjectMapper();
        database = mapper.readValue(
            Files.newInputStream(Paths.get("data.json")),
            new TypeReference<List<SearchResult>>() {});

        int port = 8000;
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/", new RootHandler());
        server.createContext("/api/search", new SearchHandler());
        server.setExecutor(null);
        System.out.println("Server started at http://localhost:" + port);
        server.start();
    }

    // Serves the HTML page with search box and results area
    static class RootHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String htmlResponse = """
                <!DOCTYPE html>
                <html lang="en">
                <head>
                    <meta charset="UTF-8" />
                    <meta name="viewport" content="width=device-width, initial-scale=1" />
                    <title>Simple Java Search Engine</title>
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

    // Handles the POST search request and returns matching results as JSON
    static class SearchHandler implements HttpHandler {
        private final ObjectMapper mapper = new ObjectMapper();

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) {
                exchange.sendResponseHeaders(405, -1); // Method Not Allowed
                return;
            }

            InputStream is = exchange.getRequestBody();
            SearchQuery queryObj = mapper.readValue(is, SearchQuery.class);
            String query = queryObj.query == null ? "" : queryObj.query.toLowerCase();

            // Filter database for matches in title or snippet
            List<SearchResult> results = database.stream()
                .filter(item -> item.title.toLowerCase().contains(query) ||
                                item.snippet.toLowerCase().contains(query))
                .collect(Collectors.toList());

            SearchResponse response = new SearchResponse(results);
            String jsonResponse = mapper.writeValueAsString(response);

            exchange.getResponseHeaders().add("Content-Type", "application/json; charset=UTF-8");
            exchange.sendResponseHeaders(200, jsonResponse.getBytes(StandardCharsets.UTF_8).length);
            OutputStream os = exchange.getResponseBody();
            os.write(jsonResponse.getBytes(StandardCharsets.UTF_8));
            os.close();
        }
    }

    // Class to hold the search query sent by client
    public static class SearchQuery {
        public String query;
    }

    // Class to wrap search results in a response
    public static class SearchResponse {
        public List<SearchResult> results;

        public SearchResponse(List<SearchResult> results) {
            this.results = results;
        }
    }

    // Represents one search result item
    public static class SearchResult {
        public String title;
        public String url;
        public String snippet;

        // Default constructor needed for Jackson
        public SearchResult() {}

        public SearchResult(String title, String url, String snippet) {
            this.title = title;
            this.url = url;
            this.snippet = snippet;
        }
    }
}
