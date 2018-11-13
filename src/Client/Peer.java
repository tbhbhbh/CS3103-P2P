package Client;

import static Client.Client.DIRECTORY;
import static Client.Client.INPUT_DIRECTORY;
import static Client.Client.OUTPUT_DIRECTORY;
import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.SocketTimeoutException;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Semaphore;

import Commons.Packet;
import Commons.QueryDirPacket;
import Commons.QueryFilePacket;
import Commons.RegisterPacket;
import Commons.RequestPacket;
import Commons.Stun;
import Commons.UpdatePacket;
import Commons.FileInfo;
import Commons.ChunkInfo;


public class Peer {

    private final String CHUNK_DIRECTORY = OUTPUT_DIRECTORY + "chunks/";
    private final String GOOGLE_STUN_1 = "74.125.200.127";
    private final String GOOGLE_STUN_2 = "108.177.98.127";
    private final int BUFFER_SIZE = 1500;
    private FileInfo FileInfo;
    private int port;
    private int numChunks;
    Semaphore sem;

    public ServerSocket serverSocket;
    public DatagramSocket dataSocket;
    InetSocketAddress holePunchedIP;

    LinkedList<DatagramPacket> mq;
    public static boolean isHolePunched = false;

    public Peer() {
        this.port = generatePort();
        initDirectories();
        mq = new LinkedList<>();
        sem = new Semaphore(1);
    }

    private void initDirectories() {
        new File(DIRECTORY).mkdir();
        new File(INPUT_DIRECTORY).mkdir();
        new File(OUTPUT_DIRECTORY).mkdir();
        new File(CHUNK_DIRECTORY).mkdir();
    }

    // Randomly generates a port number for client to use for their socket.
    private int generatePort() {
        Random r = new Random();
        int rdmPort = r.nextInt(9000 - 8100) + 8100;
        while (rdmPort == port) {
            rdmPort = r.nextInt(9000 - 8100) + 8100;
        }
        return rdmPort;
    }

    // Send Heartbeat to Tracker to ensure the TCP connection is alive
    public void heartbeat(ObjectInputStream ois, ObjectOutputStream oos) {
        new Thread() {
            public void run(){
                while (true) {
                    try {
                        Thread.sleep(1000);
                        Packet heartbeatPacket = new Packet(7,0);
                        sem.acquire();
                        oos.writeObject(heartbeatPacket);
                        oos.flush();
                        Object obj = ois.readObject();
                        sem.release();
                        Packet replyPkt = (Packet)obj;
                        if (replyPkt.getCode() == 2) {
                            String message = (String) replyPkt.getPayload();
                            String[] hosts = message.split(";");
                            for (String host : hosts) {
                                System.out.println("HolePunching for "+host);
                                String ip = host.split(":")[0];
                                int port = Integer.parseInt(host.split(":")[1]);
                                DatagramPacket dp = new DatagramPacket(new byte[BUFFER_SIZE], BUFFER_SIZE, InetAddress.getByName(ip), port);
                                mq.add(dp);
//                                System.out.println("Add Packets");
                            }
                        }
                    } catch (Exception e) {
                        System.out.println("Error occured. Closing Socket.");
                        try {
                            ois.close();
                            oos.close();
                        } catch (IOException ioe) {
                            System.out.println("Socket already closed!");
                        }
                        System.exit(-1);
                    }
                }
            }
        }.start();
    }

