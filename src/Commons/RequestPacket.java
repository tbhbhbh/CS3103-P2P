package Commons;

import java.io.Serializable;

public class RequestPacket<T> extends Packet {
    public RequestPacket(int code, Object obj) {
        super(2, code, obj);
    }

    public T getPayload() {
        return (T)payload;
    }

}
