import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;

/**
 * File: ReceiveClientThread.java Author: Jennifer Liu
 * 
 * Objective: This thread is listening for messages from the server to see if
 * there are any missing packets. This thread will continue to run until an ACK
 * (ReceivedSuccess) is sent back from server. If it receives a message from
 * server indicating missing file, it will obtain the missing file chunk
 * information and package it into a TCPProtocol and send it to server.
 */
public class ReceiveClientThread implements Runnable {

    DatagramSocket ds = null;
    Boolean transferComplete = false;
    int chunkSize = 5000;
    String directory = "TestFiles/";

    public ReceiveClientThread(DatagramSocket ds) {
        this.ds = ds;
    }

    public void run() {
        Utility utility = new Utility();
        byte[] receive = new byte[chunkSize];
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

                // Server has indicated that it has
                // received all data required to stitch the
                // data together. We cna now close this thread
                if (msg.startsWith("ReceivedSuccess")) {
                    System.out.println("Completed transaction successfully");
                    transferComplete = true;
                } else {
                    // Missing file. Package missing file and send
                    // to server
                    byte[] binaryDataBuffer = new byte[chunkSize];
                    String chunkFName = msg.replace("\n", "");
                    System.out.println("Missing files: " + chunkFName);

                    utility.fileToBinary(binaryDataBuffer, directory + chunkFName);
                    TCPProtocol tcpProtocol = new TCPProtocol(chunkFName, binaryDataBuffer, true);
                    byte[] packagedData = tcpProtocol.packageData(false);
                    dpSend = new DatagramPacket(packagedData, packagedData.length, dpReceived.getSocketAddress());
                    ds.send(dpSend);
                }
            } catch (IOException err) {
                err.printStackTrace();
            }
        }
    }
}