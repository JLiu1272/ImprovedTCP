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

/***
 * File: Utility.java Author: Jennifer Liu
 * 
 * Objective: This file contains the functions that are used in all files. They
 * are functions like converting extract the data inside a TCP protocol etc...
 */

public class Utility {

    public int chunkSize = 5000;
    public int BUFSIZE = 5 * 1024;

    /**
     * Converts the an array of byte to a StringBuilder class
     * 
     * @param a - the byte array that you want converted
     * @return a StringBuilder object of the string
     */
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

    /***
     * Extracts the packet and creates a TCPProtocol. When server/client sends
     * message to each other, it is sent as an array of bytes. This is not very
     * useful when it reaches the client side. This function parses each byte, and
     * creates a TCPProtocol packet out of it. This way, we can easily locate the
     * filename, checksum, and payload by using the getter functions of the
     * TCPProtocol
     * 
     * @param packet - a byte array sent
     * @return - A TCPProtocol object
     */
    public TCPProtocol extractData(byte[] packet) {
        // Convert byte to string
        int payloadStart = packet.length - chunkSize;

        // This is 32 because I used SHA-32 as the method for computing
        // the checksum
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

    /**
     * A compressed function for sending a message to the destination port and
     * address, which is provided by the addr
     * 
     * @param msg  - The message to be sent
     * @param ds   - The source socket, the socket that is issuing the sending
     * @param addr - The destination, the socket that is getting this data
     * @throws IOException
     */
    public void sendMsg(String msg, DatagramSocket ds, InetSocketAddress addr) throws IOException {
        byte[] msgByte = msg.getBytes();
        DatagramPacket fnamePacket = new DatagramPacket(msgByte, msgByte.length, addr);
        ds.send(fnamePacket);
    }

    /***
     * Converts a file into binary data.
     * 
     * @param buffer   - the content of the file will be stored in this byte array
     * @param fileName - the filename of this file
     * @throws IOException
     */
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