    // Peer updates track of the availablity of file
    public void updateServer(ObjectInputStream ois, ObjectOutputStream oos, String fileName) throws Exception {
        final long sourceSize;
        try {
             sourceSize = Files.size(Paths.get(INPUT_DIRECTORY + fileName));
        } catch (IOException ioe) {
            System.out.println("Cannot find File!");
            return;
        }
        final long bytesPerSplit = 1024L; //1 chunk = 1024 bytes
        final int numChunks = (int) Math.ceil(sourceSize / bytesPerSplit);
        final long remainingBytes = sourceSize % bytesPerSplit;
        int position = 0;
        int index = 0;

        //create a folder with filename
        String foldername = "c" + fileName;
        System.out.println(String.format("Create new directory for file ... - %s", new File(INPUT_DIRECTORY + foldername).mkdirs()));


        //split file
        try (RandomAccessFile sourceFile = new RandomAccessFile(INPUT_DIRECTORY + fileName, "r");
             FileChannel sourceChannel = sourceFile.getChannel()) {
            for (; position < numChunks; position++) {
                Path filePart = Paths.get(INPUT_DIRECTORY + foldername + "/" + fileName + index);
                try (RandomAccessFile toFile = new RandomAccessFile(filePart.toFile(), "rw");
                     FileChannel toChannel = toFile.getChannel()) {
                    sourceChannel.position(position * bytesPerSplit);
                    toChannel.transferFrom(sourceChannel, 0, bytesPerSplit);

                }
                index++;
            }

            if (remainingBytes != 0) {
                Path filePart = Paths.get(INPUT_DIRECTORY + foldername + "/" + fileName + index);
                try (RandomAccessFile toFile = new RandomAccessFile(filePart.toFile(), "rw");
                     FileChannel toChannel = toFile.getChannel()) {
                    sourceChannel.position(position * bytesPerSplit);
                    toChannel.transferFrom(sourceChannel, 0, remainingBytes);

                }
            }
        } catch (FileNotFoundException fnfe) {
            System.out.println("File not Found!");
            return;
        } catch (IOException ioe) {
            System.out.println("Error reading the file!");
            return;
        }

        LinkedList<String> hashes = md5ForFile(INPUT_DIRECTORY+foldername+'/'+fileName, numChunks);

        UpdatePacket upPacket = new UpdatePacket(fileName, numChunks, hashes);
        sem.acquire();
        oos.writeObject(upPacket);
        oos.flush();
        sem.release();
    }

    // register peer with tracker with it's ip&port after STUN
    public void registerPeer(ObjectInputStream ois, ObjectOutputStream oos) throws Exception {
        System.out.println(String.format("Registering peer @ %s:%d", holePunchedIP.getAddress(), holePunchedIP.getPort()));
        RegisterPacket regPacket = new RegisterPacket(holePunchedIP);
        sem.acquire();
        oos.writeObject(regPacket);
        oos.flush();
        sem.release();
    }

    // Generate a list of MD5 Hashes for inclusion in FileInfo
    private LinkedList<String> md5ForFile(String filepath, int numChunks) throws Exception {
        LinkedList<String> hashes = new LinkedList<>();
        MessageDigest md = MessageDigest.getInstance("MD5");
        for (int index = 0; index < numChunks ; index++) {
            byte[] fileBytes = Files.readAllBytes(Paths.get(filepath+index));
            MessageDigest mdParts = MessageDigest.getInstance("MD5");
            hashes.addLast(byteToHex(mdParts.digest(fileBytes)));
            md.update(fileBytes);
        }
        hashes.addLast(byteToHex(md.digest()));
        return hashes;
    }

    // Generate MD5 Checksum
    private String generateMD5(Path filename) throws Exception{
        MessageDigest md = MessageDigest.getInstance("MD5");
        FileInputStream fis = new FileInputStream(filename.toFile());
        byte[] buffer = new byte[BUFFER_SIZE];
        int read = 0;
        while( ( read = fis.read( buffer ) ) > 0 ){
           md.update(buffer,0,read);
        }
        fis.close();
        byte[] digest = md.digest();

        return byteToHex(digest);
    }

