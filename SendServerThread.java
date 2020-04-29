import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.ArrayList;

public class SendServerThread implements Runnable {

    DatagramSocket ds = null;
    SocketAddress addr = null;
    String fileName = "";

    public SendServerThread(String filename, DatagramSocket ds, SocketAddress addr) {
        this.ds = ds;
        this.addr = addr;
        this.fileName = filename;
    }

    public void run() {
        try {
            // Need to send messages to client indicating which files
            // are still missing
            String missedFile = fileName + "\n";
            byte[] errMsgBytes = missedFile.getBytes();
            DatagramPacket data = new DatagramPacket(errMsgBytes, errMsgBytes.length, addr);
            ds.send(data);

        } catch (Exception err) {
            err.printStackTrace();

        }
    }
}