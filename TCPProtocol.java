import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.*;

class TCPProtocol {

    private byte[] payload = null;
    private String chunkFileName = "";
    private byte[] chunkFileNameByte = null;
    private byte[] checksum = null;

    private int chunkSize = 2000;

    public TCPProtocol(String chunkFileName, byte[] payload) {
        this.chunkFileName = chunkFileName;
        this.chunkFileNameByte = chunkFileName.getBytes();
        this.payload = payload;
    }

    public TCPProtocol(String filename, byte[] payload, byte[] checksum) {
        this.checksum = checksum;
        this.chunkFileName = filename;
        this.chunkFileNameByte = filename.getBytes();
        this.payload = payload;
    }

    /********************************
     * GETTER FUNCTIONS
     ********************************/

    /**
     * Get File Name of this protocol
     * 
     * @return a string of the filename
     */
    public String fileName() {
        return this.chunkFileName;
    }

    /**
     * Get the payload
     * 
     * @return return a byte array of payload
     */
    public byte[] payload() {
        return this.payload;
    }

    /**
     * 
     * @return the checksum for this packet
     */
    public byte[] checksum() {
        return this.checksum;
    }

    /********************************
     * END GETTER FUNCTIONS
     ********************************/

    /********************************
     * UTILITY FUNCTIONS
     ********************************/

    /**
     * Compute the checksum for this packet
     * 
     * @return a byte array containing the checksum
     */
    public byte[] computeChecksum() {
        // Check that we have things in chunkFileNameByte and payload
        if (this.chunkFileNameByte == null || this.payload == null) {
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
     * Creates a byte array of the protocol so that it can be sent to as a packet
     * Protocol is in the follow format - Filename:::Checksum:::Payload
     * 
     * @return
     */
    public byte[] packageData() {
        if (this.chunkFileNameByte == null || this.payload == null) {
            System.err.println("Filename or payload are null");
            return new byte[1];
        }

        byte[] protocol = new byte[1];

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try {
            outputStream.write(this.chunkFileNameByte);
            outputStream.write(this.computeChecksum());
            outputStream.write(this.payload);
            protocol = outputStream.toByteArray();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return protocol;

    }

    /********************************
     * END UTILITY FUNCTIONS
     ********************************/
}