import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * File: TCPProtocol.java Author: Jennifer Liu
 * 
 * Objective: This is a class that structures how the TCP packet is formed. It
 * is my protocol.
 * 
 * Protocol works like so Resend Bit (1 Byte), chunkFileName (Variable Length),
 * Checksum (32 Byte), Payload (ChunkSize)
 */
class TCPProtocol {

    private byte[] payload = null;
    private String chunkFileName = "";
    private byte[] chunkFileNameByte = null;
    private byte[] checksum = null;
    private Boolean resend = false;

    /**
     * Initialises the TCPProtocol. When this packet is packaged, it will compute
     * the checksum
     * 
     * @param chunkFileName
     * @param payload
     * @param resend
     */
    public TCPProtocol(String chunkFileName, byte[] payload, Boolean resend) {
        this.chunkFileName = chunkFileName;
        this.chunkFileNameByte = chunkFileName.getBytes();
        this.payload = payload;
        this.resend = resend;
    }

    /**
     * There is already a checksum so update the checksum for this class
     * 
     * @param filename
     * @param payload
     * @param checksum
     * @param resend
     */
    public TCPProtocol(String filename, byte[] payload, byte[] checksum, Boolean resend) {
        this.checksum = checksum;
        this.chunkFileName = filename;
        this.chunkFileNameByte = filename.getBytes();
        this.payload = payload;
        this.resend = resend;
    }

    /**
     * Converts a boolean to a byte array. Used for the resend bit for easier
     * understanding.
     * 
     * @param resend
     * @return
     */
    public byte[] boolToByteArr(Boolean resend) {
        return new byte[] { (byte) (resend ? 1 : 0) };
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

    /**
     * Indicates whether this TCP packet was a resend. Resend happens when packet
     * was lost in the first round of sendind data so it needs to be resent
     * 
     * @return
     */
    public Boolean isResend() {
        return this.resend;
    }

    /********************************
     * END GETTER FUNCTIONS
     ********************************/

    /********************************
     * UTILITY FUNCTIONS
     ********************************/

    /**
     * Creates a byte array of the protocol so that it can be sent to as a packet
     * Protocol is in the follow format -
     * ResendState:::Filename:::Checksum:::Payload
     * 
     * @return
     */
    public byte[] packageData(Boolean corrupt) {
        // If the payload or the chunkFileNameByte is null,
        // we cannot package this data
        if (this.chunkFileNameByte == null || this.payload == null) {
            System.err.println("Filename or payload are null");
            return new byte[1];
        }

        byte[] protocol = new byte[1];
        Utility utility = new Utility();

        // Write this package to the hard disk
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try {
            outputStream.write(boolToByteArr(resend));
            outputStream.write(this.chunkFileNameByte);
            outputStream.write(utility.computeChecksum(chunkFileName, payload));

            // Used for simulating corrupted data. Will only
            // run if the corrupt state is set to True
            if (corrupt)
                this.payload[0] = 22;

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