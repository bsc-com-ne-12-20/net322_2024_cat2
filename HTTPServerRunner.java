/*public class HTTPServerRunner {

    public static void main(String[] args) {

        String bindAddress; // Initialize with first commandline program argument
        int bindPort; // Initialize with second commandline program argument

        SimpleNIOHTTPServer simpleNioHttpServer = new SimpleNIOHTTPServer(bindAddress, bindPort);
        simpleNioHttpServer.run();
    }
}*/
import java.io.*;
public class HTTPServerRunner {
    public static void main(String[] args) {
        if (args.length != 2) {
            System.out.println("Usage: java HTTPServerRunner <bindAddress> <bindPort>");
            return;
        }

        String bindAddress = args[0];
        int bindPort = Integer.parseInt(args[1]);

        try {
            SimpleNIOHTTPServer server = new SimpleNIOHTTPServer(bindAddress, bindPort);
            server.run();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

