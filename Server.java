import java.io.FileOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketAddress;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * File: Server Author: Jennifer Liu
 * 
 * Objective: The server is responsible for accepting files from the the client
 * in chunks. The server will notify client when there are missing files or
 * files that were corrupted and needs a new one. It will assemble the chunks to
 * create a complete file and save it onto the computer.
 * 
 */
public class Server {

    private static final String directory = "TestFilesReceive";
    private static String oriFileName = "";
    private String[] receivedChunks = null;
    private static int BUFSIZE = 5 * 1024;

    // It is important to know that these arraylist only
    // contains the name of the files. It does not contain
    // the binary content
    private Set<String> missingFiles = null;
    private Set<String> corruptedFiles = null;

    /**
     * Driver program for server. Polls for messages from client
     * 
     * @param args
     * @throws IOException
     */
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
                byte[] clientChecksum = tcpProtocol.checksum();
                byte[] payload = tcpProtocol.payload();

                if (server.isDataCorrupted(clientChecksum, fname, payload)) {
                    server.addCorruptedFiles(fname);
                } else {
                    if (totalNumOfChunks == -1) {
                        totalNumOfChunks = fileChunking.getNumberOfChunks(fname);
                        server.receivedChunks = new String[totalNumOfChunks];
                    }

                    int chunkNum = fileChunking.getChunkNumber(fname);
                    server.writeBinaryToFile(payload, fname);
                    server.receivedChunks[chunkNum] = fname;

                    System.out.println("Client:-" + fname + ", Resend: " + resend.toString());
                    if (resend) {
                        Boolean noMissingFiles = server.removeMissingFile(fname);
                        Boolean noCorruptedFiles = server.removeCorruptedFile(fname);
                        if (noMissingFiles && noCorruptedFiles)
                            server.completeTransaction(addr, ds);
                    }
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
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Determine whether this packet is corrupted or not by comparing the checksum
     * sent in the packet and the checksum computed when the packet arrived.
     * 
     * @param clientChecksum
     * @param chunkFileName
     * @param payload
     * @return A boolean value determining whether data is corrupted or not
     */
    public Boolean isDataCorrupted(byte[] clientChecksum, String chunkFileName, byte[] payload) {
        Utility utility = new Utility();
        byte[] serverChecksum = utility.computeChecksum(chunkFileName, payload);
        return !Arrays.equals(clientChecksum, serverChecksum);
    }

    /**
     * Add this file to the corruptedFiles hash set
     * 
     * @param filename
     */
    public void addCorruptedFiles(String filename) {
        corruptedFiles = (corruptedFiles == null) ? new LinkedHashSet<>() : corruptedFiles;
        corruptedFiles.add(filename);
    }

    /**
     * If the filename is in the corruptedFile, remove the file from the corrupted
     * file
     * 
     * @param filename
     * @return
     */
    public Boolean removeCorruptedFile(String filename) {
        if (corruptedFiles != null) {
            if (corruptedFiles.contains(filename)) {
                corruptedFiles.remove(filename);
            }
            return corruptedFiles.size() == 0;
        }
        return true;
    }

    /***
     * If the file is in the missingFiles arraylist, remove it from there.
     * 
     * @param filename
     * @return A boolean value indicating whether there are still missing files
     */
    public Boolean removeMissingFile(String filename) {
        if (missingFiles != null) {
            if (missingFiles.contains(filename)) {
                missingFiles.remove(filename);
            }
            return missingFiles.size() == 0;
        }
        return true;
    }

    /**
     * Client has sent us all the files. Check if we have everything. If we do,
     * stitch the file together, and notify client that we were able to reassemble
     * the file. If we do not have all the file, notify the client which files are
     * missing.
     * 
     * @param addr - SocketAddress, the socket address of client
     * @param ds   - DatagramSocket, the datagramSocket that is used to send
     *             messages
     * @throws IOException
     */
    public void completeTransaction(SocketAddress addr, DatagramSocket ds) throws IOException {
        System.out.println("Client:-" + "Finishing");
        if (receivedChunks == null) {
            System.err.println("No data was sent over");
        } else {
            missingFiles = (missingFiles == null) ? findMissingFiles(receivedChunks) : missingFiles;

            if (corruptedFiles != null) {
                missingFiles.addAll(corruptedFiles);
            }

            // First we need to check whether we have all
            // the files necessary to stich together the original image.
            // If the arraylist of missing files has nothing, it means
            // we have all the files.
            if (missingFiles.size() == 0) {
                notifyClient(addr, ds, "ReceivedSuccess\n");
                stitchChunks();
            } else {
                notifyClientMissingFiles(addr, ds);
            }
        }
    }

    /**
     * Piece all of the chunks together to make the final file
     * 
     * @throws IOException
     */
    public void stitchChunks() throws IOException {
        FileChunking fileChunking = new FileChunking();
        if (receivedChunks != null) {
            fileChunking.joinChunks(directory + "/" + receivedChunks[0]);
        }
    }

    /**
     * A function to notify the client of missing files. It traverses through the
     * missing file, and creates a new thread to send client a notification of
     * missing files
     * 
     * @param addr
     * @param ds
     */
    public void notifyClientMissingFiles(SocketAddress addr, DatagramSocket ds) {
        ExecutorService executor = Executors.newFixedThreadPool(missingFiles.size());
        try {
            for (String filename : missingFiles) {
                executor.execute(new SendServerThread(filename, ds, addr));
            }
            executor.shutdown();
            executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        } catch (InterruptedException err) {
            err.printStackTrace();
        }
    }

    /**
     * Notify client a generic message that is determined by the caller of this
     * function
     * 
     * @param addr - Address of client
     * @param ds   - The socket used to send this message
     * @param msg  - The message to be sent to client
     * @throws IOException
     */
    public void notifyClient(SocketAddress addr, DatagramSocket ds, String msg) throws IOException {
        // Send client a message indicating that the transaction succeeded
        byte[] msgByte = msg.getBytes();
        DatagramPacket msgPkt = new DatagramPacket(msgByte, msgByte.length, addr);
        ds.send(msgPkt);
    }

    /**
     * Finding missing chunks by traversing through the missingFiles array, and
     * seeing if any of the values are null
     * 
     * @param receivedChunks - A string array containing the filename of received
     *                       chunks
     * @return - A LinkedHashSet of Strings containing the filename of missing files
     */
    public LinkedHashSet<String> findMissingFiles(String[] receivedChunks) {
        LinkedHashSet<String> missingFiles = new LinkedHashSet<>();

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

    /**
     * Given the binary data and filename, write this binary chunk to hard drive
     * 
     * @param buffer (byte[]) - The binary data
     * @param fname  (String) - Filename of chunk
     * @throws IOException
     */
    public void writeBinaryToFile(byte[] buffer, String fname) throws IOException {
        FileOutputStream fos = new FileOutputStream(directory + "/" + fname);
        fos.write(buffer);
        fos.close();
    }
}