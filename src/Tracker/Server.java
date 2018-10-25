package Tracker;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import Commons.cInfo;
import Commons.fInfo;

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

    private ArrayList<fInfo> fileList;

    public static void main(String[] args) throws Exception {
        System.out.println("Starting P2P server\n");
        new Server();
    }


    public Server() throws Exception{
        fileList = new ArrayList<>();
        System.out.println("Init server on port " + LISTENING_PORT + "\n");

        //try to obtain server socket
        try {
            ServerSocket serverSocket = new ServerSocket(LISTENING_PORT);

            while (true) {
                System.out.println("Waiting for peer\n");
                Socket clientSocket = serverSocket.accept();
                Thread t = new Thread(){
                    public void run(){
                        try {
                            handleClientSocket(clientSocket);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                    }
                };
                t.setDaemon(true);
                t.start();
            }
        } catch (IOException e) {
            System.out.println(e);
        }


    }

    private void handleClientSocket(Socket socket) throws Exception{
        System.out.println("Client connected\n");
        DataInputStream dIn = new DataInputStream(socket.getInputStream());
        DataOutputStream dOut = new DataOutputStream(socket.getOutputStream());

        while (true) {
            byte option = dIn.readByte();

            // Inform server about availability of files
            if (option == 0) { // register
                int clientPort = dIn.readInt();
                String clientIP = socket.getInetAddress().getHostAddress();
                InetSocketAddress clientAddress = new InetSocketAddress(clientIP, clientPort);
                if (dIn.readByte() == 1) { // filename
                    String filename = dIn.readUTF();
                    fInfo fileInfo = new fInfo(filename);
                    if (dIn.readByte() == 2) { // num of chunks
                        int numChunks = dIn.readInt();
                        for (int i = 0; i< numChunks; i++) {
                            cInfo chunk = new cInfo(i);
                            chunk.addPeer(clientAddress);
                            fileInfo.addChunk(chunk);
                        }

                    }
                    fileList.add(fileInfo);
                    System.out.println("Add to FileList");
                }
                if (dIn.readByte() == -1) {
                    continue;
                }
            }

            if (option == 4) {

                List<String> filenames = fileList.stream().map((x) -> x.getFilename()).collect(Collectors.toList());
                Iterator<String> itr = filenames.iterator();
                while (itr.hasNext()) {
                    System.out.println(itr.next());
                }
                ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
                oos.writeObject(filenames);
                oos.flush();
            }
            if (option == 5) {
                String filename = dIn.readUTF();
                fInfo fileInfo = fileList.get(0);
                ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
                oos.writeObject(fileInfo);
                oos.flush();
            }
            break;

        }
        dOut.close();
        dIn.close();

    }

}