import java.io.IOException;

class Client {

    public static void main(String args[]) throws IOException {
        FileChunking fileChunker = new FileChunking();
        FileStatistics fileStatistics = new FileStatistics();

        String fname1 = "TestFiles/t1.gif";
        int chunkSize1 = 500;

        String[] fileChunks = fileChunker.splitFile(fname1, 2000);
        // fileStatistics.printChunkNames(fileChunks, 10);

        for (int i = 0; i < fileChunks.length; i++) {
            fileChunker.joinChunks(fileChunks[i]);
        }

    }
}