import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;

class Client {

    public static void main(String args[]) throws IOException {

        DatagramSocket ds = new DatagramSocket();

        if (args.length < 2) {
            System.err.println("Usage: java Client <Destination IP> <Bin File>");
            return;
        }

        InetSocketAddress addr = new InetSocketAddress(args[0], 3000);
        // ds.bind(addr);

        // String msg = "Sending something";
        // byte buf[] = msg.getBytes();

        // DatagramPacket dpSend = new DatagramPacket(buf, buf.length, addr);

        // ds.send(dpSend);

        FileChunking fileChunker = new FileChunking(addr);

        String fname1 = "TestFiles/t1.gif";

        String[] fileChunks = fileChunker.splitFile(fname1, 2000, ds);
        // // fileStatistics.printChunkNames(fileChunks, 10);

        // for (int i = 0; i < fileChunks.length; i++) {
        // fileChunker.joinChunks(fileChunks[i]);
        // }

    }
}