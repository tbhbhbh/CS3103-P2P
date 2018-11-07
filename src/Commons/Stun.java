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

                    String hex1 = Integer.toHexString(ip1^21);
                    String hex1Trim = hex1.substring(Math.max(hex1.length() - 2, 0));
                    int octlet1 = Integer.parseInt(hex1Trim,16);

                    String hex2 = Integer.toHexString(ip2^0x12);
                    String hex2Trim = hex2.substring(Math.max(hex2.length() - 2, 0));
                    int octlet2 = Integer.parseInt(hex2Trim,16);

                    String hex3 = Integer.toHexString(ip3^0xA4);
                    String hex3Trim = hex3.substring(Math.max(hex3.length() - 2, 0));
                    int octlet3 = Integer.parseInt(hex3Trim,16);

                    String hex4 = Integer.toHexString(ip4^0x42);
                    String hex4Trim = hex4.substring(Math.max(hex4.length() - 2, 0));
                    int octlet4 = Integer.parseInt(hex4Trim,16);

                    System.out.println(String.format("%s.%s.%s.%s:%s", hex1Trim, hex2Trim, hex3Trim, hex4Trim, numPort));
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
