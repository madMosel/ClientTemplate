package at.mad_mosel.clientTemplate;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.jsse.provider.BouncyCastleJsseProvider;

import javax.net.SocketFactory;
import javax.net.ssl.*;
import java.io.FileInputStream;
import java.net.InetSocketAddress;
import java.security.*;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

public class TLS13SocketFactory {
    private static SocketFactory ssf;

    private static void setupTLS13SocketFactory(String certPath) throws Exception {
        if (ssf != null) return;
        //Add BouncyCastle Repo:
        Security.addProvider(new BouncyCastleProvider());       //BouncyCastle Security Provider v1.72
        Security.addProvider(new BouncyCastleJsseProvider());   //Bouncy Castle JSSE Provider Version 1.0.13

        CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509", "BC");
        X509Certificate serverCrt = (X509Certificate) certificateFactory.generateCertificate(
                new FileInputStream(certPath));

        KeyStore clientKeyStore = KeyStore.getInstance("PKCS12");

        //TrustManager is used to verify trusted Instances
        TrustManagerFactory clientTrustManagerFactory = TrustManagerFactory.getInstance("PKIX", "BCJSSE");
        clientKeyStore.setCertificateEntry("serverCert", serverCrt);
        clientTrustManagerFactory.init(clientKeyStore);

        KeyManagerFactory clientKeyManagerFactory = KeyManagerFactory.getInstance("PKIX", "BCJSSE");
        TrustManager[] clientTrustManagers = clientTrustManagerFactory.getTrustManagers();

        KeyManager[] clientKeyManagers = clientKeyManagerFactory.getKeyManagers();

        //Context are the keys, trusted certificates and crypto-algorithms
        SSLContext clientSSLContext = SSLContext.getInstance("TLSv1.3", "BCJSSE");
        clientSSLContext.init(clientKeyManagers, clientTrustManagers, new SecureRandom());
        SSLSocketFactory clientSSLSocketFactory = clientSSLContext.getSocketFactory();

        SSLParameters clientSSLParameters = new SSLParameters();
        clientSSLParameters.setCipherSuites(new String[]{"TLS_CHACHA20_POLY1305_SHA256"});
        clientSSLParameters.setProtocols(new String[]{"TLSv1.3"});

        ssf = clientSSLSocketFactory;
    }

    public static SSLSocket produceTslSocket(String certPath, String ip, int port) throws Exception {
        setupTLS13SocketFactory(certPath);
        SSLParameters clientSSLParameters = new SSLParameters();
        clientSSLParameters.setCipherSuites(new String[]{"TLS_CHACHA20_POLY1305_SHA256"});
        clientSSLParameters.setProtocols(new String[]{"TLSv1.3"});
        SSLSocket clientSSLSocket = (SSLSocket) ssf.createSocket();
        clientSSLSocket.setSSLParameters(clientSSLParameters);
        clientSSLSocket.setSSLParameters(clientSSLParameters);
        clientSSLSocket.connect(new InetSocketAddress(ip, port));
        clientSSLSocket.startHandshake();
        clientSSLSocket.setTcpNoDelay(true);
        return clientSSLSocket;
    }
}
