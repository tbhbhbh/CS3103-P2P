package Client;

import java.io.*;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import Commons.fInfo;
import Commons.cInfo;

public class Peer {

    private final String OUTPUT_DIRECTORY = "./src/p2pdonload/";
    private final String CHUNK_DIRECTORY = OUTPUT_DIRECTORY + "chunks/";
    private final int BUFFER_SIZE = 1024;
    private fInfo fInfo;
    private int peerId;
    private int port;
    private int numChunks;
    final String directory = "./src/files/";

    public ServerSocket serverSocket;

    public Peer() {
        this.port = generatePort();

    }

    // getters
    public int getPort() { return port; }

    // setters
    public void setPort() { this.port = port; }

    /*
     * Randomly generates a port number for client to use for their socket.
     */
    private int generatePort() {
        Random r = new Random();
        return r.nextInt(9000-8100) + 8100;
//        return 8000;
    }


    /*
     * Peer updates server of a file
     *
     */
    public void updateServer(Socket socket, String fileName) throws IOException {

        final long sourceSize = Files.size(Paths.get(directory + fileName));
        final long bytesPerSplit =  1024L; //1 chunk = 1024 bytes
        final int numChunks = (int) (sourceSize / bytesPerSplit);
        final long remainingBytes = sourceSize % bytesPerSplit;
        int position = 0;
        int index = 0;

        //create a folder with filename
        String foldername = "c"+fileName;
        System.out.println(new File(directory + foldername).mkdirs());


        //split file
        try (RandomAccessFile sourceFile = new RandomAccessFile(directory + fileName, "r");
             FileChannel sourceChannel = sourceFile.getChannel()) {
            for (; position < numChunks; position++) {
                Path filePart = Paths.get(directory + foldername + "/" + fileName + index);
                try (RandomAccessFile toFile = new RandomAccessFile(filePart.toFile(), "rw");
                    FileChannel toChannel = toFile.getChannel()) {
                    sourceChannel.position(position * bytesPerSplit);
                    toChannel.transferFrom(sourceChannel,  0, bytesPerSplit);

                }
                index++;
            }

            if (remainingBytes != 0) {
                Path filePart = Paths.get(directory + foldername + "/" + fileName + index);
                try (RandomAccessFile toFile = new RandomAccessFile(filePart.toFile(), "rw");
                     FileChannel toChannel = toFile.getChannel()) {
                    sourceChannel.position(position * bytesPerSplit);
                    toChannel.transferFrom(sourceChannel, 0, remainingBytes);

                }
            }
        }


        System.out.println("Registering peer");
        DataOutputStream dOut = new DataOutputStream(socket.getOutputStream());
        //Option to register in the server (new peer) with port number
        dOut.writeByte(0);
        dOut.flush();
        dOut.writeInt(port);
        dOut.flush();

        //File name
        dOut.writeByte(1);
        dOut.writeUTF(fileName);
        dOut.flush();
        //Number of chunks
        dOut.writeByte(2);
        dOut.writeInt(numChunks);
        dOut.flush();
//        //port number (done by option 0)
//        dOut.writeByte(3);
//        dOut.writeInt(port);
//        dOut.flush();
        //end connection
        dOut.writeByte(-1);
        dOut.flush();

        dOut.close();
//        socket.close();
    }

    //Uploading
    public void server() throws IOException {
        try {
            serverSocket = new ServerSocket(port);
            System.out.println(String.format("Peer serving %d", port));
        } catch (Exception e) {
            System.out.println(e);
            return;
        }

        while (true) {
            Socket socket = serverSocket.accept();
            System.out.println("Accepted connection from peer\n");

            //read input
            DataInputStream dIn = new DataInputStream(socket.getInputStream());
            DataOutputStream dOut = new DataOutputStream(socket.getOutputStream());

            byte option = dIn.readByte();

            if (option == 6) { //someone requesting a file
                String filename = dIn.readUTF();
                System.out.println(String.format("filename: %s", filename));
                byte chunkID = dIn.readByte();
                System.out.println(String.format("chunkID:%d", chunkID));
                File f = new File(directory+"c"+filename+"/"+filename+chunkID);
                FileInputStream fis = new FileInputStream(f);
                byte[] buffer = new byte[BUFFER_SIZE];
                long fileSize = f.length();
                while (fileSize > 0) {
                    int byteRead = fis.read(buffer);
                    dOut.write(buffer, 0, byteRead);
                    dOut.flush();
                    fileSize -= byteRead;
                    System.out.println(String.format("Sent byte: %d", byteRead));
                }


                fis.close();
            }
            dOut.close();
            dIn.close();
            socket.close();
        }
    }

