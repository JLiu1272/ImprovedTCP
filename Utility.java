import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

public class Utility {

    public int chunkSize = 2000;
    public int BUFSIZE = 4 * 1024;

    // A utility method to convert the byte array
    // data into a string representation.
    // Returns the last byte index that is part of the filename
    public StringBuilder byteToStr(byte[] a) {
        if (a == null)
            return null;
        StringBuilder ret = new StringBuilder();
        int i = 0;
        while (a[i] != 0) {
            char letter = (char) a[i];
            ret.append(letter);
            i++;
        }
        return ret;
    }

    public TCPProtocol extractData(byte[] packet) {
        // Convert byte to string
        int payloadStart = packet.length - chunkSize;
        int checksumStart = payloadStart - 32;
        byte[] payload = Arrays.copyOfRange(packet, payloadStart, packet.length);
        byte[] checksum = Arrays.copyOfRange(packet, checksumStart, payloadStart);
        String filename = new String(Arrays.copyOfRange(packet, 1, checksumStart));
        Boolean resend = packet[0] == 1;

        return new TCPProtocol(filename, payload, checksum, resend);
    }

    /**
     * Compute the checksum for this packet
     * 
     * @return a byte array containing the checksum
     */
    public byte[] computeChecksum(String chunkFileName, byte[] payload) {
        byte[] chunkFileNameByte = chunkFileName.getBytes();
        // Check that we have things in chunkFileNameByte and payload
        if (chunkFileNameByte == null || payload == null) {
            System.err.println("Payload and filename is empty");
            return new byte[1];
        }

        byte[] combinedData = new byte[1];

        // Convert the filename to byte array
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            outputStream.write(chunkFileNameByte);
            outputStream.write(payload);
            combinedData = outputStream.toByteArray();
            return md.digest(combinedData);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

        return combinedData;
    }

    public void sendMsg(String msg, DatagramSocket ds, InetSocketAddress addr) throws IOException {
        byte[] msgByte = msg.getBytes();
        DatagramPacket fnamePacket = new DatagramPacket(msgByte, msgByte.length, addr);
        ds.send(fnamePacket);
    }

    public void fileToBinary(byte[] buffer, String fileName) throws IOException {
        FileInputStream fis = new FileInputStream(new File(fileName));
        try {
            fis.read(buffer);
        } catch (FileNotFoundException fileErr) {
            System.err.println("File does not exist");
        } finally {
            fis.close();
        }
    }
}
