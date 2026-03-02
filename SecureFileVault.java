import javax.crypto.*;
import javax.crypto.spec.*;
import java.io.*;
import java.nio.file.*;
import java.security.*;
import java.security.spec.KeySpec;
import java.util.Arrays;

public class SecureFileVault {

    private static final int KEY_SIZE = 256;
    private static final int ITERATIONS = 65536;
    private static final int SALT_LENGTH = 16;
    private static final int IV_LENGTH = 16;

    public static void main(String[] args) throws Exception {
        // if no arguments provided, launch the graphical frontend
        if (args.length == 0) {
            javax.swing.SwingUtilities.invokeLater(() -> {
                SecureFileVaultGUI gui = new SecureFileVaultGUI();
                gui.setVisible(true);
            });
            return;
        }

        String mode = args[0];

        if (mode.equalsIgnoreCase("encrypt") || mode.equalsIgnoreCase("decrypt")) {
            if (args.length < 3) {
                System.out.println("Usage: java SecureFileVault " + mode + " <file> <password>");
                return;
            }
            String filePath = args[1];
            String password = args[2];

            if (mode.equalsIgnoreCase("encrypt")) {
                encryptFile(filePath, password);
            } else {
                decryptFile(filePath, password);
            }
        } else if (mode.equalsIgnoreCase("web")) {
            // start embedded web server
            startWeb();
        } else {
            System.out.println("Invalid mode. Use encrypt, decrypt or web.");
        }
    }

