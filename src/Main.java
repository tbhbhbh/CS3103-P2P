import java.util.InputMismatchException;
import java.io.IOException;
import java.util.Scanner;
import Client.Client;
import Tracker.Server;

public class Main {
    public static void main(String[] args) throws Exception {
        Scanner sc = new Scanner(System.in);

        boolean isValidChoice = false;

        do {
            displayMenu();
            int choice = -1;
            try {
                choice = sc.nextInt();
                isValidChoice = true;
            } catch (InputMismatchException e) {
                sc.nextLine();
                // log exception
            }

            switch (choice) {
                case 1: try {
                            new Client();
                        } catch (NullPointerException e) {
                            // log exception
                            isValidChoice = false;
                        }
                        break;
                case 2: try {
                            new Server();
                        } catch (IOException e) {
                            // log exception
                            isValidChoice = false;
                        }
                        break;
                case 3: System.out.print("Exiting Application...");
                        System.exit(1);
                        break;
                default: System.out.println("Please enter a valid choice.\n");

            }
        } while (!isValidChoice);
    }

    private static void displayMenu() {
        System.out.println("===================================");
        System.out.println("== P2P Bit-Torrent Client/Server ==");
        System.out.println("===================================");
        System.out.println("Select which application to run");
        System.out.println("1. Client");
        System.out.println("2. Server");
        System.out.println("3. Exit");
        System.out.print("Enter your choice: ");
    }
}
