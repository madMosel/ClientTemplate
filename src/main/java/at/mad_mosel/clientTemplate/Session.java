package at.mad_mosel.clientTemplate;

import java.io.*;
import java.net.Socket;
import java.util.LinkedList;
import java.util.Queue;

public abstract class Session {
    private static final String msgPrefix = "Session: ";

    boolean shutdownSwitch = false;

    Socket socket;
    Thread processor;
    ObjectOutputStream dataOut;


    protected final Queue<Serializable> dataQueue = new LinkedList<>();

    Runnable processesData = () -> {
        printInfo("Processing thread starting...");
        try {
            dataOut = new ObjectOutputStream(socket.getOutputStream());
            processData(null);
            while (!shutdownSwitch) {
                Serializable data;
                synchronized (dataQueue) {
                    while (dataQueue.isEmpty()) dataQueue.wait();
                    data = dataQueue.remove();
                }
                this.processData(data);
            }
        } catch (Exception e) {
            Client.logger.printException(e.getMessage());
            if (Client.logger.debug) e.printStackTrace();
            printInfo("Processor shutting down...");
        }
    };

    protected void init(Socket socket) throws IOException {
        printInfo("Launching processor thread...");
        this.socket = socket;
        processor = new Thread(processesData);
        processor.start();
        printInfo("Processor up.");
        userInit();
        receive();
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
            while (!shutdownSwitch) {
                Serializable data = (Serializable) dataIn.readObject();
                synchronized (dataQueue) {
                    dataQueue.add(data);
                    dataQueue.notify();
                }
            }
        } catch (Exception e) {
            Client.logger.printException(e.getMessage());
            if (Client.logger.debug) e.printStackTrace();
            printInfo("Receiver shutting down...");
        }
    }


    /**
     * This will run in an endless loop within a Thread.
     * Implement whatever Server shall do in on receive
     * here.
     */
    public void send(Serializable data) {
        try {
            dataOut.writeObject(data);
        } catch (IOException e) {
            if (Client.logger.debug) Client.logger.printException(e.getMessage() + "while sending" + data);
            if (Client.logger.exception) e.printStackTrace();
        }
    }

    /**
     * This will run in an endless loop within a Thread.
     */
    public abstract void processData(Serializable object) throws Exception;

    private void printInfo(String msg) {
        Client.logger.printInfo(msgPrefix + msg);
    }

}
