package Commons;

import java.io.Serializable;
import java.net.InetSocketAddress;
import java.util.ArrayList;

public class FileInfo implements Serializable {
    /**
     * Contain Information of the File such as chunks and peer info
     */

    private String filename;
    private String checksum;
    private ArrayList<ChunkInfo> chunkList;
    private int numOfPeers;

    public FileInfo(String filename) {
        chunkList = new ArrayList<>();
        this.filename = filename;
        numOfPeers = 0;
    }

    public String getChecksum() {
        return checksum;
    }

    public void setChecksum(String checksum) {
        this.checksum = checksum;
    }

    public void removePeer(InetSocketAddress inet) {
        for (ChunkInfo chunk: chunkList) {
            chunk.removePeer(inet);
        }
        numOfPeers--;
    }

    public void addPeer(InetSocketAddress inet) {
        for (ChunkInfo chunk: chunkList) {
            chunk.addPeer(inet);
        }
        numOfPeers++;
    }

    public int getNumOfChunks() {
        return chunkList.size();
    }

    public void addChunk(ChunkInfo chunk) {
        chunkList.add(chunk);
    }

    public ChunkInfo getChunk(int i) {
        return chunkList.get(i);
    }

    public String getFilename() {
        return filename;
    }

    public boolean isEmpty() {
        return numOfPeers <= 0;
    }

}
