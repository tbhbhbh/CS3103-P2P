package Commons;

public class UpdatePacket extends Packet {
    private String filename;
    private String checksum;
    private int chunks;
    public UpdatePacket(String filename, int chunks) {
        super(4, 0);
        this.filename = filename;
        this.chunks = chunks;
    }

    public int getChunks() {
        return chunks;
    }

    public String getFilename() {
        return filename;
    }

    public String getPayload() {
        return null;
    }
}
