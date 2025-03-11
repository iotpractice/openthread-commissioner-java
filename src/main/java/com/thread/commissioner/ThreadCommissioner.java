/*
 *    Copyright (c) 2020, The OpenThread Commissioner Authors.
 *    All rights reserved.
 *
 *    Redistribution and use in source and binary forms, with or without
 *    modification, are permitted provided that the following conditions are met:
 *    1. Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *    2. Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *    3. Neither the name of the copyright holder nor the
 *       names of its contributors may be used to endorse or promote products
 *       derived from this software without specific prior written permission.
 *
 *    THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 *    AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 *    IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 *    ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 *    LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 *    CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 *    SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 *    INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 *    CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 *    ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 *    POSSIBILITY OF SUCH DAMAGE.
 */
package com.thread.commissioner;


import io.openthread.commissioner.*;
import io.openthread.commissioner.Error;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

/**
 * @author Kuldeep Singh
 */
public class ThreadCommissioner extends CommissionerHandler {

    private static final Logger logger = LoggerFactory.getLogger(ThreadCommissioner.class);

    private Commissioner nativeCommissioner;
    private boolean connected;
    private final Map<String, String> joiners = new HashMap<>();

    public boolean isConnected() {
        return connected;
    }

    public CompletableFuture<Void> connect(byte[] pskc, String otbrAddress, int otbrPort) {
        return CompletableFuture.runAsync(
                () -> {
                    throwIfFail(this.init(pskc));
                    throwIfFail(this.connect(otbrAddress, otbrPort));
                    connected = true;
                });
    }

    public CompletableFuture<Void> disconnect() {
        return CompletableFuture.runAsync(
                () -> {
                    nativeCommissioner.disconnect();
                    connected = false;
                });
    }

    public CompletableFuture<Void> enableAllJoiners(String pskd) {
        return CompletableFuture.runAsync(
                () -> {
                    throwIfFail(this.enableAllJoiners());
                    joiners.put(Utils.getHexString(computeJoinerIdAll()), pskd);
                });
    }

    public CompletableFuture<State> getState() {
        return CompletableFuture.supplyAsync(
                () -> {
                    return nativeCommissioner.getState();
                });
    }


    private Error init(byte[] pskc) {
        nativeCommissioner = Commissioner.create(this);
        Config config = new Config();
        config.setId("TestComm");
        config.setDomainName("TestDomain");
        config.setEnableCcm(false);
        config.setEnableDtlsDebugLogging(false);
        config.setPSKc(new ByteArray(pskc));
        //config.setLogger(new NativeCommissionerLogger());
        return nativeCommissioner.init(config);
    }

    private Error connect(String borderAgentAddress, int borderAgentPort) {
        // Petition to be the active commissioner in the Thread Network.
        return nativeCommissioner.petition(new String[1], borderAgentAddress, borderAgentPort);
    }

    public void list(){
        if(!joiners.isEmpty())
            joiners.forEach((joinerId,pskd)-> {logger.info("[{} - {}]", joinerId, pskd);} );
        else
            logger.info("No joiner enabled");
    }


    private void throwIfFail(Error error) throws CompletionException {
        if (error.getCode() != ErrorCode.kNone) {
            throw new CompletionException(new RuntimeException(error.getCode() + ":" + error.getMessage()));
        }
    }

    private ByteArray computeJoinerIdAll() {
        byte[] joinerBytes = new byte[16];
        Arrays.fill(joinerBytes, (byte) 0x00);
        return Commissioner.computeJoinerId(new BigInteger(joinerBytes));
    }

