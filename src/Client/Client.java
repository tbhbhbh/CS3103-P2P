package Client;

import java.io.File;
import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Scanner;

public class Client {

    /*
    *
    *
     */
    private final int SERVER_PORT = 8080;
    private Socket clientSocket;
    private int option;
    final String DIRECTORY = "./src/files/";

    public static void main(String[] args) throws Exception {
        Client c = new Client();
        c.run();

    }

    public void run() throws Exception {
        System.out.println("Client connecting to tracker on port " + SERVER_PORT + "\n");

        Peer peer = new Peer();

        try {
            clientSocket = new Socket("localhost", SERVER_PORT);
        } catch (IOException e) {
            System.out.println(e);
        }

        File folder = new File(DIRECTORY);
        File[] listOfFiles = folder.listFiles();

        for (int i = 0; i < listOfFiles.length; i++) {
            if (listOfFiles[i].isFile()) {
                System.out.println("File " + listOfFiles[i].getName());
            } else if (listOfFiles[i].isDirectory()) {
                System.out.println("Directory " + listOfFiles[i].getName());
            }
        }

        Thread t = new Thread(){
            public void run(){
                try {
                    peer.server();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        };
        t.setDaemon(true);
        t.start();


        Scanner scanner = new Scanner(System.in);
        while (true) {
            System.out.println("\nSelect option: ");
            System.out.println("1: List files from server");
            System.out.println("2: Request for a file");
            System.out.println("3: Download a file");
            System.out.println("4: Update server on a file");
            System.out.println("5: Disconnect");
            option = scanner.nextInt();
            scanner.nextLine();
            if (option == 1) {
                System.out.println("listing files from server...");
                peer.requestListOfFiles(clientSocket);
            }

            if (option == 2) {
                System.out.println("Requesting file from server on how many chunks and peer info");
            }

            if (option == 3) {
                System.out.println("Downloading a file");
                peer.download();
            }

            if (option == 4) {
                System.out.println("Enter filename: ");
                String filename;
                filename = scanner.nextLine();
                System.out.println("Initial announcement of a file");
                peer.updateServer(clientSocket, filename);

            }

            else if (option == 5) {
                System.out.println("Deregistering and disconnecting. Goodbye");
                disconnect(clientSocket);
                break;
            }

            else if (option > 5) {
                System.out.println("Invalid option");
            }
        }
    }

    public void disconnect(Socket clientSocket){
        try {
            clientSocket.close();
        } catch (IOException e) {
            System.out.println(e);
        }
    }
}
