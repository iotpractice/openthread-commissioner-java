package com.thread.commissioner;

public class OTBRInfo {
    private String networkName;
    private String extendedPanId;
    private String otbrAddress;
    private int otbrPort;

    /**
     * @author Kuldeep Singh
     */
    public OTBRInfo(String networkName, String extendedPanId, String otbrAddress, int otbrPort) {
        this.networkName = networkName;
        this.extendedPanId = extendedPanId;
        this.otbrAddress = otbrAddress;
        this.otbrPort = otbrPort;
    }

    public OTBRInfo(String otbrAddress, int otbrPort) {
        this.otbrAddress = otbrAddress;
        this.otbrPort = otbrPort;
    }

    public String getNetworkName() {
        return networkName;
    }

    public void setNetworkName(String networkName) {
        this.networkName = networkName;
    }

    public void setExtendedPanId(String extendedPanId) {
        this.extendedPanId = extendedPanId;
    }

    public String getExtendedPanId() {
        return extendedPanId;
    }

    public String getOtbrAddress() {
        return otbrAddress;
    }

    public int getOtbrPort() {
        return otbrPort;
    }
}
