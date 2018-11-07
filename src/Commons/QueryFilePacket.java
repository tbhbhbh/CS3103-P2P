package Commons;

public class QueryFilePacket extends Packet {

    private String filename;

    public QueryFilePacket(String filename) {
        super(2,0);
        this.filename = filename;
    }
    public QueryFilePacket(FileInfo fileInfo) {
        super(2,1, fileInfo);
    }

    public String getFilename() {
        return filename;
    }

    public Object getPayload() {
        return payload;
    }
}
