package com.thread.commissioner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceEvent;
import javax.jmdns.ServiceListener;
import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.UnknownHostException;
import java.util.Enumeration;

/**
 * @author Kuldeep Singh
 */
public class OTBRDiscoverer implements ServiceListener {

    private static final Logger logger = LoggerFactory.getLogger(OTBRDiscoverer.class);

    private static final String MDNS_SERVICE_TYPE = "_meshcop._udp.local.";
    private static final String KEY_NETWORK_NAME = "nn";
    private static final String KEY_EXTENDED_PAN_ID = "xp";
    private OTBRInfo otbrInfo;
    private JmDNS jmdns;

    public OTBRInfo getOTBRInfo() {
        return otbrInfo;
    }

    @Override
    public void serviceAdded(ServiceEvent serviceEvent) {
        logger.debug("Service added: {}", serviceEvent.getInfo());
    }

    @Override
    public void serviceRemoved(ServiceEvent serviceEvent) {
        logger.debug("Service removed: {}", serviceEvent.getInfo());
    }

    @Override
    public void serviceResolved(ServiceEvent serviceEvent) {
        logger.debug("Service resolved: {}", serviceEvent.getInfo());
        try {
            String networkName = serviceEvent.getInfo().getPropertyString(KEY_NETWORK_NAME);
            int otbrPort = serviceEvent.getInfo().getPort();
            String extPanId = Utils.getHexString(serviceEvent.getInfo().getPropertyBytes(KEY_EXTENDED_PAN_ID));
            if(serviceEvent.getInfo().getInet4Addresses().length>0){
                String otbrAddress = serviceEvent.getInfo().getInet4Addresses()[0].getHostAddress();
                otbrInfo = new OTBRInfo(networkName, extPanId, otbrAddress, otbrPort);
                logger.info("Service resolved: {}:{} {} {}", otbrAddress, otbrPort, networkName, extPanId);
            }
        } catch (Exception e) {
            logger.error("Failed to resolve service: {}", e.getMessage());
        } finally {
           close();
        }
    }

    public OTBRDiscoverer() {
        try {
            jmdns = JmDNS.create(getLocalHostIP());
            jmdns.addServiceListener(MDNS_SERVICE_TYPE, this);
            logger.info("Discovering Border Router at {}", MDNS_SERVICE_TYPE);
        } catch (IOException e) {
            logger.warn("Failed to create JmDNS {}", e.getMessage());
        }
    }

    public void close(){
        if(jmdns!=null){
            try {
                jmdns.close();
            } catch (IOException e) {
                logger.error("Failed to close JNS service: {}", e.getMessage());
            }
        }
    }

    private InetAddress getLocalHostIP() throws UnknownHostException {
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface netInterface = interfaces.nextElement();

                // Ignore loopback and inactive interfaces
                if (netInterface.isLoopback() || !netInterface.isUp()) {
                    continue;
                }

                Enumeration<InetAddress> addresses = netInterface.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress address = addresses.nextElement();
                    // Ignore loopback and IPv6 addresses
                    if (!address.isLoopbackAddress() && address.getAddress().length == 4) {
                        logger.debug("Local IP Address: {}", address.getHostAddress());
                        return address; // Stop after finding the first valid IP
                    }
                }
            }
        } catch (Exception e) {
            logger.warn("Failed to get Local IP address: {}" , e.getMessage());
        }
        return InetAddress.getLocalHost();
    }


}
