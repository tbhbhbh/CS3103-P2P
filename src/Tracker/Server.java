package Tracker;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.reflect.Array;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.Map;

public class Server {

    /*
    * 1) initial announcement
    * peerIP
    * peerPort
    * fileName
    * numChunks
    *
     */
    private static Hashtable<String, ArrayList<String>> fileNameToChunksIndex;
    private static Hashtable<String, ArrayList<InetSocketAddress>> chunksToPeerIndex;
    private static ArrayList<Integer> peerIPList;
    private static ArrayList<Integer> peerPort;

    private final int LISTENING_PORT = 8080;
    private static int CHUNK_ID = 0;
    private ServerSocket serverSocket;
    private Socket clientSocket;

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
                clientSocket = serverSocket.accept();

            }
        } catch (IOException e) {
            System.out.println(e);
        }

        try {
            DataInputStream dIn = new DataInputStream(clientSocket.getInputStream());
            DataOutputStream dOut = new DataOutputStream(clientSocket.getOutputStream());

            byte option = dIn.readByte();
            boolean isEndOfData = false;
            System.out.println(option);

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

                // register a chunk
                // TODO: registering chunk
                case 1:
                    listDirectory();

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

    public static void listDirectory() {
        for (Map.Entry<String, ArrayList<String>> fileNameToChunksIndex:
                fileNameToChunksIndex.entrySet()) {
            System.out.println("File name: " + fileNameToChunksIndex.getKey());
            System.out.println("Number of chunks: " + fileNameToChunksIndex.getValue().size());

            ArrayList<String> chunkIDList = fileNameToChunksIndex.getValue();
            for (int i = 0; i < chunkIDList.size(); i++) {
                String chunkID = chunkIDList.get(i);
                ArrayList<InetSocketAddress> peerList = chunksToPeerIndex.get(chunkID);
                for (int j = 0; j < peerList.size(); j++) {
                    if (j == 1) {
                        char[] whiteSpaces = new char[chunkID.length()];
                        Arrays.fill(whiteSpaces, ' ');
                        chunkID = new String(whiteSpaces);
                    }
                    System.out.println(chunkID + " - " + peerList.get(j).getHostName());
                }
            }

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