package at.mad_mosel.clientTemplate;

import at.mad_mosel.ConfigParser;
import at.mad_mosel.Configuration;

import javax.net.SocketFactory;

public class Client {
    boolean printDebug = false;
    boolean printVerbose = false;
    boolean printException = true;
    boolean printInfo = true;

    String ip = "127.0.0.1";
    int port = 8001;
    boolean tls = false;
    SocketFactory ssf = SocketFactory.getDefault();


    private void parseConfigAndInsertMissing() {
        ConfigParser configParser = new ConfigParser("server.conf");
        configParser.readFile();

        Configuration pd = configParser.getConfiguration("printDebug");
        if (pd != null && pd.getValue().equals("true")) printDebug = true;
        else if (pd != null && pd.getValue().equals("false")) printDebug = false;
        else configParser.addConfiguration("printDebug", Boolean.toString(printDebug), "true", "false");

        Configuration pv = configParser.getConfiguration("printVerbose");
        if (pd != null && pd.getValue().equals("true")) printVerbose = true;
        else if (pd != null && pd.getValue().equals("false")) printVerbose = false;
        else configParser.addConfiguration("printVerbose", Boolean.toString(printVerbose), "true", "false");

        Configuration pi = configParser.getConfiguration("printInfo");
        if (pi != null && pi.getValue().equals("true")) printInfo = true;
        else if (pi != null && pi.getValue().equals("false")) printInfo = false;
        else {
            configParser.addConfiguration("printInfo", Boolean.toString(printInfo), "true", "false");
        }

        Configuration pe = configParser.getConfiguration("printException");
        if (pe != null && pe.getValue().equals("true")) printException = true;
        else if (pe != null && pe.getValue().equals("false")) printException = false;
        else {
            configParser.addConfiguration("printException", Boolean.toString(printException), "true", "false");
        }

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
                Configuration passwd = configParser.getConfiguration("password");
                if (passwd == null)
                    throw new IllegalStateException("TSL but no password specified! Check config file!");
                this.ssf = TLS13SocketFactory.getTLS13SocketFactory(certPath.getValue(), passwd.getValue());
            } catch (IllegalStateException ise) {
                if (!configParser.containsKeys("certPath")) configParser.addConfiguration("certPath");
                if (!configParser.containsKeys("password"))configParser.addConfiguration("password");
                configParser.saveConfigs();
                ise.printStackTrace();
                System.exit(-1);
            } catch (Exception e) {
                e.printStackTrace();
                System.exit(-1);
            }
        }
        if (!configParser.containsKeys("certPath")) configParser.addConfiguration("certPath");
        if (!configParser.containsKeys("password"))configParser.addConfiguration("password");
        configParser.saveConfigs();
    }
}
