package Client;

import jdk.jshell.execution.Util;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Random;

public class Peer {

    private int peerId;
    private int port;
    private String fileName;
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
     * Peer updates server of a file
     *
     */
    public void updateServer(Socket socket) throws IOException {
        System.out.println("Registering peer");
        DataOutputStream dOut = new DataOutputStream(socket.getOutputStream());
        //Option to register in the server (new peer)
        dOut.writeByte(0);
        dOut.flush();

        //Files names
        dOut.writeByte(1);
        dOut.writeUTF(fileName);
        dOut.flush();
        //Number of chunks
        dOut.writeByte(2);
        dOut.writeInt(numChunks);
        dOut.flush();
        //port number
        dOut.writeByte(3);
        dOut.writeInt(port);
        dOut.flush();
        //end connection
        dOut.writeByte(-1);
        dOut.flush();

        dOut.close();
        socket.close();
    }

    //Uploading
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

    //Downloading
    public void download(String peerAddress, int port, String fileName, int i)  throws IOException {
        Socket socket = new Socket(peerAddress, port);
        DataOutputStream dOut = new DataOutputStream(socket.getOutputStream());
        dOut.writeUTF(fileName);
        InputStream in = socket.getInputStream();

        String folder = "downloads-peer" + peerId + "/";
        File f = new File(folder);
        Boolean created = false;
        if (!f.exists()){
            try {
                created = f.mkdir();
            }catch (Exception e){
                System.out.println("Couldn't create the folder, the file will be saved in the current directory!");
            }
        }else {
            created = true;
        }

        if(i != -1) fileName = fileName + i;

        OutputStream out = (created) ? new FileOutputStream(f.toString() + "/" + fileName) : new FileOutputStream(fileName);
        System.out.println("File " + fileName + " received from peer " + peerAddress + ":" + port);
        dOut.close();
        out.close();
        in.close();
        socket.close();
    }


}
