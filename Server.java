import java.io.FileOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

class Server {

    private static int chunkSize = 0;

    private static final String directory = "TestFilesReceive";

    private static String oriFileName = "";

    public static void main(String args[]) throws IOException {

        int destPort = 3000;
        DatagramSocket ds = new DatagramSocket(destPort);

        byte[] receive = new byte[4 * 1024];
        DatagramPacket dpReceieve = null;

        // An array list that keeps of which chunks have arrived
        String[] receivedChunks = null;
        int totalNumOfChunks = -1;

        while (true) {
            dpReceieve = new DatagramPacket(receive, receive.length);
            ds.receive(dpReceieve);

            FileChunking fileChunking = new FileChunking();

            StringBuilder fname = new StringBuilder();
            int fileStartIdx = byteToStr(receive, fname);
            String fnameStr = fname.toString();

            // Obtain the file name
            if (fnameStr.startsWith("Filename#")) {
                oriFileName = fnameStr.split("#")[1];
            } else if (fnameStr.startsWith("Finished")) {
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
                        for (int i = 0; i < receivedChunks.length; i++) {
                            fileChunking.joinChunks(directory + "/" + receivedChunks[i]);
                        }
                        // Send client a message indicating that the transaction succeeded
                        byte[] successCode = "ReceivedSuccess\n".getBytes();
                        DatagramPacket successMsg = new DatagramPacket(successCode, successCode.length,
                                dpReceieve.getSocketAddress());
                        ds.send(successMsg);
                    } else {
                        ExecutorService executor = Executors.newFixedThreadPool(missingFiles.size());
                        try {
                            executor.execute(new SendServerThread(missingFiles, ds, dpReceieve.getSocketAddress()));
                            executor.shutdown();
                        } catch (Exception err) {
                            err.printStackTrace();
                        }
                    }

                }

            } else {
                if (totalNumOfChunks == -1) {
                    totalNumOfChunks = fileChunking.getNumberOfChunks(fnameStr);
                    receivedChunks = new String[totalNumOfChunks];
                }

                int chunkNum = fileChunking.getChunkNumber(fnameStr);
                byte[] binaryData = Arrays.copyOfRange(receive, fileStartIdx, fileStartIdx + 2000);
                writeBinaryToFile(binaryData, fname.toString());
                receivedChunks[chunkNum] = fnameStr;

                System.out.println("Client:-" + fnameStr);
            }

        }

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
        FileOutputStream fos = new FileOutputStream("TestFilesReceive/" + fname);
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
