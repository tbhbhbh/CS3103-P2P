package Tracker;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

public class Server {

    /*
    * 1) initial announcement
    * peerIP
    * peerPort
    * fileName
    * numChunks
    *
     */

    private final int LISTENING_PORT = 8080;
    private ServerSocket serverSocket;

    private ArrayList<String> fileList;

    public static void main(String[] args) throws Exception {
        System.out.println("Starting P2P server\n");
        new Server();
    }


    public Server() {
        System.out.println("Init server on port " + LISTENING_PORT + "\n");

        //try to obtain server socket
        try {
            ServerSocket serverSocket = new ServerSocket(LISTENING_PORT);

            while (true) {
                System.out.println("Waiting for peer\n");
                Socket clientSocket = serverSocket.accept();
                handleClientSocket(clientSocket);

            }
        } catch (IOException e) {
            System.out.println(e);
        }


    }

    private void handleClientSocket(Socket clientSocket) {
        System.out.println("Client connected\n");
    }

}