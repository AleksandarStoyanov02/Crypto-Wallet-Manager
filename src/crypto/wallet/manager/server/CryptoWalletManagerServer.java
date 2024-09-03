package crypto.wallet.manager.server;

import crypto.wallet.manager.api.Api;
import crypto.wallet.manager.api.ApiRunnable;
import crypto.wallet.manager.commands.CommandExecutor;
import crypto.wallet.manager.database.CryptoCoinsDatabase;
import crypto.wallet.manager.database.UserAccountsDatabase;
import crypto.wallet.manager.exceptions.ParseException;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static crypto.wallet.manager.commands.Command.newCommand;

public class CryptoWalletManagerServer {
    private static final int SERVER_PORT = 8888;
    private static final String SERVER_HOST = "localhost";
    private static final String DISCONNECT = "disconnect";
    private static final int BUFFER_SIZE = 65536;
    private static final int TIME_BETWEEN_API_REQUESTS = 30;
    private static final String ACCOUNTS_PATH = "database" + File.separator + "accounts.dat";

    private static final int INITIAL_DELAY = 0;
    private boolean isServerWorking;
    private int connectedClients;
    private ByteBuffer buffer;
    private final CommandExecutor commandExecutor;
    private static CryptoWalletManagerServer instance;

    private CryptoWalletManagerServer(CommandExecutor commandExecutor, boolean isServerWorking) {
        this.commandExecutor = commandExecutor;
        this.isServerWorking = isServerWorking;
        connectedClients = 0;
    }

    public static CryptoWalletManagerServer getInstance() throws IOException {
        if (instance == null) {
            instance = new CryptoWalletManagerServer(
                    CommandExecutor.getInstance(UserAccountsDatabase.getInstance(ACCOUNTS_PATH),
                            new CryptoCoinsDatabase()),
                    true);
        }

        return instance;
    }

    public void startServer() {
        try (ServerSocketChannel serverSocketChannel = ServerSocketChannel.open()) {
            Selector selector = Selector.open();
            configureServerSocketChannel(serverSocketChannel, selector);
            buffer = ByteBuffer.allocateDirect(BUFFER_SIZE);
            isServerWorking = true;
            while (isServerWorking) {
                int readyChannels = selector.select();
                if (readyChannels == 0) {
                    continue;
                }

                Set<SelectionKey> selectedKeys = selector.selectedKeys();
                Iterator<SelectionKey> keyIterator = selectedKeys.iterator();
                while (keyIterator.hasNext()) {
                    SelectionKey key = keyIterator.next();
                    if (key.isReadable()) {
                        handleReadableKey(key);
                    } else if (key.isAcceptable()) {
                        accept(selector, key);
                    }
                    keyIterator.remove();
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("There is a problem with the server socket", e);
        } catch (Exception e) {
            throw new RuntimeException("AutoCloseable object threw exception");
        }
    }

    private void handleReadableKey(SelectionKey key) throws IOException {
        SocketChannel sc = (SocketChannel) key.channel();
        try {
            String clientInput = readClientInput(sc, key);
            if (clientInput == null || !key.isValid()) {
                return;
            }
            String response = null;
            try {
                response = commandExecutor.execute(newCommand(clientInput), key);
            } catch (Exception e) {
                throw new ParseException("Exception when parsing the clientInput", e);
            } finally {
                sendResponseToClient(sc, response);
            }
        } catch (IOException e) {
            if (e.getMessage().contains("Connection reset")) {
                handleDisconnect(sc, key);
            }
        }
    }

    private void configureServerSocketChannel(ServerSocketChannel channel, Selector selector) throws IOException {
        channel.bind(new InetSocketAddress(SERVER_HOST, SERVER_PORT));
        channel.configureBlocking(false);
        channel.register(selector, SelectionKey.OP_ACCEPT);
    }

    private String readClientInput(SocketChannel sc, SelectionKey key) throws IOException {
        buffer.clear();
        int r = sc.read(buffer);
        if (r < 0) {
            handleDisconnect(sc, key);
            return null;
        }

        buffer.flip();
        byte[] byteArray = new byte[buffer.remaining()];
        buffer.get(byteArray);
        return new String(byteArray, StandardCharsets.UTF_8);
    }

    private void handleDisconnect(SocketChannel sc, SelectionKey key) throws IOException {
        commandExecutor.execute(newCommand(DISCONNECT), key);
        sc.close();
        key.cancel();

        connectedClients--;
        if (connectedClients == 0) {
            System.out.println("No clients connected. Server is stopping.");
            isServerWorking = false;
        }

        System.out.println("Client disconnected. Remaining clients: " + connectedClients);
    }

    private void sendResponseToClient(SocketChannel sc, String response) throws IOException {
        if (response == null) {
            response = "There was a problem with reading your input. Try again.";
        }

        buffer.clear();
        buffer.put(response.getBytes());
        buffer.flip();
        sc.write(buffer);
        if (response.equals("disconnect")) {
            sc.close();
        } else if ("shutdown".equals(response) && connectedClients == 0) {
            sc.close();
            isServerWorking = false;
        }
    }

    private void accept(Selector selector, SelectionKey key) throws IOException {
        ServerSocketChannel sockChannel = (ServerSocketChannel) key.channel();
        SocketChannel accept = sockChannel.accept();

        accept.configureBlocking(false);
        accept.register(selector, SelectionKey.OP_READ);

        connectedClients++;
    }

    public void start(Api api) {
        try (ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(1)) {
            Runnable apiRunnable = new ApiRunnable(api, commandExecutor.getCryptoCoinsDatabase());
            Thread apiThread = new Thread(apiRunnable);
            apiRunnable.run();
            scheduledExecutorService.scheduleAtFixedRate(apiThread,
                    INITIAL_DELAY, TIME_BETWEEN_API_REQUESTS, TimeUnit.MINUTES);
            startServer();
        } catch (RuntimeException e) {
            throw new RuntimeException("Problem occurred with client communication or API request.", e);
        }
    }
}
