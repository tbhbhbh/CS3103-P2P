package Commons;

import java.util.LinkedList;

public class UpdatePacket extends Packet {
    private String filename;
    private LinkedList<String> checksums;
    private int chunks;
    public UpdatePacket(String filename, int chunks, LinkedList checksums) {
        super(4, 0);
        this.filename = filename;
        this.chunks = chunks;
        this.checksums = checksums;
    }

    public int getChunks() {
        return chunks;
    }

    public String getFilename() {
        return filename;
    }

    public LinkedList<String> getChecksums() {
        return checksums;
    }

    public String getPayload() {
        return null;
    }
}
