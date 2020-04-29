import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;

public class ReceiveClientThread implements Runnable {

    DatagramSocket ds = null;
    Boolean transferComplete = false;
    int buffSize = 4 * 1024;
    String directory = "TestFiles/";

    public ReceiveClientThread(DatagramSocket ds) {
        this.ds = ds;
    }

    public void run() {
        FileChunking fileChunking = new FileChunking();
        Utility utility = new Utility();
        byte[] receive = new byte[buffSize];
        DatagramPacket dpReceived = null;
        DatagramPacket dpSend = null;

        // Continue to listen for messages until
        // we receive from the server that it has completed all
        // transactions
        while (!transferComplete) {
            try {
                dpReceived = new DatagramPacket(receive, receive.length);
                ds.receive(dpReceived);

                String msg = utility.byteToStr(receive).toString();

                if (msg.startsWith("ReceivedSuccess")) {
                    System.out.println("Completed transaction successfully");
                    transferComplete = true;
                } else {
                    byte[] binaryDataBuffer = new byte[buffSize];
                    String chunkFName = msg.replace("\n", "");
                    System.out.println("Missing files: " + chunkFName);

                    utility.fileToBinary(binaryDataBuffer, directory + chunkFName);
                    byte[] combinedData = utility.createPacketObj(chunkFName, binaryDataBuffer);
                    dpSend = new DatagramPacket(combinedData, combinedData.length, dpReceived.getSocketAddress());
                    ds.send(dpSend);
                }
            } catch (IOException err) {
                err.printStackTrace();
            }
        }
    }
}