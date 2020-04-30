import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.ArrayList;

class FileChunking {
    private static final int BUFSIZE = 4 * 1024;
    private InetSocketAddress addr = null;
    private static String dir = "TestFiles/";
    private static ArrayList<Integer> droppedIdx = new ArrayList<>();

    public FileChunking() {

    }

    public FileChunking(InetSocketAddress addr) {
        this.addr = addr;
        droppedIdx.add(207);
        droppedIdx.add(198);
        droppedIdx.add(161);
        droppedIdx.add(77);
        droppedIdx.add(216);
    }

    public boolean needsSplitting(String file, int chunkSize) {
        return new File(file).length() > chunkSize;
    }

    private boolean isASplitFileChunk(String file) {
        return chunkIndexLen(file) > 0;
    }

    public int chunkIndexLen(String file) {
        int n = numberOfTrailingDigits(file);
        if (n > 0) {
            String zeroes = new String(new char[n]).replace("\0", "0");
            if (file.matches(".*\\.part[0-9]{" + n + "}?of[0-9]{" + n + "}?$") && !file.endsWith(zeroes)
                    && !chunkNumberStr(file, n).equals(zeroes)) {
                return n;
            }
        }
        return 0;
    }

    private String getWholeFileName(String chunkName) {
        int n = chunkIndexLen(chunkName);
        if (n > 0) {
            return chunkName.substring(0, chunkName.length() - 7 - 2 * n); // 7+2n: 1+4+n+2+n : .part012of345
        }
        return chunkName;
    }

    public int getNumberOfChunks(String filename) {
        int n = chunkIndexLen(filename);
        if (n > 0) {
            try {
                String digits = chunksTotalStr(filename, n);
                return Integer.parseInt(digits);
            } catch (NumberFormatException x) { // should never happen
            }
        }
        return 1;
    }

    public int getChunkNumber(String filename) {
        int n = chunkIndexLen(filename);
        if (n > 0) {
            try {
                // filename.part001of200
                String digits = chunkNumberStr(filename, n);
                return Integer.parseInt(digits) - 1;
            } catch (NumberFormatException x) {
            }
        }
        return 0;
    }

    private int numberOfTrailingDigits(String s) {
        int n = 0, l = s.length() - 1;
        while (l >= 0 && Character.isDigit(s.charAt(l))) {
            n++;
            l--;
        }
        return n;
    }

    private String chunksTotalStr(String filename, int chunkIndexLen) {
        return filename.substring(filename.length() - chunkIndexLen);
    }

    protected String chunkNumberStr(String filename, int chunkIndexLen) {
        int p = filename.length() - 2 - 2 * chunkIndexLen; // 123of456
        return filename.substring(p, p + chunkIndexLen);
    }

    // 0,8 ==> part1of8; 7,8 ==> part8of8
    public String chunkFileName(String filename, int n, int total, int chunkIndexLength) {
        return filename + String.format(".part%0" + chunkIndexLength + "dof%0" + chunkIndexLength + "d", n + 1, total);
    }

    public String[] splitFile(String fname, long chunkSize, DatagramSocket ds) throws IOException {
        FileInputStream fis = null;
        Utility utility = new Utility();
        ArrayList<String> res = new ArrayList<String>();
        byte[] buffer = new byte[BUFSIZE];
        try {
            long totalSize = new File(fname).length();
            int nChunks = (int) ((totalSize + chunkSize - 1) / chunkSize);
            int chunkIndexLength = String.format("%d", nChunks).length();
            fis = new FileInputStream(fname);
            long written = 0;
            for (int i = 0; written < totalSize; i++) {
                String chunkFName = chunkFileName(fname, i, nChunks, chunkIndexLength);
                FileOutputStream fos = new FileOutputStream(chunkFName);
                try {
                    written += copyStream(fis, buffer, fos, chunkSize);
                    if (!droppedIdx.contains(i)) {
                        // Remove the directory
                        String chunkFileName = chunkFName.replace(dir, "");
                        byte[] payload = new byte[(int) chunkSize];
                        System.arraycopy(buffer, 0, payload, 0, (int) chunkSize);
                        TCPProtocol tcpProtocol = new TCPProtocol(chunkFileName, payload);
                        byte[] packagedData = tcpProtocol.packageData();

                        // byte[] combinedData = utility.createPacketObj(chunkFNameEnd, buffer);

                        // Initialise a title for sending the data
                        DatagramPacket dpSend = new DatagramPacket(packagedData, packagedData.length, addr);
                        ds.send(dpSend);
                    }

                } finally {
                    fos.close();
                }
                res.add(chunkFName);
            }
            // Send the finishing command, telling the server that
            // we have sent all our chunks
            byte[] endBuf = "Finished\n".getBytes();
            DatagramPacket dpSend = new DatagramPacket(endBuf, endBuf.length, addr);
            ds.send(dpSend);

        } finally {
            fis.close();
        }
        return res.toArray(new String[0]);
    }

    public boolean canJoinFile(String chunkName) {
        int n = chunkIndexLen(chunkName);
        if (n > 0) {
            int nChunks = getNumberOfChunks(chunkName);
            String filename = getWholeFileName(chunkName);
            for (int i = 0; i < nChunks; i++) {
                if (!new File(chunkFileName(filename, i, nChunks, n)).exists()) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    public void joinChunks(String chunkName) throws IOException {
        int n = chunkIndexLen(chunkName);
        if (n > 0) {
            int nChunks = getNumberOfChunks(chunkName);
            String filename = getWholeFileName(chunkName);
            // String filename = "TestFiles/t1_comb.gif";
            byte[] buffer = new byte[BUFSIZE];
            FileOutputStream fos = new FileOutputStream("TestFilesReceive/t1_comb.gif");
            try {
                for (int i = 0; i < nChunks; i++) {
                    FileInputStream fis = new FileInputStream(chunkFileName(filename, i, nChunks, n));
                    try {
                        copyStream(fis, buffer, fos, -1);
                    } finally {
                        fis.close();
                    }
                }
            } finally {
                fos.close();
            }
        }
    }

    public boolean deleteAllChunks(String chunkName) {
        boolean res = true;
        int n = chunkIndexLen(chunkName);
        if (n > 0) {
            int nChunks = getNumberOfChunks(chunkName);
            String filename = getWholeFileName(chunkName);
            for (int i = 0; i < nChunks; i++) {
                File f = new File(chunkFileName(filename, i, nChunks, n));
                res &= (f.delete() || !f.exists());
            }
        }
        return res;
    }

    private long copyStream(FileInputStream fis, byte[] buffer, FileOutputStream fos, long maxAmount)
            throws IOException {
        long chunkSizeWritten;
        for (chunkSizeWritten = 0; chunkSizeWritten < maxAmount || maxAmount < 0;) {
            int toRead = maxAmount < 0 ? buffer.length : (int) Math.min(buffer.length, maxAmount - chunkSizeWritten);
            int lengthRead = fis.read(buffer, 0, toRead);
            if (lengthRead < 0) {
                break;
            }
            fos.write(buffer, 0, lengthRead);
            chunkSizeWritten += lengthRead;
        }
        return chunkSizeWritten;
    }

}