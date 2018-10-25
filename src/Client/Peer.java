package Client;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Random;

public class Peer {

    private int peerId;
    private int port;
    private ArrayList<String> listOfFiles = new ArrayList<>();
    private String fileName;
    private String fileNameHash;
    private int numChunks;

    public ServerSocket serverSocket;

    public Peer() {
        this.port = generatePort();
    }

    // getters
    public int getPeerId() { return peerId; }
    public int getPort() { return port; }

    // setters
    public void setPeerId() { this.peerId = peerId; }
    public void setPort() { this.port = port; }

    /*
     * Randomly generates a port number for client to use for their socket.
     */
    private int generatePort() {
        Random r = new Random();
        return r.nextInt(9000-8100) + 8100;
    }

    /*
     * Peer registers itself to the central directory server.
     *
     */
    public void register(Socket socket) throws IOException {
        System.out.println("Registering peer");
        DataOutputStream dataOutToServer = new DataOutputStream(socket.getOutputStream());
        //Option to register in the server (new peer)
        dataOutToServer.writeByte(0);
        dataOutToServer.flush();
    }

    public void server() throws IOException {
        try {
            serverSocket = new ServerSocket(port);
        } catch (Exception e) {
            System.out.println(e);
            return;
        }

        while (true) {
            Socket socket = serverSocket.accept();
            System.out.println("Accepted connection from peer\n");
        }
    }
}
