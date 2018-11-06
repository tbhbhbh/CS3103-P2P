package Commons;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;

public class Stun {
    public static void main(String[] args) throws Exception{
        holePunch(new DatagramSocket(5000),"74.125.200.127");
    }
    public static InetSocketAddress holePunch(DatagramSocket dsock, String STUNIP) throws Exception{
//        dsock = new DatagramSocket(peerPort);
        System.out.println("Running STUN Discovery...");
        dsock.connect(InetAddress.getByName(STUNIP), 19305);
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
        DatagramPacket recvPkt = new DatagramPacket(new byte[256], 256);
        dsock.receive(recvPkt);

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
                    int numPort = port ^0x2112;
                    if (port < 0) {
                        numPort = port ^0xffff2112;
                    }
                    byte ip1 = bb1.get();
                    byte ip2 = bb1.get();
                    byte ip3 = bb1.get();
                    byte ip4 = bb1.get();

                    int octlet1 = ip1^0xffffff21;
                    if (!isValidOctlet(octlet1)) {
                        octlet1 = ip1^0x21;
                    }
                    int octlet2 = ip2^0xffffff12;
                    if (!isValidOctlet(octlet2)) {
                        octlet2 = ip1^0x12;
                    }
                    int octlet3 = ip3^0xffffffA4;
                    if (!isValidOctlet(octlet3)) {
                        octlet3 = ip1^0xA4;
                    }
                    int octlet4 = ip4^0xffffff42;
                    if (!isValidOctlet(octlet4)) {
                        octlet4 = ip1^0x42;
                    }


                    System.out.println(String.format("%d.%d.%d.%d:%d", octlet1, octlet2, octlet3, octlet4, numPort));
                    InetSocketAddress inetSocketAddress = new InetSocketAddress(
                            String.format("%d.%d.%d.%d", octlet1,octlet2,octlet3,octlet4), numPort);
                    dsock.disconnect();
                    System.out.println(dsock.isConnected());
//                    dsock.connect(InetAddress.getByName("74.125.200.127"), 19305);

                    return inetSocketAddress;
                }
                i += (4  + attrLen);

            }

        }
        return null;
    }

    public static boolean isValidOctlet(int x) {
        return x>=0 && x<=255;
    }

}
