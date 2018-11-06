package Tracker;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import Commons.Packet;
import Commons.QueryDirPacket;
import Commons.QueryFilePacket;
import Commons.RegisterPacket;
import Commons.UpdatePacket;
import Commons.ChunkInfo;
import Commons.FileInfo;

public class Server {

    private static final Logger LOGGER = Logger.getLogger( Server.class.getName() );
    private final int LISTENING_PORT = 8080;
    private ServerSocket serverSocket;

    private ConcurrentHashMap<String, FileInfo> fileList;
    private ConcurrentHashMap<String, String> messages;

    public static void main(String[] args) throws Exception {
        LOGGER.info("Starting P2P server");
        new Server();
    }


    public Server() throws Exception {
        fileList = new ConcurrentHashMap<>();
        messages = new ConcurrentHashMap<>();
        LOGGER.info("Init server on port " + LISTENING_PORT + "");

        //try to obtain server socket
        try {
            ServerSocket serverSocket = new ServerSocket(LISTENING_PORT);

            while (true) {
                LOGGER.info("Waiting for peer");
                Socket clientSocket = serverSocket.accept();
                LOGGER.info(String.format("Client received %s:%d", clientSocket.getInetAddress(), clientSocket.getPort()));
                Thread t = new Thread() {
                    public void run() {
                        try {
                            handleClientSocket(clientSocket);
                        } catch (Exception e) {
                            LOGGER.warning(e.getMessage());
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

    public void udpHeartBeatServer() throws Exception {
        DatagramSocket dSock = new DatagramSocket(LISTENING_PORT);
        while (true) {
            LOGGER.info("Echo UDP..");
            DatagramPacket dPkt = new DatagramPacket(new byte[1500], 1500);
            dSock.receive(dPkt);
            DatagramPacket echoPkt = new DatagramPacket(dPkt.getData(), dPkt.getData().length, dPkt.getSocketAddress());
            dSock.send(echoPkt);
        }
    }

    private void handleClientSocket(Socket socket) throws Exception {
        LOGGER.info("Client connected\n");
        ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
        ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());
        InetSocketAddress clientAddress = null;
        while (true) {
            Object obj = ois.readObject();
            Packet pkt = (Packet) obj;
            if (pkt.getType() == 0) { // Register packet
                LOGGER.info("Register to Tracker");
                RegisterPacket regPkt = (RegisterPacket) pkt;
//                String clientIP = socket.getInetAddress().getHostAddress();
                int clientPort = regPkt.getPort();
                InetAddress clientIP = socket.getInetAddress();
                clientAddress = new InetSocketAddress(clientIP, clientPort);
                LOGGER.info(String.format("Client Address: %s :%d", clientIP, clientPort));
            }
            if (pkt.getType() == 4) {// Update Packet
                LOGGER.info("Update Availability");
                if (clientAddress == null) {
                    continue;
                }
                UpdatePacket upPkt = (UpdatePacket) pkt;
                String filename = upPkt.getFilename();
                int numChunks = upPkt.getChunks();
                LinkedList<String> checksums = upPkt.getChecksums();
                FileInfo fileInfo;
                if (!fileList.containsKey(filename)) {
                    LOGGER.info("creating new file entry: %s".format(filename));
                    fileInfo = new FileInfo(filename);
                    for (int i = 0; i < numChunks; i++) {
                        ChunkInfo chunk = new ChunkInfo(i);
                        chunk.setChecksum(checksums.removeFirst());
                        chunk.addPeer(clientAddress);
                        fileInfo.addChunk(chunk);
                    }
                    fileInfo.setChecksum(checksums.getLast());
                } else {
                    fileInfo = fileList.get(filename);
                    fileInfo.addPeer(clientAddress);
                }
                fileList.put(filename, fileInfo);
                LOGGER.info(String.format("%s:%d :Add to FileList", clientAddress.getAddress(), clientAddress.getPort()));
            }
            if (pkt.getType() == 1) { // Query Packet (Directory)
                LOGGER.info("List Directory");
                List<String> filenames = fileList.keySet().stream().collect(Collectors.toList());
                QueryDirPacket responsePkt = new QueryDirPacket(filenames);
                oos.writeObject(responsePkt);
                oos.flush();
            }

            if (pkt.getType() == 2) { // Query Packet (File)
                QueryFilePacket queryPkt = (QueryFilePacket) pkt;
                String filename = queryPkt.getFilename();
                LOGGER.info(String.format("Request for file: %s", filename));
                FileInfo fileInfo = fileList.get(filename);
                QueryFilePacket responsePkt = new QueryFilePacket(fileInfo);
                oos.writeObject(responsePkt);
                oos.flush();
            }

            if (pkt.getType() == 5) { // Shutdown
                // Remove records...
                LOGGER.info("shutdown peer");
                for (FileInfo fileInfo : fileList.values()) {
                    fileInfo.removePeer(clientAddress);
                }
                break;
            }

            if (pkt.getType() == 6) { // DownloadRequest
                LOGGER.info("DOWNLOAD REQ");
                String x = (String) pkt.getPayload();
                String y = "";
                if (messages.containsKey(x)) {
                    y = ";" + messages.get(x);
                }
                y += socket.getInetAddress().getHostAddress();
                messages.put(x,y);
            }

            if (pkt.getType() == 7) { // Heartbeat
                LOGGER.info("HeartBeat");
                // check if have message
                Packet packet ;
                if (!messages.containsKey(socket.getInetAddress().getHostAddress())) {
                    packet = new Packet(7,1);
                } else {
                    packet= new Packet(7,2, messages.get(socket.getInetAddress().getHostAddress()));
                }
                oos.writeObject(packet);
                oos.flush();
            }
        }
        ois.close();
        oos.close();
        socket.close();
    }
}
