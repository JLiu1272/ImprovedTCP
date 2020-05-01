import java.io.FileOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

class Server {

    private static int chunkSize = 2000;
    private static final String directory = "TestFilesReceive";
    private static String oriFileName = "";
    private String[] receivedChunks = null;
    private static int BUFSIZE = 4 * 1024;
    private ArrayList<String> missingFiles = null;

    public static void main(String[] args) throws IOException {

        int destPort = 3000;
        DatagramSocket ds = new DatagramSocket(destPort);
        Server server = new Server();

        byte[] receive = new byte[BUFSIZE];
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
                    server.receivedChunks = new String[totalNumOfChunks];
                }

                int chunkNum = fileChunking.getChunkNumber(fname);
                server.writeBinaryToFile(payload, fname);
                server.receivedChunks[chunkNum] = fname;

                System.out.println("Client:-" + fname + ", Resend: " + resend.toString());
                if (resend) {
                    Boolean allFilesPatched = server.removeMissingFile(fname);
                    if (allFilesPatched)
                        server.completeTransaction(addr, ds);
                }
            } catch (IllegalArgumentException | ArrayIndexOutOfBoundsException e) {
                String msg = new String(packet);
                if (msg.startsWith("Filename#")) {
                    oriFileName = msg.split("#")[1];
                    server.notifyClient(addr, ds, "ACK\n");
                } else if (msg.startsWith("Finished")) {
                    System.out.println("Finishing");

                    // to see if all the packets have arrived
                    server.completeTransaction(addr, ds);
                }
            }
        }
    }

    public Boolean removeMissingFile(String filename) {
        if (missingFiles.contains(filename)) {
            missingFiles.remove(filename);
        }
        return missingFiles.size() == 0;
    }

    public void completeTransaction(SocketAddress addr, DatagramSocket ds) throws IOException {
        System.out.println("Client:-" + "Finishing");
        if (receivedChunks == null) {
            System.err.println("No data was sent over");
        } else {

            missingFiles = (missingFiles == null) ? findMissingFiles(receivedChunks) : missingFiles;

            // First we need to check whether we have all
            // the files necessary to stich together the original image.
            // If the arraylist of missing files has nothing, it means
            // we have all the files.
            if (missingFiles.size() == 0) {
                stitchChunks();
                notifyClient(addr, ds, "ReceivedSuccess\n");
            } else {
                notifyClientMissingFiles(addr, ds);
            }
        }
    }

    public void stitchChunks() throws IOException {
        FileChunking fileChunking = new FileChunking();
        for (int i = 0; i < receivedChunks.length; i++) {
            fileChunking.joinChunks(directory + "/" + receivedChunks[i]);
        }
    }

    public void notifyClientMissingFiles(SocketAddress addr, DatagramSocket ds) {
        ExecutorService executor = Executors.newFixedThreadPool(missingFiles.size());
        try {
            executor.execute(new SendServerThread(missingFiles, ds, addr));
            executor.shutdown();
            executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        } catch (InterruptedException err) {
            err.printStackTrace();
        }
    }

    public void notifyClient(SocketAddress addr, DatagramSocket ds, String msg) throws IOException {
        // Send client a message indicating that the transaction succeeded
        byte[] msgByte = msg.getBytes();
        DatagramPacket msgPkt = new DatagramPacket(msgByte, msgByte.length, addr);
        ds.send(msgPkt);
    }

    public ArrayList<String> findMissingFiles(String[] receivedChunks) {
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

    public void writeBinaryToFile(byte[] buffer, String fname) throws IOException {
        FileOutputStream fos = new FileOutputStream(directory + "/" + fname);
        fos.write(buffer);
        fos.close();
    }

    // A utility method to convert the byte array
    // data into a string representation.
    // Returns the last byte index that is part of the filename
    public int byteToStr(byte[] a, StringBuilder fname) {
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
