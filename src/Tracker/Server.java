package Tracker;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.reflect.Array;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Hashtable;

public class Server {

    /*a
    * 1) initial announcement
    * peerIP
    * peerPort
    * fileName
    * numChunks
    *
     */
    private static Hashtable<String, ArrayList<String>> fileNameToChunksIndex;
    private static Hashtable<String, ArrayList<String>> chunksToPeerIndex;
    private static ArrayList<Integer> peerIPList;
    private static ArrayList<Integer> peerPort;

    private final int LISTENING_PORT = 8080;
    private static int CHUNK_ID = 0;
    private ServerSocket serverSocket;
    private Socket clientSocket;

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
      
        try {
            DataInputStream dIn = new DataInputStream(clientSocket.getInputStream());
            DataOutputStream dOut = new DataOutputStream(clientSocket.getOutputStream());

            byte option = dIn.readByte();
            boolean isEndOfData = false;

            switch (option) {
                //initial announcement - register a file
                case 0:

                    String fileName = null;
                    String peerIP = null;
                    int numChunks = 0, peerPort = 0;
                    ArrayList<String> chunkList;

                    while(!isEndOfData) {
                       byte message = dIn.readByte();
                       switch (message) {
                           case 1: //file name
                               fileName = dIn.readUTF();
                           case 2: //num chunks
                               numChunks = dIn.readInt();
                           case 3: //peer ip
                               peerIP = dIn.readUTF();
                           case 4: //peer port
                               peerPort = dIn.readInt();
                           default:
                               isEndOfData = true;
                       }
                    }

                    registerFile(fileName, numChunks, peerIP, peerPort);
                    break;

                // register a chunk
                // TODO: registering chunk
                case 1:

                // TODO: peer/client requesting for file
                case 2:
            }
        } catch (IOException e) {
            System.out.println(e);
        }
    }

    /*
        Handles initial announcement when a peer registers.
     */
    public static void registerFile(String fileName, int numChunks,
                                    String peerIP, int peerPort) {

        if (!fileNameToChunksIndex.containsKey(fileName)) {
            ArrayList<String> chunkNameList = new ArrayList<>();

            for (int i = 0; i < numChunks; i++) {
                String chunkName = fileName + String.valueOf(i);
                chunkNameList.add(chunkName);
            }
            fileNameToChunksIndex.put(fileName, chunkNameList);
        }
    }

    /*
        Handles chunk announcement when an existing peer finishes downloading a chunk.
     */
    public static void registerChunk(String fileName, int numChunks,
                                     String peerIP, int peerPort) {
        //TODO: implement chunk registering

    }
}