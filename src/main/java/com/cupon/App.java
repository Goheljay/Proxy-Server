package com.cupon;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;

/**
 * Hello world!
 */
public class App {
    public static void main(String[] args) {
        System.out.println("Server Connection");
        try(ServerSocket serverSocket = new ServerSocket(9091)) {
            System.out.println("Proxy Server is running on port " + 9091);
            while(true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Client connected: " + clientSocket.getInetAddress());

                // Handle the client request in a separate thread
                new Thread(() -> {
                    try {
                        handleClientRequest(clientSocket);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void handleClientRequest(Socket clientSocket) throws IOException {
        try (
                // Input and output streams for client communication
                InputStream clientInput = clientSocket.getInputStream();
                OutputStream clientOutput = clientSocket.getOutputStream();
                BufferedReader clientReader = new BufferedReader(new InputStreamReader(clientInput))
        ) {
            // Read the HTTP request from the client
            String requestLine = clientReader.readLine();
            if (requestLine == null || requestLine.isEmpty()) {
                clientSocket.close();
                return;
            }

            System.out.println("Request: " + requestLine);

            // Extract the host and port from the HTTP headers
            String[] requestParts = requestLine.split(" ");
            String method = requestParts[0];
            String url = requestParts[1];

            System.out.println("Target url: "+url);
            System.out.println("Target url Method: "+method);

            URL targetUrl = new URL(url);
            String host = targetUrl.getHost();
            int port = (targetUrl.getPort() == -1) ? 80 : targetUrl.getPort();

            // Establish a connection to the target server
            try (Socket serverSocket = new Socket(host, port)) {
                // Forward the client request to the target server
                OutputStream serverOutput = serverSocket.getOutputStream();
                serverOutput.write((requestLine + "\r\n").getBytes());
                String line;
                while (!(line = clientReader.readLine()).isEmpty()) {
                    serverOutput.write((line + "\r\n").getBytes());
                }
                serverOutput.write("\r\n".getBytes());
                serverOutput.flush();

                // Read the response from the target server
                InputStream serverInput = serverSocket.getInputStream();
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = serverInput.read(buffer)) != -1) {
                    // Send the response back to the client
                    clientOutput.write(buffer, 0, bytesRead);
                }
            }
        } catch (IOException e) {
            System.err.println("Error handling client request: " + e.getMessage());
        } finally {
                clientSocket.close();
        }
    }
}
