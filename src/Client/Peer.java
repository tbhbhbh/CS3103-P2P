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

    public void heartbeat(ObjectInputStream ois, ObjectOutputStream oos) throws Exception {
        new Thread() {
            public void run(){
                while (true) {
                    try {
                        Thread.sleep(1000);
                        Packet heartbeatPacket = new Packet(7,0);
                        oos.writeObject(heartbeatPacket);
                        oos.flush();
                        Object obj = ois.readObject();
                        Packet replyPkt = (Packet)obj;
                        if (replyPkt.getCode() == 2) {
                            String message = (String) replyPkt.getPayload();
                            String[] hosts = message.split(";");
                            for (String host : hosts) {
                                System.out.println("HolePunching for THIS DUDE. "+host);
                                String ip = host.split(":")[0];
                                int port = Integer.parseInt(host.split(":")[1]);
                                DatagramPacket dp = new DatagramPacket(new byte[BUFFER_SIZE], BUFFER_SIZE, InetAddress.getByName(ip), port);
                                for (int i=0;i>10;i++){
                                    dataSocket.send(dp);
                                }
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }.start();
    }

    /*
     * Peer updates server of a file
     *
     */
    public void updateServer(ObjectInputStream ois, ObjectOutputStream oos, String fileName) throws Exception {

        final long sourceSize = Files.size(Paths.get(INPUT_DIRECTORY + fileName));
        final long bytesPerSplit = 1024L; //1 chunk = 1024 bytes
        final int numChunks = (int) Math.ceil(sourceSize / bytesPerSplit);
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

        LinkedList<String> hashes = md5ForFile(INPUT_DIRECTORY+foldername+'/'+fileName, numChunks);

        System.out.println(String.format("Registering peer @ %s:%d", holePunchedIP.getAddress(), holePunchedIP.getPort()));
        RegisterPacket regPacket = new RegisterPacket(holePunchedIP);
        oos.writeObject(regPacket);
        oos.flush();

        UpdatePacket upPacket = new UpdatePacket(fileName, numChunks, hashes);
        oos.writeObject(upPacket);
        oos.flush();
    }

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
    //Uploading
    public void server() throws Exception {
        try {
            serverSocket = new ServerSocket(port);
            dataSocket = new DatagramSocket(port);
//            holePunchedIP = new InetSocketAddress(dataSocket.getLocalAddress(), port);
            holePunchedIP = Stun.holePunch(dataSocket, "108.177.98.127");
            // check if under Symmetric NAT
            InetSocketAddress secPunchedIP = Stun.holePunch(dataSocket,"74.125.200.127");
            if (holePunchedIP.getPort() != secPunchedIP.getPort()) {
                System.out.println("Symmetric NAT concluded based on PORT");
                // handle symmetric nat
            }
            if (!holePunchedIP.getHostString().equals(secPunchedIP.getHostString())) {
                System.out.println("Symmetric NAT concluded based on IP");
                // handle symmetric nat
            }
//            new Thread(){
//                @Override
//                public void run() {
//                    dataSocket.send();
//                }
//            }.start();
            System.out.println(String.format("Peer serving %d", port));
        } catch (Exception e) {
            System.out.println(e);
            return;
        }
        while (true) {
            System.out.println(dataSocket.isConnected());
            DatagramPacket dataPkt = new DatagramPacket(new byte[BUFFER_SIZE], BUFFER_SIZE);
            dataSocket.receive(dataPkt);
            byte[] buffer = dataPkt.getData();

            ByteArrayInputStream bais = new ByteArrayInputStream(buffer);
            ObjectInputStream ois = new ObjectInputStream(bais);
//            try {
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
//            } catch (Exception e) {
//                System.out.println(e);
//                System.out.println("No object could be read from the received UDP datagram.");
//            }
        }
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

        LinkedList<String> hashes = new LinkedList<>();
        numChunks = FileInfo.getNumOfChunks();
        System.out.println(String.format("Total Number of chunks to download. %d", numChunks));
        // Single Thread for now...
        for (int i = 0; i < numChunks; i++) {
            ChunkInfo chunk = FileInfo.getChunk(i);
            hashes.add(chunk.getChecksum());
            int chunkID = chunk.getChunkID();
            InetSocketAddress peerSocket = chunk.getRdmPeer();

            int ownPort = generatePort();
            Packet downloadRequestPacket = new Packet(6,0,peerSocket.getAddress().getHostAddress()+":"+ownPort );
            oos.writeObject(downloadRequestPacket);
            oos.flush();

            InetAddress peerAddress = peerSocket.getAddress();
            int peerPort = peerSocket.getPort();
            downloadFromPeer(directory, peerAddress, ownPort, peerPort, filename, chunkID, chunk.getChecksum());
            System.out.println(String.format("%d/%d", i+1,numChunks));
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
        System.out.println(String.format("Hash for file - %s", generateMD5(Paths.get(OUTPUT_DIRECTORY,filename)).equals(FileInfo.getChecksum())));
        System.out.println(String.format("%s successfully combined from its chunks", filename));

        UpdatePacket upPacket = new UpdatePacket(filename, numChunks, hashes);
        oos.writeObject(upPacket);
        oos.flush();
        // TODO: checksum handling

    }

    public void downloadFromPeer(Path directory, InetAddress peerAddress, int ownPort, int port, String fileName, int i, String checksum) throws Exception {
        DatagramSocket dSock = new DatagramSocket(ownPort);
        dSock.connect(peerAddress,port);
        dSock.setSoTimeout(3000);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ArrayList<String> params = new ArrayList<>();
        params.add(fileName);
        params.add(String.valueOf(i));
        RequestPacket<ArrayList<String>> requestPacket = new RequestPacket<>(0, params);
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(requestPacket);
        oos.flush();
        byte[] data = baos.toByteArray();
        System.out.println(peerAddress);
        dSock.send(new DatagramPacket(data, data.length));

        byte[] buffer = new byte[BUFFER_SIZE];
        while(true) {
            DatagramPacket dataPkt = new DatagramPacket(buffer, BUFFER_SIZE, peerAddress, port);
            try {
                System.out.println(String.format("Waiting at %s:%s", dSock.getLocalAddress(), dSock.getLocalPort()));
                dSock.receive(dataPkt);
                break;
            } catch (SocketTimeoutException ste) {
//                dSock.connect(peerAddress,port);
//                dSock.setSoTimeout(10000);
//                buffer = new byte[BUFFER_SIZE];
//                System.out.println(String.format("Timeout... resend request using new port to %s:%d",dSock.getInetAddress(), dSock.getPort()));
                dSock.send(new DatagramPacket(data, data.length));
                continue;
            }
        }

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
        System.out.println(String.format("Chunk matches Hash - %s",
                generateMD5(Paths.get(directory.toString(),String.valueOf(i))).equals(checksum)));
        dSock.close();
//        } catch (Exception e) {
//            System.out.println(e);
//            System.out.println("No object could be read from the received UDP datagram.");
//        }

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
