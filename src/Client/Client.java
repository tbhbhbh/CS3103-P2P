package Client;

import java.io.File;
import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Scanner;

public class Client {

    private final int SERVER_PORT = 8100;
    private Socket clientSocket;
    private int option;
    private final String directory = "../local directory";

    public static void main(String[] args) throws IOException {
        new Client();
    }

    public Client() throws IOException {
        System.out.println("Client connecting to tracker on port " + SERVER_PORT + "\n");

        Peer peer = new Peer();

        try {
            clientSocket = new Socket("localhost", SERVER_PORT);
        } catch (IOException e) {
            System.out.println(e);
        }

        File folder = new File(directory);
        ArrayList<String> fileNames = new ArrayList<>();
        for (final File file : folder.listFiles())
            fileNames.add(file.getName());


        peer.register(clientSocket);

        Scanner scanner = new Scanner(System.in);
        while (true) {
            System.out.println("\nSelect option: ");
            System.out.println("1: List files on directory server");
            System.out.println("2: Disconnect");
            option = scanner.nextInt();
            if (option == 1) {
                System.out.println("listing files");
            }

            else if (option == 2) {
                System.out.println("Deregistering and disconnecting. Goodbye");
                disconnect(clientSocket);
                break;
            }

            else if (option > 1) {
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
