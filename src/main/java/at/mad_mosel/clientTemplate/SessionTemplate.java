package at.mad_mosel.clientTemplate;

import java.io.*;
import java.net.Socket;
import java.net.SocketException;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Timer;
import java.util.TimerTask;

public abstract class SessionTemplate {
    private static final String msgPrefix = "Session: ";


    private Client client;
    Socket socket;
    Thread processor;
    Thread receiver;
    ObjectOutputStream dataOut;


    protected final Queue<Serializable> dataQueue = new LinkedList<>();

    Runnable processesData = () -> {
        printInfo("Processing thread starting...");
        try {
            dataOut = new ObjectOutputStream(socket.getOutputStream());
            processData(null);
            while (true) {
                Serializable data;
                synchronized (dataQueue) {
                    while (dataQueue.isEmpty()) dataQueue.wait();
                    data = dataQueue.remove();
                }
                this.processData(data);
            }
        } catch (InterruptedException ie) {

        } catch (Exception e) {
            onError(e);
        }
        printInfo("Processor shutting down...");
    };

    Runnable receiveData = () -> {
        receive();
    };

    protected void init(Socket socket, Client server) throws IOException {
        this.client = server;
        this.socket = socket;
        userInit();
        printInfo("Launching processor thread...");
        this.socket = socket;
        processor = new Thread(processesData);
        processor.start();
        printInfo("Processor up.");
        receiver = new Thread(receiveData);
        receiver.start();
    }

    /**
     * This method is meant to help customizing the fields
     * of InputStream dataIn and OutputStream dataOut. Do
     * something like:
     * * dataIn = new BufferedInputStream(dataIn);
     * * dataOut = new BufferedInputStream(dataOut);
     * The fields will be Initialized at the moment this
     * member is called. Do not call this from your Code.
     */
    public abstract void userInit();


    private void receive() {
        printInfo("Listening for input...");
        try {
            ObjectInputStream dataIn = new ObjectInputStream(socket.getInputStream());
            while (true) {
                Serializable data = (Serializable) dataIn.readObject();
                synchronized (dataQueue) {
                    dataQueue.add(data);
                    dataQueue.notify();
                }
            }
        } catch (SocketException | EOFException se) {
            printVerbose("Receiver - socket closed");
        } catch (Exception e) {
            onError(e);
        }
        printInfo("Receiver shutting down...");
    }


    /**
     * This will run in an endless loop within a Thread.
     * Implement whatever Server shall do in on receive
     * here.
     */
    public synchronized void send(Serializable data) {
        try {
            dataOut.writeObject(data);
        } catch (IOException e) {
            if (Client.logger.exception) e.printStackTrace();
            printDebug("Failed on send. Trying kill()...");
            kill();
        }
    }

    /**
     * This will run in an endless loop within a Thread.
     */
    public abstract void processData(Serializable object) throws Exception;


    private void printInfo(String msg) {
        Client.logger.printInfo(msgPrefix + msg);
    }

    private void printDebug(String msg) {
        Client.logger.printDebug(msgPrefix + msg);
    }

    private void printVerbose(String msg) {
        Client.logger.printVerbose(msgPrefix + msg);
    }


    private void onError(Exception e) {
        if (Client.logger.debug) e.printStackTrace();
        printDebug(e.getMessage());

        try {
            socket.close();
            processor.interrupt();
        } catch (Exception x) {
            x.printStackTrace();
            printDebug(x.getMessage());
            printDebug("This should really not happen! Killing app for safety!");
            printInfo("There was some issue with terminating the processor thread.");
            System.exit(-1);
        }
        kill();
        client.removeSession(this);
    }

    public void kill() {
        try {
            Timer timer = new Timer();
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    throw new IllegalStateException("Failed to cancel Session");
                }
            }, 3000);

            if (!socket.isClosed()) {
                socket.close();
                printVerbose("Socket successfully closed");
            }
            processor.interrupt();
            timer.cancel();
            printInfo("Killed Session.");
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(-1);
        }
        client.removeSession(this);
    }
}
