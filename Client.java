import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

class Client {

    public static String directory = "TestFiles/";
    public static int chunkSize = 2000;
    public static int port = 3000;
    public static int TIMEOUT = 3000;

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
        InetSocketAddress addr = new InetSocketAddress(ipDest, port);
        FileChunking fileChunker = new FileChunking(addr);
        Utility utility = new Utility();
        String fname1 = "t1.gif";

        // Send the initial filename without any parts differentiation
        String initMsg = "Filename#" + fname1;
        utility.sendMsg(initMsg, ds, addr);
        Boolean connAvail = initialHandshake(ds);

        if (connAvail) {
            String[] fileChunks = fileChunker.splitFile(directory + fname1, chunkSize, ds);

            ExecutorService executor = Executors.newFixedThreadPool(1);
            try {
                executor.execute(new ReceiveClientThread(ds));
            } catch (Exception err) {
                err.printStackTrace();
            }
            executor.shutdown();
        } else {
            System.err.println("Server is not available");
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
            ds.setSoTimeout(TIMEOUT);
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
