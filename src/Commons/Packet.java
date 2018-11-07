package Commons;

import java.io.Serializable;

public class Packet implements Serializable {

    int type;
    int code;
    Object payload;

    public Packet(int type, int code, Object payload) {
        this.type = type;
        this.code = code;
        this.payload = payload;
    }

    public Packet(int type, int code){
        this.type = type;
        this.code = code;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public Object getPayload() {
        return payload;
    }
}
