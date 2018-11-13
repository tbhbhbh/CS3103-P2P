package Commons;

public class QueryDirPacket extends Packet {

    public QueryDirPacket(Object obj) {
        super(1, 1, obj);
    }

    public QueryDirPacket() {
        super(1,0);
    }


    public Object getPayload() {
        return payload;
    }
}
