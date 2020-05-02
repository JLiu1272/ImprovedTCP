import java.io.File;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * File: Client.java Function: This class is the client in the client-server
 * Author: Jennifer Liu
 * 
 * architecture. This class is responsible for splitting the binary file into
 * mini chunks, and sending each chunks to the server. Additionally, it also
 * handles resending missing chunks to the server.
 */
public class Client {

    public static String directory = "TestFiles/";
    public static int chunkSize = 5000;
    public static int port = 3000;

    /**
     * Driver program for client
     */
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

        // Send the initial filename without any parts differentiation
        String initMsg = "Filename#" + binFile;
        utility.sendMsg(initMsg, ds, addr);
        Boolean connAvail = initialHandshake(ds);

        // Only perform chunk splitting if the server
        // is available
        if (connAvail) {
            ds.setSoTimeout(Integer.MAX_VALUE);
            Date startTime = new Date();
            System.out.println("Starting Time: " + startTime.toString());

            fileChunker.splitFile(directory + binFile, chunkSize, ds);

            ExecutorService executor = Executors.newFixedThreadPool(1);
            try {
                executor.execute(new ReceiveClientThread(ds));
                executor.shutdown();
                Date endTime = new Date();
                long timeElapsed = (endTime.getTime() - startTime.getTime()) / 1000 % 60;
                while (!executor.isTerminated()) {
                    utility.sendMsg("Finished\n", ds, addr);
                    Thread.sleep(1000);
                }
                System.out.println("Execution Time: " + String.valueOf(timeElapsed) + "s");
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
