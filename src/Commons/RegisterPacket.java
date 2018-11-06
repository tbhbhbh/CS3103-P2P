package Commons;

import java.net.InetAddress;
import java.net.InetSocketAddress;

public class RegisterPacket extends Packet {

    private int port;
    private InetAddress publicIP;
    public RegisterPacket(InetSocketAddress publicIP){
        super(0,0);
        this.port = publicIP.getPort();
        this.publicIP = publicIP.getAddress();
    }

    public RegisterPacket(int port){
        super(0,0);
        this.port = port;
    }

    public InetAddress getPublicIP() {
        return publicIP;
    }

    public int getPort() {
        return port;
    }

    public Object getPayload() {
        return payload;
    }
}
