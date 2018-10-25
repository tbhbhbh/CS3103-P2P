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
import java.util.Random;

import Commons.fInfo;
import Commons.cInfo;

public class Peer {

    private final String OUTPUT_DIRECTORY = "p2pdownload/";
    private final String CHUNK_DIRECTORY = OUTPUT_DIRECTORY + "chunks/";
    private final int BUFFER_SIZE = 1024;
    private int peerId;
    private int port;
    private String fileName = "";
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
    }


    /*
     * Peer updates server of a file
     *
     */
    public void updateServer(Socket socket, String fileName) throws IOException {
        this.fileName = fileName;

        final long sourceSize = Files.size(Paths.get(directory + fileName));
        final long bytesPerSplit =  1024L; //1 chunk = 1024 bytes
        final int numChunks = (int) (sourceSize / bytesPerSplit);
        final long remainingBytes = sourceSize % bytesPerSplit;
        int position = 0;
        int index = 0;

        //create a folder with filename
        new File(directory + fileName).mkdirs();


        //split file
        try (RandomAccessFile sourceFile = new RandomAccessFile(directory + fileName, "r");
             FileChannel sourceChannel = sourceFile.getChannel()) {
            for (; position < numChunks; position++) {
                Path filePart = Paths.get(directory + fileName + "/" + fileName + index);
                try (RandomAccessFile toFile = new RandomAccessFile(filePart.toFile(), "rw");
                    FileChannel toChannel = toFile.getChannel()) {
                    sourceChannel.position(position * bytesPerSplit);
                    toChannel.transferFrom(sourceChannel,  0, bytesPerSplit);

                }
                index++;
            }

            if (remainingBytes != 0) {
                Path filePart = Paths.get(directory + fileName + "/" + fileName + index);
                try (RandomAccessFile toFile = new RandomAccessFile(filePart.toFile(), "rw");
                     FileChannel toChannel = toFile.getChannel()) {
                    sourceChannel.position(position * bytesPerSplit);
                    toChannel.transferFrom(sourceChannel, 0, remainingBytes);

                }
            }
        }


        System.out.println("Registering peer");
        DataOutputStream dOut = new DataOutputStream(socket.getOutputStream());
        //Option to register in the server (new peer)
        dOut.writeByte(0);
        dOut.flush();

        //File name
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

            //read input
            DataInputStream dIn = new DataInputStream(socket.getInputStream());
            DataOutputStream dOut = new DataOutputStream(socket.getOutputStream());

            byte option = dIn.readByte();

            if (option == 6) { //someone requesting a file
                String filename = dIn.readUTF();
                byte chunkID = dIn.readByte();

                File f = new File(directory+filename+"/"+filename+chunkID);
                FileInputStream fis = new FileInputStream(f);
                byte[] buffer = new byte[BUFFER_SIZE];
                int byteRead = fis.read(buffer);
                dOut.write(buffer, 0, byteRead);
                dOut.flush();


                fis.close();
            }
            dOut.close();
            dIn.close();
            socket.close();
        }
    }

    // Downloading
    public void download(fInfo fInfo) throws Exception {

        // allocate space for file
        String filename = "placeholder";

        Path directory = Files.createDirectory(Paths.get(CHUNK_DIRECTORY, filename));

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
            do {
                byteRead = fis.read(buffer);
                dos.write(buffer, 0, byteRead);
                dos.flush();
            } while(byteRead != 0);
            fis.close();
        }
        dos.close();
        System.out.println(String.format("%s successfully combined from its chunks", filename));

        // TODO: checksum

    }

    //Downloading
    public void downloadFromPeer(Path directory, InetAddress peerAddress, int port, String fileName, int i)  throws IOException {
        // connect to peer
        Socket socket = new Socket(peerAddress, port);

        // send chunk request
        DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
        dos.writeUTF(fileName+String.valueOf(i));
        dos.flush();

        // recv chunk data
        InputStream in = socket.getInputStream();

        File f = new File(directory.toString(),String.valueOf(i));
        DataOutputStream dosFile = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(f)));
        int byteRead;
        byte[] buffer = new byte[BUFFER_SIZE];
        do {
            byteRead = in.read(buffer);
            dosFile.write(buffer, 0, byteRead);
            dosFile.flush();
        } while(byteRead != 0);
        System.out.println(String.format("Downloaded Chunk %d of %s", i, fileName));
        dosFile.close();
        in.close();
        dos.close();
        socket.close();

        // TODO: inform tracker of completed chunk
    }


}
