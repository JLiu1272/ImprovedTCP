import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.io.FileOutputStream;

class Server {

    private static int chunkSize = 2000;
    private static final String directory = "TestFilesReceive";
    private static String oriFileName = "";
    private static String[] receivedChunks = null;

    public static void main(String args[]) throws IOException {

        int destPort = 3000;
        DatagramSocket ds = new DatagramSocket(destPort);

        byte[] receive = new byte[4 * 1024];
        DatagramPacket dpReceieve = null;

        // An array list that keeps of which chunks have arrived
        int totalNumOfChunks = -1;

        while (true) {
            dpReceieve = new DatagramPacket(receive, receive.length);
            ds.receive(dpReceieve);
            byte[] packet = new byte[dpReceieve.getLength()];
            System.arraycopy(receive, 0, packet, 0, dpReceieve.getLength());

            SocketAddress addr = dpReceieve.getSocketAddress();

            FileChunking fileChunking = new FileChunking();
            Utility utility = new Utility();

            try {
                TCPProtocol tcpProtocol = utility.extractData(packet);
                Boolean resend = tcpProtocol.isResend();
                String fname = tcpProtocol.fileName();
                byte[] checksum = tcpProtocol.checksum();
                byte[] payload = tcpProtocol.payload();
                if (totalNumOfChunks == -1) {
                    totalNumOfChunks = fileChunking.getNumberOfChunks(fname);
                    receivedChunks = new String[totalNumOfChunks];
                }

                int chunkNum = fileChunking.getChunkNumber(fname);
                writeBinaryToFile(payload, fname);
                receivedChunks[chunkNum] = fname;

                System.out.println("Client:-" + fname);
            } catch (IllegalArgumentException | ArrayIndexOutOfBoundsException e) {
                String msg = new String(packet);
                if (msg.startsWith("Filename#")) {
                    oriFileName = msg.split("#")[1];
                } else if (packet[0] == 1 || msg.startsWith("Finished")) {
                    System.out.println("Finishing");
                    // The packet received is a resent. Now check 
                    // to see if all the packets have arrived
                    completeTransaction(addr, ds, msg);
                }
            }
        }
    }

    public static void completeTransaction(SocketAddress addr, DatagramSocket ds, String msg) throws IOException {
        System.out.println("Client:-" + "Finishing");
        if (receivedChunks == null) {
            System.err.println("No data was sent over");
        } else {

            ArrayList<String> missingFiles = findMissingFiles(receivedChunks);

            // First we need to check whether we have all
            // the files necessary to stich together the original image.
            // If the arraylist of missing files has nothing, it means
            // we have all the files.
            if (missingFiles.size() == 0) {
                stitchChunks();
                notifyClient(addr, ds, "ReceivedSuccess\n");
            } else {
                notifyClientMissingFiles(missingFiles, addr, ds);
            }
        }
    }

    public static void stitchChunks() throws IOException {
        FileChunking fileChunking = new FileChunking();
        for (int i = 0; i < receivedChunks.length; i++) {
            fileChunking.joinChunks(directory + "/" + receivedChunks[i]);
        }
    }

    public static void notifyClientMissingFiles(ArrayList<String> missingFiles, SocketAddress addr, DatagramSocket ds) {
        ExecutorService executor = Executors.newFixedThreadPool(missingFiles.size());
        try {
            executor.execute(new SendServerThread(missingFiles, ds, addr));
            executor.shutdown();
        } catch (Exception err) {
            err.printStackTrace();
        }
    }

    public static void notifyClient(SocketAddress addr, DatagramSocket ds, String msg) throws IOException {
        // Send client a message indicating that the transaction succeeded
        byte[] msgByte = msg.getBytes();
        DatagramPacket msgPkt = new DatagramPacket(msgByte, msgByte.length, addr);
        ds.send(msgPkt);
    }

    private static ArrayList<String> findMissingFiles(String[] receivedChunks) {
        ArrayList<String> missingFiles = new ArrayList<>();

        FileChunking fileChunking = new FileChunking();
        int nChunks = receivedChunks.length;
        int chunkIndexLength = String.format("%d", nChunks).length();

        for (int i = 0; i < nChunks; i++) {
            // If the value in this cell is null, it means
            // I never receieved any data for this chunk.
            if (receivedChunks[i] == null) {
                missingFiles.add(fileChunking.chunkFileName(oriFileName, i, nChunks, chunkIndexLength));
            }
        }
        return missingFiles;
    }

    public static void writeBinaryToFile(byte[] buffer, String fname) throws IOException {
        FileOutputStream fos = new FileOutputStream(directory + "/" + fname);
        fos.write(buffer);
        fos.close();
    }
        
    // A utility method to convert the byte array
    // data into a string representation.
    // Returns the last byte index that is part of the filename
    public static int byteToStr(byte[] a, StringBuilder fname) {
        if (a == null)
            return -1;
        // StringBuilder ret = new StringBuilder();
        int i = 0;
        int numColon = 1;
        while (a[i] != 0) {
            char letter = (char) a[i];
            // If there are 4 colons consecutively, we have already obtained
            // the filename
            if (numColon >= 4) {
                return i + 1;
            }

            // If the letter is a color, we increment the numColon counter
            if (letter == ':') {
                numColon += 1;
            } else {
                // We have encoutered a character that is
                // not a colon, so reset the numColon counter.
                // Append the character to the name
                fname.append(letter);
                numColon = 1;
            }
            i++;
        }
        return i;
    }
}
