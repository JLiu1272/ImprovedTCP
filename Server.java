import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;

class Server {

    public static void main(String args[]) throws IOException {

        // System.out.println("Server");
        int destPort = 3000;
        DatagramSocket ds = new DatagramSocket(destPort);

        byte[] receive = new byte[65535];
        DatagramPacket dpReceieve = null;

        while (true) {
            dpReceieve = new DatagramPacket(receive, receive.length);
            ds.receive(dpReceieve);
            System.out.println("Client:-" + data(receive));
        }

    }

    // A utility method to convert the byte array
    // data into a string representation.
    public static StringBuilder data(byte[] a) {
        if (a == null)
            return null;
        StringBuilder ret = new StringBuilder();
        int i = 0;
        while (a[i] != 0) {
            ret.append((char) a[i]);
            i++;
        }
        return ret;
    }
}