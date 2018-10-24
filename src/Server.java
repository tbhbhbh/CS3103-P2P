import java.net.DatagramSocket;
import java.net.SocketException;

public class Server {

    private final int LISTENING_PORT = 8181;
    private DatagramSocket socket;

    public Server() {
        System.out.println("Init server\n");

        //try to obtain server socket
        try {
            socket = new DatagramSocket(LISTENING_PORT);
        } catch (SocketException e) {
            System.err.println("Cannot obtain server socket");

        }
    }
}
