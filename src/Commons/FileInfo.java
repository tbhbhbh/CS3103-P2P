package Commons;

import java.util.ArrayList;

public class FileInfo {
    /**
     * Contain Information of the File such as chunks and peer info
     */

    private String filename;
//    private String checksum;
    private ArrayList<ChunkInfo> chunkList;

    public FileInfo(String filename) {
        chunkList = new ArrayList<>();
        this.filename = filename;
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

}
