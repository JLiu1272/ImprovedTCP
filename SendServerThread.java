import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketAddress;

/**
 * File: SendServerThread.java Author: Jennifer Liu
 * 
 * Objective: This thread is responsible for notifying the client that there are
 * missing files, and it needs to be resent.
 */
public class SendServerThread implements Runnable {

    DatagramSocket ds = null;
    SocketAddress addr = null;
    String filename = "";

    /**
     * The thread requires the caller to provide datagramsocket and destination
     * address that this packet should be sent. Additionally, caller needs to
     * provide the missing filename
     * 
     * @param filename
     * @param ds
     * @param addr
     */
    public SendServerThread(String filename, DatagramSocket ds, SocketAddress addr) {
        this.ds = ds;
        this.addr = addr;
        this.filename = filename;
    }

    /**
     * This is what runs when this thread is kickstarted. It sends a packet to
     * client indicating that this file is missing
     */
    public void run() {
        try {

            // Need to send messages to client indicating which files
            // are still missing
            String filenameWNewLine = filename + "\n";
            byte[] filenameBytes = filenameWNewLine.getBytes();
            DatagramPacket data = new DatagramPacket(filenameBytes, filenameBytes.length, addr);
            ds.send(data);

        } catch (Exception err) {
            err.printStackTrace();

        }
    }
}