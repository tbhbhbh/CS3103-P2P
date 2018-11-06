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
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

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
    private final int BUFFER_SIZE = 1500;
    private FileInfo FileInfo;
    private int peerId;
    private int port;
    private int numChunks;

    public ServerSocket serverSocket;
    public DatagramSocket dataSocket;
    InetSocketAddress holePunchedIP;

    public Peer() {
        this.port = generatePort();
        initDirectories();
    }

    private void initDirectories() {
        new File(DIRECTORY).mkdir();
        new File(INPUT_DIRECTORY).mkdir();
        new File(OUTPUT_DIRECTORY).mkdir();
        new File(CHUNK_DIRECTORY).mkdir();
    }

    /*
     * Randomly generates a port number for client to use for their socket.
     */
    private int generatePort() {
        Random r = new Random();
        return r.nextInt(9000 - 8100) + 8100;
    }


    /*
     * Peer updates server of a file
     *
     */
    public void updateServer(ObjectInputStream ois, ObjectOutputStream oos, String fileName) throws IOException {

        final long sourceSize = Files.size(Paths.get(INPUT_DIRECTORY + fileName));
        final long bytesPerSplit = 1024L; //1 chunk = 1024 bytes
        final int numChunks = (int) (sourceSize / bytesPerSplit);
        final long remainingBytes = sourceSize % bytesPerSplit;
        int position = 0;
        int index = 0;

        //create a folder with filename
        String foldername = "c" + fileName;
        System.out.println(new File(INPUT_DIRECTORY + foldername).mkdirs());


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
        }


        System.out.println(String.format("Registering peer @ %s:%d", holePunchedIP.getAddress(), holePunchedIP.getPort()));
        RegisterPacket regPacket = new RegisterPacket(holePunchedIP);
        oos.writeObject(regPacket);
        oos.flush();

        UpdatePacket upPacket = new UpdatePacket(fileName, numChunks);
        oos.writeObject(upPacket);
        oos.flush();
    }

    //Uploading
    public void server() throws Exception {
        try {
            serverSocket = new ServerSocket(port);
            holePunchedIP = Stun.holePunch(port);
            dataSocket = new DatagramSocket(port);
            System.out.println(String.format("Peer serving %d", port));
        } catch (Exception e) {
            System.out.println(e);
            return;
        }
        while (true) {
            DatagramPacket dataPkt = new DatagramPacket(new byte[BUFFER_SIZE], BUFFER_SIZE);
            dataSocket.receive(dataPkt);
            byte[] buffer = dataPkt.getData();

            ByteArrayInputStream bais = new ByteArrayInputStream(buffer);
            ObjectInputStream ois = new ObjectInputStream(bais);
//            try {
                Object readObject = ois.readObject();
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
//            } catch (Exception e) {
//                System.out.println(e);
//                System.out.println("No object could be read from the received UDP datagram.");
//            }
        }
        // TCP VERSION (WORKING)
//        while (true) {
//            Socket socket = serverSocket.accept();
//            System.out.println("Accepted connection from peer\n");
//
//            ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());
//            ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
//
//            Object obj = ois.readObject();
//            RequestPacket<ArrayList<String>> request = (RequestPacket<ArrayList<String>>) obj;
//            ArrayList<String> params = request.getPayload();
//            String filename = params.get(0);
//            int chunkID = Integer.parseInt(params.get(1));
//            File f = new File(INPUT_DIRECTORY + "c" + filename + "/" + filename + chunkID);
//            FileInputStream fis = new FileInputStream(f);
//            byte[] data = fis.readAllBytes();
//            fis.close();
//            RequestPacket<byte[]> response = new RequestPacket<>(1, data);
//            oos.writeObject(response);
//            socket.close();
//        }
    }

    // Downloading
    public void download(ObjectInputStream ois, ObjectOutputStream oos) throws Exception {

        if (FileInfo == null) {
            System.out.println("Request file from server first!");
            return;
        }
        String filename = FileInfo.getFilename();
        Path directory = Files.createDirectories(Paths.get(CHUNK_DIRECTORY, filename));

        // download chunks from peers

        numChunks = FileInfo.getNumOfChunks();
        System.out.println(String.format("Total Number of chunks to download. %d", numChunks));
        // Single Thread for now...
        for (int i = 0; i < numChunks; i++) {
            ChunkInfo chunk = FileInfo.getChunk(i);
            int chunkID = chunk.getChunkID();
            InetSocketAddress peerSocket = chunk.getRdmPeer();
            InetAddress peerAddress = peerSocket.getAddress();
            int peerPort = peerSocket.getPort();
            downloadFromPeer(directory, peerAddress, peerPort, filename, chunkID);
            System.out.println(String.format("%d/%d", i,numChunks));
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
        dos.close();
        System.out.println(String.format("%s successfully combined from its chunks", filename));

        UpdatePacket upPacket = new UpdatePacket(filename, numChunks);
        oos.writeObject(upPacket);
        oos.flush();
        // TODO: checksum

    }

    public void downloadFromPeer(Path directory, InetAddress peerAddress, int port, String fileName, int i) throws IOException, ClassNotFoundException {
        DatagramSocket dSock = new DatagramSocket();
        dSock.connect(peerAddress,port);
        dSock.setSoTimeout(10000);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ArrayList<String> params = new ArrayList<>();
        params.add(fileName);
        params.add(String.valueOf(i));
        RequestPacket<ArrayList<String>> requestPacket = new RequestPacket<>(0, params);
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(requestPacket);
        oos.flush();
        byte[] data = baos.toByteArray();
        dSock.send(new DatagramPacket(data, data.length));

        byte[] buffer = new byte[BUFFER_SIZE];
        while(true) {
            DatagramPacket dataPkt = new DatagramPacket(buffer, BUFFER_SIZE, peerAddress, port);
            try {
                System.out.println(String.format("Waiting at %s:%s", dSock.getLocalAddress(), dSock.getLocalPort()));
                dSock.receive(dataPkt);
                break;
            } catch (SocketTimeoutException ste) {
                dSock = new DatagramSocket();
                dSock.connect(peerAddress,port);
                dSock.setSoTimeout(10000);
                buffer = new byte[BUFFER_SIZE];
                System.out.println(String.format("Timeout... resend request using new port to %s:%d",dSock.getInetAddress(), dSock.getPort()));
                dSock.send(new DatagramPacket(data, data.length));
                continue;
            }
        }
            System.out.println(buffer.length);
            ByteArrayInputStream bais = new ByteArrayInputStream(buffer);
            ObjectInputStream ois = new ObjectInputStream(bais);
//        try {
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
            } else {
                System.out.println("The received object is not of type RequestPacket!");
            }
            dSock.close();
//        } catch (Exception e) {
//            System.out.println(e);
//            System.out.println("No object could be read from the received UDP datagram.");
//        }

// TCP VERSION (WORKING)
//        // connect to peer
//        Socket socket = new Socket(peerAddress, port);
//
//        // send chunk request
//        ArrayList<String> params = new ArrayList<>();
//        params.add(fileName);
//        params.add(String.valueOf(i));
//        RequestPacket<ArrayList<String>> requestPacket = new RequestPacket<>(0, params);
//        ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
//        oos.writeObject(requestPacket);
//        oos.flush();
//
//        ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());
//        RequestPacket<byte[]> response = (RequestPacket<byte[]>) ois.readObject();
//        byte[] data = response.getPayload();
//
//        File f = new File(directory.toString(), String.valueOf(i));
//        DataOutputStream dosFile = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(f)));
//        dosFile.write(data);
//        dosFile.flush();
//        dosFile.close();
//        System.out.println(String.format("Downloaded Chunk %d of %s", i, fileName));
//        ois.close();
//        oos.close();
//        socket.close();


    }

    public void getDir(ObjectInputStream ois, ObjectOutputStream oos) throws Exception {
        QueryDirPacket queryDirPacket = new QueryDirPacket();
        oos.writeObject(queryDirPacket);
        oos.flush();

        Object obj = ois.readObject();
        QueryDirPacket replyPkt = (QueryDirPacket) obj;
        Object payload = replyPkt.getPayload();
        Iterator<String> fileListing = ((List<String>) payload).iterator();
        while (fileListing.hasNext()) {
            System.out.println(fileListing.next());
        }

    }

    public void getFile(ObjectInputStream ois, ObjectOutputStream oos, String filename) throws Exception {
        QueryFilePacket queryDirPacket = new QueryFilePacket(filename);
        oos.writeObject(queryDirPacket);
        oos.flush();
        Object obj = ois.readObject();
        QueryFilePacket replyPkt = (QueryFilePacket) obj;
        Object payload = replyPkt.getPayload();
        FileInfo fileInfo = (FileInfo) payload;
        this.FileInfo = fileInfo;
        System.out.println("Fetched File Info!");

    }

    public void shutdown(ObjectInputStream ois, ObjectOutputStream oos) throws Exception {
        Packet pkt = new Packet(5, 0);
        oos.writeObject(pkt);
        oos.flush();
    }


}
