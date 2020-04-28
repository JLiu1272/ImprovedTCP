import java.io.FileOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Arrays;

class Server {

    private static int chunkSize = 0;

    public static void main(String args[]) throws IOException {

        int destPort = 3000;
        DatagramSocket ds = new DatagramSocket(destPort);

        byte[] receive = new byte[65535];
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

            if (totalNumOfChunks == -1) {
                totalNumOfChunks = fileChunking.getNumberOfChunks(fnameStr);
                receivedChunks = new String[totalNumOfChunks];
            }

            int chunkNum = fileChunking.getChunkNumber(fnameStr);
            receivedChunks[chunkNum] = fnameStr;

            byte[] binaryData = Arrays.copyOfRange(receive, fileStartIdx, receive.length);
            writeBinaryToFile(binaryData, fname.toString().split("/")[1]);

            System.out.println("Client:-" + fnameStr);
        }

    }

    private static int getTotalNumChunks(String fname) {
        String[] tokens = fname.split("of");

        if (tokens.length == 2) {
            return Integer.parseInt(tokens[1]);
        }
        return -1;
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
                i++;
                numColon = 1;
            }
        }
        return i;
    }
}
