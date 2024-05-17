import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.Set;

interface HTTPServerHandler {
    void handle(SelectionKey key) throws IOException;
}

public class SimpleNIOHTTPServer implements HTTPServerHandler {
    private String bindAddress;
    private int bindPort;
    private ServerSocketChannel serverSocketChannel;
    private Selector selector;

    public SimpleNIOHTTPServer(String bindAddress, int bindPort) throws IOException {
        this.bindAddress = bindAddress;
        this.bindPort = bindPort;
        this.serverSocketChannel = ServerSocketChannel.open();
        this.selector = Selector.open();
        serverSocketChannel.socket().bind(new InetSocketAddress(bindAddress, bindPort));
        serverSocketChannel.configureBlocking(false);
        serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
    }

    public void run() {
        System.out.println("Server started");
        System.out.println("Running on " + bindAddress + ":" + bindPort);
        while (true) {
            try {
                selector.select();
                Set<SelectionKey> selectedKeys = selector.selectedKeys();
                Iterator<SelectionKey> iter = selectedKeys.iterator();
                while (iter.hasNext()) {
                    SelectionKey key = iter.next();
                    if (key.isAcceptable()) {
                        register(selector, serverSocketChannel);
                    }
                    if (key.isReadable()) {
                        handle(key);
                    }
                    iter.remove();
                }
            } catch (IOException e) {
                System.err.println("Web Server failed " + e);
            }
        }
    }

    private void register(Selector selector, ServerSocketChannel serverSocketChannel) throws IOException {
        SocketChannel client = serverSocketChannel.accept();
        System.out.println("receved request from " + client.getRemoteAddress());
        client.configureBlocking(false);
        client.register(selector, SelectionKey.OP_READ);
    }

    //@Override
    public void handle(SelectionKey key) throws IOException {
        SocketChannel client = (SocketChannel) key.channel();
        ByteBuffer buffer = ByteBuffer.allocate(1024);
        int numRead = client.read(buffer);

        if (numRead == -1) {
            client.close();
            return;
        }

        String request = new String(buffer.array()).trim();
        if (request.isEmpty()) {
            client.close();
            return;
        }

        String[] requestLines = request.split("\r\n");
        String[] requestLine = requestLines[0].split(" ");
        String method = requestLine[0];
        String path = requestLine[1];

        if (method.equals("GET")){
            handleGetRequest(client, path);
        } else if (method.equals("POST")) {
            handlePostRequest(client, requestLines);
        }
    }

    private void handleGetRequest(SocketChannel client, String path) throws IOException {
        String resourcePath;
    
        // Explicitly handle the /register path
        if (path.equals("/")) {
            resourcePath = "templates/index.html";
        } else if (path.equals("/register")) {
            resourcePath = "templates/register.html";
        } else {
            resourcePath = "templates" + path + ".html";
        }
    
        if (Files.exists(Paths.get(resourcePath))) {
            String response = "HTTP/1.1 200 OK\r\nContent-Type: text/html\r\n\r\n" + new String(Files.readAllBytes(Paths.get(resourcePath)));
            client.write(ByteBuffer.wrap(response.getBytes()));
        } else {
            String response = "HTTP/1.1 404 Not Found\r\nContent-Type: text/html\r\n\r\n<html><body><h1>404 - Not Found/ Palibe</h1></body></html>";
            client.write(ByteBuffer.wrap(response.getBytes()));
        }
        client.close();
    }
    

    private void handlePostRequest(SocketChannel client, String[] requestLines) throws IOException {
        StringBuilder requestBody = new StringBuilder();
        boolean isBody = false;

        for (String line : requestLines) {
            if (line.isEmpty()) {
                isBody = true;
                continue;
            }
            if (isBody) {
                requestBody.append(line);
            }
        }

        String[] params = requestBody.toString().split("&");
        String username = "";
        String email = "";

        for (String param : params) {
            String[] keyValue = param.split("=");
            if (keyValue[0].equals("username")) {
                username = keyValue[1];
                //belongs to inncoent waluza
            } else if (keyValue[0].equals("email")) {
                email = keyValue[1];
            }
        }

        RandomAccessFile dbFile = new RandomAccessFile("db.txt", "rw");
        dbFile.seek(dbFile.length());
        dbFile.writeBytes(username + " " + email + "\n");
        dbFile.close();

        String response = "HTTP/1.1 200 OK\r\nContent-Type: text/plain\r\n\r\nPOST request received successfully!";
        client.write(ByteBuffer.wrap(response.getBytes()));
        client.close();
    }

    public static void main(String[] args) {
        try {
            SimpleNIOHTTPServer server = new SimpleNIOHTTPServer("localhost", 8085);
            server.run();
        } catch (IOException e) {
            System.err.println("Web Server failed: " + e);
        }
    }
}
