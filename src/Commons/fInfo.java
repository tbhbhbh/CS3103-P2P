package Commons;

import java.util.ArrayList;

public class fInfo {
    /**
     * Contain Information of the File such as chunks and peer info
     */

    private String filename;
//    private String checksum;
    private ArrayList<cInfo> chunkList;

    public fInfo(String filename) {
        chunkList = new ArrayList<>();
        this.filename = filename;
    }


    public int getNumOfChunks() {
        return chunkList.size();
    }

    public void addChunk(cInfo chunk) {
        chunkList.add(chunk);
    }

    public cInfo getChunk(int i) {
        return chunkList.get(i);
    }

    public String getFilename() {
        return filename;
    }

}