    // Conversion from bytes to hex string
    private String byteToHex(byte[] digest) {
        StringBuffer hexString = new StringBuffer();
        for (int i = 0; i < digest.length; i++) {
            if ((0xff & digest[i]) < 0x10) {
                hexString.append("0"
                        + Integer.toHexString((0xFF & digest[i])));
            } else {
                hexString.append(Integer.toHexString(0xFF & digest[i]));
            }
        }
        return hexString.toString();
    }
    // Open UDP Socket so that other Peer can connect to and download files. Do Hole-punching for Peers if needed.
    public void server() {
        try {
            serverSocket = new ServerSocket(port);
            dataSocket = new DatagramSocket(new InetSocketAddress("0.0.0.0", port));
            dataSocket.setSoTimeout(10000);
//            holePunchedIP = new InetSocketAddress(dataSocket.getLocalAddress(), port);
            holePunchedIP = Stun.holePunch(dataSocket, GOOGLE_STUN_2);
            // check if under Symmetric NAT
            InetSocketAddress secPunchedIP = Stun.holePunch(dataSocket,GOOGLE_STUN_1);
            if (holePunchedIP.getPort() != secPunchedIP.getPort()) {
                System.out.println("Symmetric NAT concluded based on PORT. Closing Program.");
                System.exit(1);
                // handle symmetric nat
            }
            if (!holePunchedIP.getHostString().equals(secPunchedIP.getHostString())) {
                System.out.println("Symmetric NAT concluded based on IP. Closeing Program.");
                System.exit(1);
                // handle symmetric nat
            }
            System.out.println(String.format("Peer serving %d", port));
            isHolePunched = true;
        } catch (Exception e) {
            System.out.println(e);
            return;
        }
        while (true) {
            try {
                DatagramPacket dataPkt = new DatagramPacket(new byte[BUFFER_SIZE], BUFFER_SIZE);
                dataSocket.receive(dataPkt);
                byte[] buffer = dataPkt.getData();

                ByteArrayInputStream bais = new ByteArrayInputStream(buffer);
                ObjectInputStream ois = new ObjectInputStream(bais);
                Object readObject = ois.readObject();
                System.out.println("Received Object!");
                if (readObject instanceof RequestPacket) {
                    RequestPacket<ArrayList<String>> request = (RequestPacket) readObject;
                    ArrayList<String> params = request.getPayload();
                    String filename = params.get(0);
                    int chunkID = Integer.parseInt(params.get(1));
                    File f = new File(INPUT_DIRECTORY + "c" + filename + "/" + filename + chunkID);
                    FileInputStream fis = new FileInputStream(f);
                    byte[] fileData = fis.readAllBytes();
                    fis.close();
                    RequestPacket<byte[]> response = new RequestPacket<>(1, fileData);
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    ObjectOutputStream oos = new ObjectOutputStream(baos);
                    oos.writeObject(response);
                    byte[] data = baos.toByteArray();

                    //Send data
                    dataSocket.send(new DatagramPacket(data, data.length, dataPkt.getSocketAddress()));
                    System.out.println(String.format("Sent %d Datagram of size: %d", chunkID, data.length));
                    System.out.println(String.format("Sent to %s:%d", dataPkt.getAddress(), dataPkt.getPort()));
                } else {
                    System.out.println("The received object is not of type RequestPacket!");
                }
            } catch (SocketTimeoutException ste) {
                // Send HeartBeat(UDP)
//                System.out.println(String.format("Timeout.. send Heartbeat MQ.isEmpty-%s", mq.isEmpty()));
                while(!mq.isEmpty()) {
                    try {
                        DatagramPacket dp = mq.poll();
                        System.out.println(String.format("Hole Punch %s:%d",dp.getAddress().getHostAddress(), dp.getPort() ));
                        Packet heartbeatPkt = new Packet(7,0);
                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        ObjectOutputStream oos = new ObjectOutputStream(baos);
                        oos.writeObject(heartbeatPkt);
                        oos.flush();

                        byte[] data = baos.toByteArray();
                        dataSocket.send(new DatagramPacket(data, data.length,dp.getSocketAddress()));

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                try {
                    InetSocketAddress reStunIP = Stun.holePunch(dataSocket, GOOGLE_STUN_1);
                    if (reStunIP.getPort() != holePunchedIP.getPort()) {
                        System.out.println("Port Have Changed! Closing Program.");
                        // Close Program when port changed ...
                        dataSocket.close();
                        System.exit(1);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            catch (Exception e) {
                System.out.println(e);
                System.out.println("No object could be read from the received UDP datagram.");
            }
        }
    }

    // Downloading of file from another Peer
    public void download(ObjectInputStream ois, ObjectOutputStream oos) throws Exception {

        if (FileInfo == null) {
            System.out.println("Request file from server first!");
            return;
        }
        String filename = FileInfo.getFilename();
        Path directory = Files.createDirectories(Paths.get(CHUNK_DIRECTORY, filename));

        // download chunks from peers
        LinkedList<String> hashes = new LinkedList<>();
        boolean isValidFile;
        do {
            hashes = new LinkedList<>();
            numChunks = FileInfo.getNumOfChunks();
            System.out.println(String.format("Total Number of chunks to download. %d", numChunks));
            // Single Thread for now...
            for (int i = 0; i < numChunks; i++) {
                ChunkInfo chunk = FileInfo.getChunk(i);
                hashes.add(chunk.getChecksum());
                int chunkID = chunk.getChunkID();
                boolean isCompleted;
                do {
                    InetSocketAddress peerSocket = chunk.getRdmPeer();

                    int ownPort = generatePort();
                    Packet downloadRequestPacket = new Packet(6, 0,
                            peerSocket.getAddress().getHostAddress() + ":" + ownPort);
                    sem.acquire();
                    oos.writeObject(downloadRequestPacket);
                    oos.flush();
                    sem.release();

                    InetAddress peerAddress = peerSocket.getAddress();
                    int peerPort = peerSocket.getPort();
                    isCompleted = downloadFromPeer(directory, peerAddress,
                            ownPort, peerPort, filename, chunkID, chunk.getChecksum());
                    if (!isCompleted) {
                        System.out.println("Redownloading Chunk with other peer!");
                    }
                } while (!isCompleted);
                System.out.println(String.format("%d/%d", i + 1, numChunks));
            }

            System.out.println(String.format("Finish downloading all the chunks of %s", filename));
            System.out.println("Combining all the chunks together...");

            // merge chunks together
            FileOutputStream fos = new FileOutputStream(OUTPUT_DIRECTORY + filename);
            DataOutputStream dos = new DataOutputStream(new BufferedOutputStream(fos));

            byte[] buffer = new byte[BUFFER_SIZE];
            for (int i = 0; i < numChunks; i++) {
                FileInputStream fis = new FileInputStream(new File(directory.toString(), String.valueOf(i)));
                int byteRead;
                while (true) {
                    byteRead = fis.read(buffer);
                    if (byteRead == -1)
                        break;
                    dos.write(buffer, 0, byteRead);
                    dos.flush();
                }
                fis.close();
            }
            hashes.addLast(FileInfo.getChecksum());
            dos.close();
            isValidFile = generateMD5(Paths.get(OUTPUT_DIRECTORY, filename)).equals(FileInfo.getChecksum());
            System.out.println(String.format("Hash for file - %s", isValidFile));
            System.out.println(String.format("%s successfully combined from its chunks", filename));
        } while (!isValidFile); // Download till it is valid

        // Update server that the chunk is available for download by other peers
        UpdatePacket upPacket = new UpdatePacket(filename, numChunks, hashes);
        sem.acquire();
        oos.writeObject(upPacket);
        oos.flush();
        sem.release();
    }

    // Open UDP socket to download chunk
    public boolean downloadFromPeer(Path directory, InetAddress peerAddress, int ownPort, int port, String fileName, int i, String checksum) throws Exception {
        DatagramSocket dSock = new DatagramSocket(ownPort);
        dSock.connect(peerAddress,port);
        dSock.setSoTimeout(3000);

        ArrayList<String> params = new ArrayList<>();
        params.add(fileName);
        params.add(String.valueOf(i));
        RequestPacket<ArrayList<String>> requestPacket = new RequestPacket<>(0, params);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(requestPacket);
        oos.flush();
        byte[] data = baos.toByteArray();
        dSock.send(new DatagramPacket(data, data.length));
        int ttl = 10;

        byte[] buffer = new byte[BUFFER_SIZE];
        while (true) {
            ttl--;
            DatagramPacket dataPkt = new DatagramPacket(buffer, BUFFER_SIZE, peerAddress, port);
            try {
                System.out.println(String.format("Waiting at %s:%s... Sending to %s:%s",
                        dSock.getLocalAddress(), dSock.getLocalPort(), peerAddress, port));
                dSock.receive(dataPkt);

            } catch (SocketTimeoutException ste) {
                // Retry 10 times before give up
                if (ttl == 0) {
                    break;
                }
                System.out.println("Timeout waiting for packet.. Retry..");
                dSock.send(new DatagramPacket(data, data.length));
                continue;
            }
            try {
                ByteArrayInputStream bais = new ByteArrayInputStream(buffer);
                ObjectInputStream ois = new ObjectInputStream(bais);
                Object readObject = ois.readObject();
                if (readObject instanceof RequestPacket) {
                    RequestPacket<byte[]> response = (RequestPacket<byte[]>) readObject;
                    byte[] payload = response.getPayload();

                    File f = new File(directory.toString(), String.valueOf(i));
                    DataOutputStream dosFile = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(f)));
                    dosFile.write(payload);
                    dosFile.flush();
                    dosFile.close();
                    System.out.println(String.format("Downloaded Chunk %d of %s", i, fileName));

                    boolean isValidFile = generateMD5(Paths.get(directory.toString(), String.valueOf(i))).equals(checksum);
                    System.out.println(String.format("Chunk matches Hash - %s", isValidFile));
                    // If Chunk downloaded is corrupted, re-download from same peer.
                    if (!isValidFile) {
                        dSock.send(new DatagramPacket(data, data.length));
                    } else {
                      break;
                    }
                } else {
                    System.out.println("The received object is not of type RequestPacket!");
                }
            } catch (Exception e) {
                System.out.println(e);
                System.out.println("No object could be read from the received UDP datagram.");
            }
        }
        dSock.close();
        return (ttl > 0);
    }

    // Ask Tracker to give the listing of files available to download
    public void getDir(ObjectInputStream ois, ObjectOutputStream oos) throws Exception {
        QueryDirPacket queryDirPacket = new QueryDirPacket();
        sem.acquire();
        oos.writeObject(queryDirPacket);
        oos.flush();

        Object obj = ois.readObject();
        sem.release();
        QueryDirPacket replyPkt = (QueryDirPacket) obj;
        Object payload = replyPkt.getPayload();
        List<String> files = (List<String>) payload;
        if (files.isEmpty()) {
            System.out.println("No File Info available in Tracker!");
        } else {
            Iterator<String> fileListing = files.iterator();
            while (fileListing.hasNext()) {
                System.out.println(fileListing.next());
            }
        }

    }

    // Ask Tracker for FileInfo of file
    public void getFile(ObjectInputStream ois, ObjectOutputStream oos, String filename) throws Exception {
        QueryFilePacket queryDirPacket = new QueryFilePacket(filename);
        sem.acquire();
        oos.writeObject(queryDirPacket);
        oos.flush();
        Object obj = ois.readObject();
        sem.release();
        QueryFilePacket replyPkt = (QueryFilePacket) obj;
        Object payload = replyPkt.getPayload();
        FileInfo fileInfo = (FileInfo) payload;
        this.FileInfo = fileInfo;
        if (fileInfo == null) {
            System.out.println("No Such File!");
        } else {
            System.out.println("Fetched File Info!");
        }

    }

    // Tell Tracker that Peer is leaving.
    public void shutdown(ObjectInputStream ois, ObjectOutputStream oos) throws Exception {
        Packet pkt = new Packet(5, 0);
        sem.acquire();
        oos.writeObject(pkt);
        oos.flush();
        sem.release();
    }


}