    // Downloading
    public void download() throws Exception {
        fInfo = new fInfo("test");
        for (int i= 0; i< 14 ; i++) {
            cInfo cInfo = new cInfo(i);
            cInfo.addPeer(new InetSocketAddress("localhost", 8000));
            fInfo.addChunk(cInfo);
        }

        if (fInfo == null) {
            System.out.println("Request file from server first!");
            return;
        }

        String filename = fInfo.getFilename();
        Path directory = Files.createDirectories(Paths.get(CHUNK_DIRECTORY, filename));

        // download chunks from peers

        numChunks = fInfo.getNumOfChunks();
        // Single Thread for now...
        for (int i = 0; i < numChunks; i++) {
            cInfo chunk = fInfo.getChunk(i);
            int chunkID = chunk.getChunkID();
            InetSocketAddress peerSocket = chunk.getRdmPeer();
            InetAddress peerAddress = peerSocket.getAddress();
            int peerPort = peerSocket.getPort();
            downloadFromPeer(directory, peerAddress, peerPort, filename, chunkID);
        }

        System.out.println(String.format("Finish downloading all the chunks of %s", filename));
        System.out.println("Combining all the chunks together...");

        // merge chunks together
        FileOutputStream fos = new FileOutputStream(OUTPUT_DIRECTORY+filename);
        DataOutputStream dos = new DataOutputStream(new BufferedOutputStream(fos));

        byte[] buffer = new byte[BUFFER_SIZE];
        for (int i = 0; i<numChunks; i++) {
            FileInputStream fis = new FileInputStream(new File(directory.toString(),String.valueOf(i)));
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

        // TODO: checksum

    }

    public void downloadFromPeer(Path directory, InetAddress peerAddress, int port, String fileName, int i)  throws IOException {
        // connect to peer
        Socket socket = new Socket(peerAddress, port);

        // send chunk request
        DataOutputStream dos = new DataOutputStream(socket.getOutputStream());

        dos.writeByte(6);
        dos.writeUTF(fileName);
        dos.writeByte(i);


        // recv chunk data
        InputStream in = socket.getInputStream();

        File f = new File(directory.toString(),String.valueOf(i));
        DataOutputStream dosFile = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(f)));
        int byteRead;
        byte[] buffer = new byte[BUFFER_SIZE];
        while (true) {
            byteRead = in.read(buffer);
            System.out.println(byteRead);
            if (byteRead == -1)
                break;
            dosFile.write(buffer, 0, byteRead);
            dosFile.flush();
        }
        System.out.println(String.format("Downloaded Chunk %d of %s", i, fileName));
        dosFile.close();
        in.close();
        dos.close();
        socket.close();

        // TODO: inform tracker of completed chunk
    }

    public void getDir(Socket socket) throws Exception {
        DataOutputStream dOut = new DataOutputStream(socket.getOutputStream());
        // option to get file listing
        dOut.writeByte(4);
        dOut.flush();

        ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());
        Object obj = ois.readObject();
        Iterator<String> fileListing = ((List<String>) obj).iterator();
        while(fileListing.hasNext()) {
            System.out.println(fileListing.next());
        }
    }

    public void getFile(Socket socket) throws  Exception {
        DataOutputStream dOut = new DataOutputStream(socket.getOutputStream());
        // Option to get file Info
        dOut.writeByte(5);
        dOut.writeUTF("test");
        dOut.flush();

        ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());
        Object obj = ois.readObject();
        fInfo fileInfo = (fInfo)obj;
        this.fInfo = fileInfo;
        System.out.println("Fetched File Info!");
    }


}
