/*
 * Copyright 2021-Current jittagornp.me
 */
package me.jittagornp.defi;

import lombok.extern.slf4j.Slf4j;
import org.web3j.crypto.WalletUtils;

import java.io.File;

/**
 * @author jittagornp
 */
@Slf4j
public class GenerateWallet {

    private static final String WALLET_DIRECTORY = System.getProperty("user.home") + "/crypto-wallet";
    private static final String WALLET_PASSWORD = "<YOUR_WALLET_PASSWORD>";

    public static void main(String[] args) throws Exception {
        final File directory = new File(WALLET_DIRECTORY);
        if (!directory.exists()) {
            directory.mkdirs();
        }
        final String outputFile = WalletUtils.generateNewWalletFile(WALLET_PASSWORD, directory);
        log.info("Output file => {}", outputFile);
    }
}
