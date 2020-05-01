import java.io.File;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class Client {

    public static String directory = "TestFiles/";
    public static int chunkSize = 2000;
    public static int port = 3000;

    public static void main(String args[]) throws IOException {
        if (args.length < 2) {
            System.err.println("Usage: java Client <Destination IP> <Bin File>");
            return;
        }

        String ipDest = args[0];
        String binFile = args[1];

        DatagramSocket ds = new DatagramSocket();
        InetSocketAddress addr = new InetSocketAddress(ipDest, port);
        FileChunking fileChunker = new FileChunking(addr);
        Utility utility = new Utility();

        // File does not exist, so do not proceed further
        if (!new File(directory + binFile).exists()) {
            System.err.println(binFile + ": does not exist");
            return;
        }

        // String binFile = "t1.gif";

        // Send the initial filename without any parts differentiation
        String initMsg = "Filename#" + binFile;
        utility.sendMsg(initMsg, ds, addr);
        Boolean connAvail = initialHandshake(ds);

        if (connAvail) {
            ds.setSoTimeout(Integer.MAX_VALUE);

            String[] fileChunks = fileChunker.splitFile(directory + binFile, chunkSize, ds);

            ExecutorService executor = Executors.newFixedThreadPool(1);
            try {
                executor.execute(new ReceiveClientThread(ds));
                executor.shutdown();
                while (!executor.isTerminated()) {
                    Thread.sleep(7000);
                    utility.sendMsg("Finished\n", ds, addr);
                }
            } catch (Exception err) {
                err.printStackTrace();
            }
        } else {
            System.err.println("Server: " + ipDest + " - is not available");
        }
    }

    /**
     * Does initial handshakes. Waits for ACK to come back from server. If after 2s,
     * an ACK is still not returned, we consider this server unavailable
     *
     * @param ds - DatagramSocket used for sending and receiving data
     * @return success - True if server is available, False otherwise
     */
    public static boolean initialHandshake(DatagramSocket ds) {
        try {
            byte[] ackByte = new byte[chunkSize];
            ds.setSoTimeout(3000);
            DatagramPacket dpACK = new DatagramPacket(ackByte, ackByte.length);
            ds.receive(dpACK);
            String ack = new String(ackByte);
            if (ack.startsWith("ACK")) {
                return true;
            }
        } catch (IOException e) {
            return false;
        }
        return false;
    }
}
