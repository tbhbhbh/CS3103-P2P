package Client;

import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Scanner;

public class Client {

    public static final String DIRECTORY = "./files/";
    public static final String INPUT_DIRECTORY = DIRECTORY + "input/";
    public static final String OUTPUT_DIRECTORY = DIRECTORY + "output/";
    private final int SERVER_PORT = 8080;
    private Socket clientSocket;
    private ObjectOutputStream oos;
    private ObjectInputStream ois;
    private int option;

    public static void main(String[] args) throws Exception {
        Client c = new Client();
        c.run();

    }

    public void run() throws Exception {
        System.out.println("Client connecting to tracker on port " + SERVER_PORT + "\n");

        Peer peer = new Peer();

        try {
            clientSocket = new Socket("localhost", SERVER_PORT);
            oos = new ObjectOutputStream(clientSocket.getOutputStream());
            oos.flush();
            ois = new ObjectInputStream(clientSocket.getInputStream());
            System.out.println(String.format("Connected! %s:%d", clientSocket.getInetAddress(), clientSocket.getPort()));


        } catch (IOException e) {
            System.out.println(e);
        }

        File folder = new File(INPUT_DIRECTORY);
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
                } catch (Exception e) {
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
                peer.getDir(ois, oos);
            }

            if (option == 2) {
                System.out.println("Enter filename: ");
                String filename;
                filename = scanner.nextLine();
                System.out.println("Requesting file from server on how many chunks and peer info");
                peer.getFile(ois, oos,filename);
            }

            if (option == 3) {
                System.out.println("Downloading a file");
                peer.download(ois, oos);
            }

            if (option == 4) {
                System.out.println("Enter filename: ");
                String filename;
                filename = scanner.nextLine();
                System.out.println("Initial announcement of a file");
                peer.updateServer(ois, oos, filename);

            }

            else if (option == 5) {
                System.out.println("Deregistering and disconnecting. Goodbye");
                peer.shutdown(ois, oos);
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
            oos.close();
            ois.close();
            clientSocket.close();
        } catch (IOException e) {
            System.out.println(e);
        }
    }
}
