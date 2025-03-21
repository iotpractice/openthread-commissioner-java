package com.thread.commissioner;

import io.openthread.commissioner.ByteArray;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.ParseException;
import java.util.Scanner;
import java.util.concurrent.CompletionException;

/**
 * @author Kuldeep Singh
 */
public class Runner {
    private static final Logger logger = LoggerFactory.getLogger(Runner.class);
    private static final int TIMEOUT = 60;

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        ThreadCommissioner commissioner = new ThreadCommissioner();
        while (true) {
            System.out.println("Commands:");
            System.out.println("1. Connect Border Router");
            System.out.println("2. Check State");
            System.out.println("3. Enable All Joiners");
            System.out.println("4. List Joiners");
            System.out.println("5. Disconnect");
            System.out.println("6. Exit");
            System.out.println("Enter command number: ");
            int command = 0;
            try {
                command = Integer.parseInt(scanner.nextLine());
            } catch (NumberFormatException e){
                logger.warn("Invalid Command");
                continue;
            }

            switch (command) {
                case 1:
                    connnect(commissioner, scanner);
                    break;
                case 2:
                    state(commissioner);
                    break;
                case 3:
                    enableAllJoiners(commissioner, scanner);
                    break;
                case 4:
                    commissioner.list();
                    break;
                case 5:
                    stop(commissioner);
                    break;
                case 6:
                    logger.warn("Exiting...");
                    System.exit(0);
                    break;
                default:
                    logger.warn("Invalid command.");
            }
        }
    }

    private static void enableAllJoiners(ThreadCommissioner commissioner,Scanner scanner) {
        if (commissioner.isConnected()) {
            try {
                System.out.println("Enter PSKd For All Joiner:");
                String pskd = scanner.nextLine();
                if (!pskd.isEmpty()) {
                    commissioner.enableAllJoiners(pskd)
                            .thenRun(() -> {
                                logger.info("All Joiners are accepted at PSKD:{}", pskd);
                            })
                            .exceptionally(
                                    ex -> {
                                        logger.error("Failed to add Joiner :{}", ex.getMessage());
                                        return null;
                                    });
                } else {
                    logger.warn("Invalid PSKd");
                }
            } catch (CompletionException e) {
                logger.warn("Failed to Enable All Joiners:{}", e.getMessage());
            }
        } else {
            logger.warn("Commissioner is not connected, reconnect or try again after sometime!");
        }
    }

    private static void state(ThreadCommissioner commissioner) {
        if (commissioner.isConnected()) {
            try {
                commissioner.getState().thenAccept((state) -> {
                            logger.info("State:{}", state);
                        })
                        .exceptionally(
                                ex -> {
                                    logger.warn("Failed to get State : {}", String.valueOf(ex));
                                    return null;
                                });
            } catch (CompletionException e) {
                logger.warn("Failed to get get State: {}", e.getMessage());
            }
        } else {
            logger.warn("Commissioner is not connected, reconnect or try again after sometime!");
        }
    }

    private static void connnect(ThreadCommissioner commissioner, Scanner scanner) {
        if (!commissioner.isConnected()) {
            OTBRDiscoverer otbrDiscoverer = new OTBRDiscoverer();
            int counter = 0;
            do {
                try {
                    counter++;
                    logger.info("Discovering Border Router...{}", counter);
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            } while (counter < TIMEOUT && otbrDiscoverer.getOTBRInfo() == null);

            OTBRInfo otbrInfo = otbrDiscoverer.getOTBRInfo();
            if (otbrInfo == null) {
                otbrDiscoverer.close();
                logger.info("Unable to Discover Border Router Let's manually resolve it");
                System.out.println(">>> Enter Border Agent Address: ");
                String borderAgentAddress = scanner.nextLine();

                System.out.println(">>> Enter Border Agent Port: ");
                int borderAgentPort = Integer.parseInt(scanner.nextLine());

                otbrInfo = new OTBRInfo(borderAgentAddress, borderAgentPort);
            }

            System.out.println(">>> Enter PSKc (enter blank if want to compute):");
            String pskcStr = scanner.nextLine();
            byte[] pskc;
            if (pskcStr.isEmpty()) {
                if (otbrInfo.getNetworkName() == null) {
                    System.out.println(">>> Enter Network Name:");
                    String networkName = scanner.nextLine();
                    otbrInfo.setNetworkName(networkName);
                }

                if (otbrInfo.getExtendedPanId() == null) {
                    System.out.println(">>> Enter EXT Pan ID:");
                    String extPanId = scanner.nextLine();
                    otbrInfo.setExtendedPanId(extPanId);
                }

                System.out.println(">>>  Enter Commissioner Passphrase:");
                String passphrase = scanner.nextLine();
                pskc = commissioner.computePskc(passphrase, otbrInfo.getNetworkName(), new ByteArray(Utils.getByteArray(otbrInfo.getExtendedPanId())));
            } else {
                pskc = Utils.getByteArray(pskcStr);
            }
            try {
                commissioner.connect(pskc, otbrInfo.getOtbrAddress(), otbrInfo.getOtbrPort())
                        .thenRun(() -> {
                            logger.info("Commissioner connected successfully!");
                        })
                        .exceptionally(
                                ex -> {
                                    logger.error("Commissioner failed to connect : {}", String.valueOf(ex));
                                    return null;
                                });
            } catch (CompletionException e) {
                logger.error("Failed to connect: {}", e.getMessage());
            }
        } else {
            logger.warn("Commissioner already connected!");
        }
    }

    private static void stop(ThreadCommissioner commissioner) {
        if (commissioner.isConnected()) {
            try {
                commissioner.disconnect().thenRun(() -> {
                    logger.info("Commissioner disconnected!");
                });
            } catch (CompletionException e) {
                logger.warn("Failed to disconnect: {}", e.getMessage());
            }
        } else {
            logger.warn("Commissioner is not connected!");
        }
    }
}