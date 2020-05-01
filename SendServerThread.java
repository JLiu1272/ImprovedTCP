import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.ArrayList;

public class SendServerThread implements Runnable {

    DatagramSocket ds = null;
    SocketAddress addr = null;
    ArrayList<String> missingFiles = null;

    public SendServerThread(ArrayList<String> missingFiles, DatagramSocket ds, SocketAddress addr) {
        this.ds = ds;
        this.addr = addr;
        this.missingFiles = missingFiles;
    }

    public void run() {
        try {

            for (String filename : missingFiles) {
                // Need to send messages to client indicating which files
                // are still missing
                String filenameWNewLine = filename + "\n";
                byte[] filenameBytes = filenameWNewLine.getBytes();
                DatagramPacket data = new DatagramPacket(filenameBytes, filenameBytes.length, addr);
                ds.send(data);
            }

        } catch (Exception err) {
            err.printStackTrace();

        }
    }
}