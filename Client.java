import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

class Client {

    public static String directory = "TestFiles/";
    public static int chunkSize = 2000;

    public static void main(String args[]) throws IOException {
        // Utility utility = new Utility();
        // byte[] binaryDataBuffer = new byte[4 * 1024];
        // String filename = directory + "t1.gif.part200of243";
        // System.out.println(filename);
        // utility.fileToBinary(binaryDataBuffer, filename);

        // IP Destination
        // String ipDest = args[0];
        String ipDest = "127.0.0.1";

        // if (args.length < 2) {
        // System.err.println("Usage: java Client <Destination IP> <Bin File>");
        // return;
        // }

        DatagramSocket ds = new DatagramSocket();
        InetSocketAddress addr = new InetSocketAddress(ipDest, 3000);
        FileChunking fileChunker = new FileChunking(addr);
        Utility utility = new Utility();
        String fname1 = "t1.gif";

        // Send the initial filename without any parts differentiation
        String initMsg = "Filename#" + fname1;
        utility.sendMsg(initMsg, ds, addr);

        String[] fileChunks = fileChunker.splitFile(directory + fname1, chunkSize, ds);

        ExecutorService executor = Executors.newFixedThreadPool(1);
        try {
            executor.execute(new ReceiveClientThread(ds));
        } catch (Exception err) {
            err.printStackTrace();
        }
        executor.shutdown();

    }
}