    private Error enableAllJoiners(){
        byte[] steeringDataBytes = new byte[16];
        Arrays.fill(steeringDataBytes, (byte) 0xFF); // Fill with 0xFF to allow all joiners
        ByteArray steeringData = new ByteArray(steeringDataBytes);

        ByteArray joinerId = computeJoinerIdAll();
        logger.info("enableAllJoiners - steeringData={} A joiner (ID={})", Utils.getHexString(steeringData), Utils.getHexString(joinerId));

        Commissioner.addJoiner(steeringData, joinerId);
        CommissionerDataset commDataset = new CommissionerDataset();
        commDataset.setPresentFlags(commDataset.getPresentFlags() & ~CommissionerDataset.kSessionIdBit);
        commDataset.setPresentFlags(commDataset.getPresentFlags() & ~CommissionerDataset.kBorderAgentLocatorBit);
        commDataset.setPresentFlags(commDataset.getPresentFlags() | CommissionerDataset.kSteeringDataBit);
        commDataset.setSteeringData(steeringData);
        return nativeCommissioner.setCommissionerDataset(commDataset);
    }

    @Override
    public String onJoinerRequest(ByteArray joinerId) {
        String joinerIdStr = Utils.getHexString(joinerId);
        logger.info("A joiner (ID={}) is requesting commissioning", joinerIdStr);
        String pskd = joiners.get(joinerIdStr);
        if (pskd == null) {
            // Check is JOINER ID All is registered
            pskd = joiners.get(Utils.getHexString(computeJoinerIdAll()));
        }
        return pskd;
    }

    @Override
    public void onJoinerConnected(ByteArray joinerId, Error error) {
        logger.info("A joiner (ID={}) is connected with {}", Utils.getHexString(joinerId), error);
    }

    @Override
    public void onKeepAliveResponse(Error error) {
        logger.trace("received keep-alive response: {}", error.toString());
    }

    @Override
    public void onPanIdConflict(String peerAddr, ChannelMask channelMask, int panId) {
        logger.trace("received PAN ID CONFLICT report");
    }

    @Override
    public void onEnergyReport(String aPeerAddr, ChannelMask aChannelMask, ByteArray aEnergyList) {
        logger.trace("received ENERGY SCAN report");
    }

    @Override
    public void onDatasetChanged() {
        logger.info("Thread Network Dataset chanaged");
    }

    @Override
    public boolean onJoinerFinalize(
            ByteArray joinerId,
            String vendorName,
            String vendorModel,
            String vendorSwVersion,
            ByteArray vendorStackVersion,
            String provisioningUrl,
            ByteArray vendorData) {
        String joinerIdStr = Utils.getHexString(joinerId);
        logger.info("A joiner (ID={}) is finalizing", joinerIdStr);
        if(joiners.get(joinerIdStr)==null){
            //Add joiner with key.
            joiners.put(joinerIdStr, joiners.get(Utils.getHexString(computeJoinerIdAll())));
        }

        return true;
    }

    static class NativeCommissionerLogger extends io.openthread.commissioner.Logger {

        @Override
        public void log(LogLevel level, String region, String msg) {
            if (level == LogLevel.kDebug) {
                logger.debug("[ {} ]: {}", region, msg);
            } else if (level == LogLevel.kInfo) {
                logger.info("[ {} ]: {}", region, msg);
            } else if (level == LogLevel.kWarn) {
                logger.warn("[ {} ]: {}", region, msg);
            } else if (level == LogLevel.kError) {
                logger.error("[ {} ]: {}", region, msg);
            } else {
                logger.trace("[ {} ]: {}", region, msg);
            }
        }
    }


    public byte[] computePskc(String passphrase, String networkName, ByteArray extendedPanId) {
        ByteArray pskc = new ByteArray();
        Error error = Commissioner.generatePSKc(pskc, passphrase, networkName, extendedPanId);
        if (error.getCode() != ErrorCode.kNone) {
            logger.info("Failed to generate PSKc: {}; network-name={}, extended-pan-id={}", error, networkName, Utils.getHexString(extendedPanId));
        } else {
            logger.info("Generated pskc={}, network-name={}, extended-pan-id={}", Utils.getHexString(pskc), networkName, Utils.getHexString(extendedPanId));
        }
        return Utils.getByteArray(pskc);
    }

}
