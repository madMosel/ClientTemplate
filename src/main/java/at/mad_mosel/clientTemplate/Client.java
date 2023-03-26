package at.mad_mosel.clientTemplate;

import at.mad_mosel.ConfigParser;
import at.mad_mosel.Configuration;
import at.mad_mosel.Logger.Logger;

import java.lang.reflect.Constructor;
import java.net.Socket;
import java.util.ArrayList;

public class Client {
    protected static Logger logger = new Logger();

    boolean printDebug = false;
    boolean printVerbose = false;
    boolean printException = true;
    boolean printInfo = true;

    String ip = "127.0.0.1";
    int port = 8001;
    boolean tls = false;
    String certPath;

    public ArrayList<SessionTemplate> sessions = new ArrayList<>();

    public Client() {
        parseAndInsertMissingConfig();
    }

    public SessionTemplate startSession(Constructor sessionConstructor) {
        try {
            SessionTemplate session = (SessionTemplate) sessionConstructor.newInstance();
            Socket socket = null;
            if (tls) {
                socket = TLS13SocketFactory.produceTslSocket(certPath, ip, port);
                Thread.sleep(100);
            }
            else {
                socket = new Socket(ip, port);
            }
            session.init(socket, this);
            sessions.add(session);

            Thread.sleep(50);
            return session;
        } catch (Exception e) {
            logger.printException(e.getMessage());
            if (printDebug) e.printStackTrace();
            return null;
        }
    }


    protected void removeSession(SessionTemplate session) {
        synchronized (this.sessions) {
            sessions.remove(session);
        }
    }

    private void parseAndInsertMissingConfig() {
        ConfigParser configParser = new ConfigParser("client.conf");
        configParser.readFile();

        Configuration pd = configParser.getConfiguration("printDebug");
        if (pd != null && pd.getValue().equals("true")) logger.debug = true;
        else if (pd != null && pd.getValue().equals("false")) logger.debug = false;
        else configParser.addConfiguration("printDebug", Boolean.toString(printDebug), "true", "false");

        Configuration pv = configParser.getConfiguration("printVerbose");
        if (pd != null && pd.getValue().equals("true")) logger.verbose = true;
        else if (pd != null && pd.getValue().equals("false")) logger.verbose = false;
        else configParser.addConfiguration("printVerbose", Boolean.toString(printVerbose), "true", "false");

        Configuration pi = configParser.getConfiguration("printInfo");
        if (pi != null && pi.getValue().equals("true")) logger.info = true;
        else if (pi != null && pi.getValue().equals("false")) logger.info = false;
        else {
            configParser.addConfiguration("printInfo", Boolean.toString(printInfo), "true", "false");
        }

        Configuration pe = configParser.getConfiguration("printException");
        if (pe != null && pe.getValue().equals("true")) logger.exception = true;
        else if (pe != null && pe.getValue().equals("false")) logger.exception = false;
        else {
            configParser.addConfiguration("printException", Boolean.toString(printException), "true", "false");
        }

        Configuration ip = configParser.getConfiguration("ip");
        if (ip != null) this.ip = ip.getValue();
        else configParser.addConfiguration("ip", this.ip);

        Configuration port = configParser.getConfiguration("port");
        if (port != null) this.port = Integer.parseInt(port.getValue());
        else configParser.addConfiguration("port", Integer.toString(this.port), "\\d*");

        Configuration tlsConfig = configParser.getConfiguration("tls");
        if (tlsConfig != null && tlsConfig.getValue().equals("true")) tls = true;
        else if (tlsConfig != null && tlsConfig.getValue().equals("false")) tls = false;
        else configParser.addConfiguration("tls", Boolean.toString(tls), "true", "false");

        if (tls) {
            try {
                Configuration certPath = configParser.getConfiguration("certPath");
                if (certPath == null) throw new IllegalStateException("TSL but no cert specified! Check config file!");
                this.certPath = certPath.getValue();
            } catch (IllegalStateException ise) {
                if (!configParser.containsKeys("certPath")) configParser.addConfiguration("certPath");
                configParser.saveConfigs();
                ise.printStackTrace();
                System.exit(-1);
            } catch (Exception e) {
                e.printStackTrace();
                System.exit(-1);
            }
        }
        if (!configParser.containsKeys("certPath")) configParser.addConfiguration("certPath");
        configParser.saveConfigs();
    }
}
