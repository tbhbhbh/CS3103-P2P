package Tracker;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

public class Server {

    /*a
    * 1) initial announcement
    * peerIP
    * peerPort
    * fileName
    * numChunks
    *
     */

    private static final int LISTENING_PORT = 8080;
    private static ServerSocket serverSocket;

    private static ArrayList<String> fileList;

    public Server() throws Exception {
        run();
    }

    private static void run() throws Exception {
        System.out.println("Starting P2P Server...");
        System.out.println("Init server on port " + LISTENING_PORT + "\n");

        ServerSocket serverSocket = new ServerSocket(LISTENING_PORT);

        while (true) {
            System.out.println("Waiting for peer\n");
            Socket clientSocket = serverSocket.accept();
            handleClientSocket(clientSocket);
        }
    }

    private static void handleClientSocket(Socket clientSocket) {
        System.out.println("Client connected\n");
    }

}