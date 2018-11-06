package Commons;

import java.io.Serializable;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Random;

public class ChunkInfo implements Serializable {

    private int chunkID;
//    private String checksum;
    private ArrayList<InetSocketAddress> peers = new ArrayList<>();

    public ChunkInfo(int chunkID) {
        this.chunkID = chunkID;
    }

    public int getChunkID() {
        return chunkID;
    }

    public void addPeer(InetSocketAddress inet){
        peers.add(inet);
    }

    public void addPeer(String ipAddress, int port) {
        InetSocketAddress inet = new InetSocketAddress(ipAddress, port);
        peers.add(inet);
    }

    public void removePeer(InetSocketAddress inet) {
        peers.remove(inet);
    }

    public InetSocketAddress getRdmPeer() {
        Random r = new Random();
        if (peers.size() == 0) {
            return null; // throw error instead???
        }
        int choosenInt = r.nextInt(peers.size());
        return peers.get(choosenInt);
    }

    public ArrayList<InetSocketAddress> getPeers() {
        return peers;
    }
}
