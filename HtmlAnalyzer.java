import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;

public class HtmlAnalyzer {

    /**
     * Main method to execute the HTML analysis
     * 
     * @param args Command line arguments, expects a single URL
     * @return void
     * @throws Exception in case of unexpected errors
     */
    public static void main(String[] args) {
        if (args.length == 0) {
            return;
        }

        if (args[0].equals("--test")) {
            UnitTests.run();
            return;
        }

        String urlString = args[0];
        BufferedReader reader = null;

        try {
            reader = HtmlClient.getReader(urlString);

            String result = HtmlParser.findDeepestText(reader);

            System.out.println(result);

        } catch (MalformedHtmlException e) {
            System.out.println("malformed HTML");
        } catch (java.io.IOException e) {
            System.out.println("URL connection error");
        } catch (Exception e) {
            // Generic exception catch for unexpected errors
            System.out.println("Internal error");
        } finally {
            // Ensuring the closure of hardware (socket/stream)
            if (reader != null) {
                try {
                    reader.close();
                } catch (java.io.IOException ignored) {
                }
            }
        }
    }

    private static class HtmlParser {
        private static final int STACK_CAPACITY = 1024;
        private static final String[] stack = new String[STACK_CAPACITY];
        private static int top = -1;

        /**
         * Finds the deepest text content in the provided BufferedReader
         * 
         * @param reader BufferedReader containing HTML content
         * @return The deepest text content found
         * @throws Exception in case of malformed HTML or reading issues
         */
        public static String findDeepestText(BufferedReader reader) throws Exception {
            String deepestText = "";
            int maxDepth = -1;
            int currentDepth = 0;
            String line;

            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty())
                    continue;

                if (isTag(line)) {
                    if (isClosingTag(line)) {
                        String tagName = extractTagName(line);
                        handleClosingTag(tagName);
                        currentDepth--;
                    } else {
                        String tagName = extractTagName(line);
                        handleOpeningTag(tagName);
                        currentDepth++;
                    }
                } else {
                    if (currentDepth > maxDepth) {
                        maxDepth = currentDepth;
                        deepestText = line;
                    }
                }
            }

            if (top != -1)
                throw new MalformedHtmlException();

