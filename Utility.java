import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
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
        String filename = new String(Arrays.copyOfRange(packet, 0, checksumStart));

        return new TCPProtocol(filename, payload, checksum);
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