    // package-private so other classes in the default package (like the GUI) can use it
    static SecretKey generateKey(String password, byte[] salt) throws Exception {
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        KeySpec spec = new PBEKeySpec(password.toCharArray(), salt, ITERATIONS, KEY_SIZE);
        SecretKey tmp = factory.generateSecret(spec);
        return new SecretKeySpec(tmp.getEncoded(), "AES");
    }
    /**
     * Encrypt a byte array in memory and return the salt+iv+ciphertext.
     */
    public static byte[] encryptBytes(byte[] fileData, String password) throws Exception {
        SecureRandom random = new SecureRandom();
        byte[] salt = new byte[SALT_LENGTH];
        random.nextBytes(salt);
        byte[] iv = new byte[IV_LENGTH];
        random.nextBytes(iv);
        SecretKey key = generateKey(password, salt);
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, key, new IvParameterSpec(iv));
        byte[] encryptedData = cipher.doFinal(fileData);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        outputStream.write(salt);
        outputStream.write(iv);
        outputStream.write(encryptedData);
        return outputStream.toByteArray();
    }

    /**
     * Decrypt a byte array produced by {@link #encryptBytes(byte[], String)}.
     */
    public static byte[] decryptBytes(byte[] fileData, String password) throws Exception {
        byte[] salt = Arrays.copyOfRange(fileData, 0, SALT_LENGTH);
        byte[] iv = Arrays.copyOfRange(fileData, SALT_LENGTH, SALT_LENGTH + IV_LENGTH);
        byte[] encryptedData = Arrays.copyOfRange(fileData, SALT_LENGTH + IV_LENGTH, fileData.length);
        SecretKey key = generateKey(password, salt);
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, key, new IvParameterSpec(iv));
        return cipher.doFinal(encryptedData);
    }
    /**
     * Encrypts the file at the given path using the provided password.  
     * This method is now public so that GUI components can call it.
     */
    public static void encryptFile(String filePath, String password) throws Exception {
        byte[] fileData = Files.readAllBytes(Paths.get(filePath));
        byte[] encrypted = encryptBytes(fileData, password);
        Files.write(Paths.get(filePath + ".enc"), encrypted);
        System.out.println("File encrypted successfully.");
    }

    public static void decryptFile(String filePath, String password) throws Exception {
        byte[] fileData = Files.readAllBytes(Paths.get(filePath));
        byte[] decrypted = decryptBytes(fileData, password);
        Files.write(Paths.get("decrypted_" + filePath.replace(".enc", "")), decrypted);
        System.out.println("File decrypted successfully.");
    }

    // ---- simple embedded HTTP server for web UI ------------------------------------------------

    private static void startWeb() throws IOException {
        com.sun.net.httpserver.HttpServer server = com.sun.net.httpserver.HttpServer.create(new java.net.InetSocketAddress(8080), 0);
        server.createContext("/", SecureFileVault::handleRoot);
        server.createContext("/api/encrypt", SecureFileVault::handleEncrypt);
        server.createContext("/api/decrypt", SecureFileVault::handleDecrypt);
        server.setExecutor(java.util.concurrent.Executors.newCachedThreadPool());
        server.start();
        System.out.println("Web UI running at http://localhost:8080");
    }

    private static void handleRoot(com.sun.net.httpserver.HttpExchange exchange) throws IOException {
        if (!"GET".equals(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(405, -1);
            return;
        }
        try {
            String filePath = "index.html";
            byte[] bytes = Files.readAllBytes(Paths.get(filePath));
            exchange.getResponseHeaders().set("Content-Type", "text/html; charset=utf-8");
            exchange.getResponseHeaders().set("Cache-Control", "no-cache");
            exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
            exchange.sendResponseHeaders(200, bytes.length);
            exchange.getResponseBody().write(bytes);
        } catch (Exception ex) {
            String fallback = "<!DOCTYPE html><html><body><h1>Secure File Vault</h1><p>Place index.html in the working directory.</p></body></html>";
            byte[] bytes = fallback.getBytes(java.nio.charset.StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "text/html; charset=utf-8");
            exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
            exchange.sendResponseHeaders(200, bytes.length);
            exchange.getResponseBody().write(bytes);
        } finally {
            exchange.close();
        }
    }

    private static void handleEncrypt(com.sun.net.httpserver.HttpExchange exchange) throws IOException {
        if (!"POST".equals(exchange.getRequestMethod())) {
            exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
            exchange.sendResponseHeaders(405, -1);
            return;
        }
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        
        String body = new String(exchange.getRequestBody().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
        System.out.println("[ENCRYPT] Received body: " + body.substring(0, Math.min(100, body.length())) + "...");
        java.util.Map<String, String> map = parseJsonProperly(body);
        String fileName = map.get("fileName");
        String fileData = map.get("fileData");
        String password = map.get("password");
        
        System.out.println("[ENCRYPT] fileName: " + fileName);
        System.out.println("[ENCRYPT] fileData length: " + (fileData != null ? fileData.length() : "null"));
        System.out.println("[ENCRYPT] password length: " + (password != null ? password.length() : "null"));
        
        try {
            if (fileData == null || fileData.isEmpty()) {
                throw new Exception("File data is empty or missing");
            }
            if (password == null || password.isEmpty()) {
                throw new Exception("Password is empty or missing");
            }
            
            System.out.println("[ENCRYPT] Decoding base64...");
            byte[] input = java.util.Base64.getDecoder().decode(fileData);
            System.out.println("[ENCRYPT] Input decoded: " + input.length + " bytes");
            
            System.out.println("[ENCRYPT] Encrypting...");
            byte[] out = encryptBytes(input, password);
            System.out.println("[ENCRYPT] Encryption successful: " + out.length + " bytes");
            
            String b64 = java.util.Base64.getEncoder().encodeToString(out);
            String resp = "{\"success\":true,\"fileName\":\"" + escapeJson(fileName + ".enc") + "\",\"fileData\":\"" + b64 + "\"}";
            byte[] bytes = resp.getBytes(java.nio.charset.StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, bytes.length);
            exchange.getResponseBody().write(bytes);
            System.out.println("[ENCRYPT] Response sent successfully");
        } catch (Exception ex) {
            System.err.println("[ENCRYPT] Exception: " + ex.getMessage());
            ex.printStackTrace();
            String resp = "{\"success\":false,\"message\":\"" + escapeJson(ex.getMessage()) + "\"}";
            byte[] bytes = resp.getBytes(java.nio.charset.StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, bytes.length);
            exchange.getResponseBody().write(bytes);
            System.out.println("[ENCRYPT] Error response sent");
        } finally {
            exchange.close();
        }
    }

    private static void handleDecrypt(com.sun.net.httpserver.HttpExchange exchange) throws IOException {
        if (!"POST".equals(exchange.getRequestMethod())) {
            exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
            exchange.sendResponseHeaders(405, -1);
            return;
        }
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        
        String body = new String(exchange.getRequestBody().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
        System.out.println("[DECRYPT] Received body: " + body.substring(0, Math.min(100, body.length())) + "...");
        java.util.Map<String, String> map = parseJsonProperly(body);
        String fileName = map.get("fileName");
        String fileData = map.get("fileData");
        String password = map.get("password");
        
        System.out.println("[DECRYPT] fileName: " + fileName);
        System.out.println("[DECRYPT] fileData length: " + (fileData != null ? fileData.length() : "null"));
        System.out.println("[DECRYPT] password length: " + (password != null ? password.length() : "null"));
        
        try {
            if (fileData == null || fileData.isEmpty()) {
                throw new Exception("File data is empty or missing");
            }
            if (password == null || password.isEmpty()) {
                throw new Exception("Password is empty or missing");
            }
            
            System.out.println("[DECRYPT] Decoding base64...");
            byte[] input = java.util.Base64.getDecoder().decode(fileData);
            System.out.println("[DECRYPT] Input decoded: " + input.length + " bytes");
            
            System.out.println("[DECRYPT] Decrypting...");
            byte[] out = decryptBytes(input, password);
            System.out.println("[DECRYPT] Decryption successful: " + out.length + " bytes");
            
            String b64 = java.util.Base64.getEncoder().encodeToString(out);
            String resp = "{\"success\":true,\"fileName\":\"" + escapeJson("decrypted_" + fileName.replaceAll("\\.enc$", "")) + "\",\"fileData\":\"" + b64 + "\"}";
            byte[] bytes = resp.getBytes(java.nio.charset.StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, bytes.length);
            exchange.getResponseBody().write(bytes);
            System.out.println("[DECRYPT] Response sent successfully");
        } catch (Exception ex) {
            System.err.println("[DECRYPT] Exception: " + ex.getMessage());
            ex.printStackTrace();
            String resp = "{\"success\":false,\"message\":\"" + escapeJson(ex.getMessage()) + "\"}";
            byte[] bytes = resp.getBytes(java.nio.charset.StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, bytes.length);
            exchange.getResponseBody().write(bytes);
            System.out.println("[DECRYPT] Error response sent");
        } finally {
            exchange.close();
        }
    }

    private static java.util.Map<String, String> parseJsonProperly(String json) {
        java.util.Map<String, String> map = new java.util.HashMap<>();
        json = json.trim();
        
        if (json.startsWith("{")) json = json.substring(1);
        if (json.endsWith("}")) json = json.substring(0, json.length() - 1);
        
        int i = 0;
        while (i < json.length()) {
            // skip whitespace
            while (i < json.length() && Character.isWhitespace(json.charAt(i))) i++;
            if (i >= json.length()) break;
            
            // parse key
            if (json.charAt(i) != '"') break;
            i++; // skip opening quote
            StringBuilder key = new StringBuilder();
            while (i < json.length() && json.charAt(i) != '"') {
                if (json.charAt(i) == '\\' && i + 1 < json.length()) {
                    i++; // skip escape char
                }
                key.append(json.charAt(i));
                i++;
            }
            if (i < json.length()) i++; // skip closing quote
            
            // skip to colon
            while (i < json.length() && json.charAt(i) != ':') i++;
            if (i < json.length()) i++; // skip colon
            
            // skip whitespace
            while (i < json.length() && Character.isWhitespace(json.charAt(i))) i++;
            
            // parse value
            StringBuilder value = new StringBuilder();
            if (i < json.length() && json.charAt(i) == '"') {
                i++; // skip opening quote
                while (i < json.length() && json.charAt(i) != '"') {
                    if (json.charAt(i) == '\\' && i + 1 < json.length()) {
                        i++; // skip escape char
                    }
                    value.append(json.charAt(i));
                    i++;
                }
                if (i < json.length()) i++; // skip closing quote
            }
            
            map.put(key.toString(), value.toString());
            
            // skip to comma or end
            while (i < json.length() && json.charAt(i) != ',' && json.charAt(i) != '}') i++;
            if (i < json.length() && json.charAt(i) == ',') i++;
        }
        
        return map;
    }
    
    private static String escapeJson(String str) {
        if (str == null) return "";
        return str.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}