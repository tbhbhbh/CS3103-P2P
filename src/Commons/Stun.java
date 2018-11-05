package Commons;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;

public class Stun {
    public static void main(String[] args) throws Exception{
        holePunch(5000);
    }
    public static InetSocketAddress holePunch(int peerPort) throws Exception{
        DatagramSocket dsock = new DatagramSocket(peerPort);
        dsock.connect(InetAddress.getByName("74.125.200.127"), 19305);
        byte[] bindingReq = new byte[20];
        short stunMethod = 0x0001;
        short msgLength = 0x0000;
        long magicCookie = 0x2112A442;
        long transID1 = 0x63c7117e;
        long transID2 = 0x0714278f;
        long transID3 = 0x5ded3221;

        ByteBuffer bb = ByteBuffer.allocate(20);
        bb.putShort(stunMethod);
        bb.putShort(msgLength);
        bb.putInt((int)magicCookie);
        bb.putInt((int)transID1);
        bb.putInt((int)transID2);
        bb.putInt((int)transID3);
        bb.flip();
        bb.get(bindingReq);

        DatagramPacket dp = new DatagramPacket(bindingReq,20);
        dsock.send(dp);
        System.out.println("SEND");
        DatagramPacket recvPkt = new DatagramPacket(new byte[256], 256);
        dsock.receive(recvPkt);
        System.out.println("RECV");

        byte[] data = recvPkt.getData();
        System.out.println(data.length);

        ByteBuffer bb1 = ByteBuffer.wrap(data);
        if (bb1.getShort() == 0x0101) {
            int i = 20;
            int n = bb1.getShort();
            while (i < data.length) {
                short attrType = bb1.position(i).getShort();
                short attrLen = bb1.getShort();
                if (attrType == 0x0020) {
                    short port = bb1.position(i+6).getShort();
                    port ^= 0x2112;
//                    System.out.println(port);
//                    System.out.println("position: "+ bb1.position());

                    byte ip1 = bb1.get();
                    byte ip2 = bb1.get();
                    byte ip3 = bb1.get();
                    byte ip4 = bb1.get();

//                    System.out.println(ip1^0xffffff21);

                    int octlet1 = ip1^0xffffff21;
                    int octlet2 = ip2^0xffffff12;
                    int octlet3 = ip3^0xffffffA4;
                    int octlet4 = ip4^0xffffff42;


//                    System.out.println("position: "+ bb1.position());
                    System.out.println(String.format("%d.%d.%d.%d:%d", octlet1, octlet2, octlet3, octlet4, port));
                    InetSocketAddress inetSocketAddress = new InetSocketAddress(
                            String.format("%d.%d.%d.%d", octlet1,octlet2,octlet3,octlet4), port);
                    dsock.close();
                    return inetSocketAddress;
                }
                i += (4  + attrLen);

            }

        }
        return null;
    }

}
