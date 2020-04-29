import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;

public class Utility {

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

    public byte[] createPacketObj(String chunkFName, byte[] buffer) {
        // We want to send the binary data and additionally, we want
        // to send the file name. We use "::::" to indicate that
        // the first segment is for filename, and the remaining segment
        // is for binary data
        String chunkFNameEnd = chunkFName + "::::";
        byte bufName[] = chunkFNameEnd.getBytes();
        byte[] combinedData = new byte[bufName.length + buffer.length];
        System.arraycopy(bufName, 0, combinedData, 0, bufName.length);
        System.arraycopy(buffer, 0, combinedData, bufName.length, buffer.length);
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
    }}

    

    

    

    

    

    
    