            return deepestText;
        }

        // =============================
        // ========== Helpers ==========
        // =============================

        /**
         * Checks if the line is an HTML tag
         * 
         * @param line The line to check
         * @return true if it's a tag, false otherwise
         */
        private static boolean isTag(String line) {
            return line.startsWith("<") && line.endsWith(">");
        }

        /**
         * Checks if the tag is a closing tag
         * 
         * @param line The line to check
         * @return true if it's a closing tag, false otherwise
         */
        private static boolean isClosingTag(String line) {
            return line.startsWith("</");
        }

        /**
         * Extracts the tag name from the line
         * 
         * @param line The line containing the tag
         * @return The extracted tag name
         */
        private static String extractTagName(String line) {
            int start = line.startsWith("</") ? 2 : 1;
            return line.substring(start, line.length() - 1);
        }

        /**
         * Handles an opening tag by pushing it onto the stack
         * 
         * @param tagName The name of the tag
         * @throws MalformedHtmlException if the stack overflows
         */
        private static void handleOpeningTag(String tagName) throws MalformedHtmlException {
            if (top + 1 >= STACK_CAPACITY)
                throw new MalformedHtmlException();
            stack[++top] = tagName;
        }

        /**
         * Handles a closing tag by popping from the stack
         * 
         * @param tagName The name of the tag
         * @throws MalformedHtmlException if the tag does not match the top of the stack
         */
        private static void handleClosingTag(String tagName) throws MalformedHtmlException {
            if (top == -1 || !stack[top].equals(tagName)) {
                throw new MalformedHtmlException();
            }
            top--;
        }
    }

    private static class HtmlClient {
        private static final int TIMEOUT_MS = 5000;

        /**
         * Establishes a connection to the provided URL and returns a BufferedReader
         * 
         * @param urlString The URL HTTP to connect to
         * @return BufferedReader to read the content of the URL
         * @throws Exception in case of a connection or reading failure
         */
        public static BufferedReader getReader(String urlString) throws Exception {
            URL url = new URL(urlString);
            URLConnection connection = url.openConnection();

            // Headers and timeouts for maximum compatibility
            connection.setConnectTimeout(TIMEOUT_MS);
            connection.setReadTimeout(TIMEOUT_MS);
            connection.setRequestProperty("User-Agent", "HtmlAnalyzer/1.0");

            return new BufferedReader(new InputStreamReader(connection.getInputStream()));
        }
    }

    protected static class MalformedHtmlException extends Exception {
        public MalformedHtmlException() {
            super();
        }
    }

    static class UnitTests {
        private static final String BASE_URL = "http://hiring.axreng.com/internship/example";
        private static final String[] REAL_WORLD_URLS = {
                "https://engineering.axur.com/2025/03/25/praticas-de-engenharia-de-software-na-axur.html",
                "https://www.scidev.net/global/enterprise/technology"
        };

        private static final String[] EXPECTED = {
                "This is the body.",                         // example1.html
                "This is in level 4. Correct result.",       // example2.html
                "malformed HTML",                            // example3.html
                "malformed HTM",                             // example4.html
                "malformed HTM",                             // example5.html
                "aster egg | Este não é só mais um exemplo", // example6.html
                "URL connection error"                       // example7.html
        };
        private static final String[] EXPECTED_REAL_URLS = {
                "Compartilhe em:", // engineering.axur.com
                "2026"             // scidev.net
        };

        /**
         * Executes a single test case
         * 
         * @return Void
         */
        public static void run() {
            System.out.println("\n--- STARTING UNIT TESTS SUITE ---");
            int passed = 0;

            for (int i = 0; i < EXPECTED.length; i++) {
                if (executeTest("Test #" + (i + 1), BASE_URL + (i + 1) + ".html", EXPECTED[i]))
                    passed++;
            }

            System.out.println("--- STARTING REAL WORLD VALIDATION ---");
            for (int i = 0; i < REAL_WORLD_URLS.length; i++) {
                executeTest("Real World #" + (i + 1), REAL_WORLD_URLS[i], EXPECTED_REAL_URLS[i]);
            }

            System.out.println("--- SUITE COMPLETED: " + passed + "/" + EXPECTED.length + " CONTROLLED PASSED ---\n");
        }

        // =============================
        // ========== Helpers ==========
        // =============================

        private static boolean executeTest(String label, String url, String expected) {
            long startTime = System.currentTimeMillis();
            String result = performSingleTest(url);
            long duration = System.currentTimeMillis() - startTime;

            return handleTestFeedback(label, url, expected, result, duration);
        }

        /**
         * Performs a single test by fetching the URL and parsing its content
         * 
         * @param url The URL to test
         * @return The result of the parsing
         */
        private static String performSingleTest(String url) {
            try (java.io.BufferedReader reader = HtmlClient.getReader(url)) {
                return HtmlParser.findDeepestText(reader);
            } catch (MalformedHtmlException e) {
                return "malformed HTML";
            } catch (Exception e) {
                return "URL connection error";
            }
        }

        /**
         * Handles the feedback of a test case
         * 
         * @param label    The label of the test
         * @param url      The URL tested
         * @param expected The expected result
         * @param result   The actual result
         * @param duration The duration of the test in milliseconds
         * @return true if the test passed, false otherwise
         */
        private static boolean handleTestFeedback(String label, String url, String expected, String result,
                long duration) {
            boolean success = result.equals(expected);
            System.out.println("[" + label + "] URL: " + url);
            System.out.println("  Result: " + result + " | Expected: " + expected);
            System.out.println("  Status: " + (success ? "SUCCESS" : "FAILURE") + " (" + duration + "ms)\n");
            return success;
        }
    }
}