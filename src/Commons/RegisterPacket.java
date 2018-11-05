package Commons;

import java.net.InetSocketAddress;

public class RegisterPacket extends Packet {

    private int port;
    private InetSocketAddress publicIP;
    public RegisterPacket(int port, InetSocketAddress publicIP){
        super(0,0);
        this.port = port;
        this.publicIP = publicIP;
    }

    public RegisterPacket(int port){
        super(0,0);
        this.port = port;
    }

    public int getPort() {
        return port;
    }

    public Object getPayload() {
        return payload;
    }
}
