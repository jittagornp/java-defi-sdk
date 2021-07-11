/*
 * Copyright 2021-Current jittagornp.me
 */
package me.jittagornp.defi;

import lombok.extern.slf4j.Slf4j;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.WalletUtils;

import java.io.File;
import java.util.function.Consumer;

/**
 * @author jittagornp
 */
@Slf4j
public class DeFiTest {

    private static final String WALLET_DIRECTORY = System.getProperty("user.home") + "/crypto-wallet";
    private static final String WALLET_FILE_NAME = "<YOUR_WALLET_FILE>.json";
    private static final String WALLET_PASSWORD = "<YOUR_WALLET_PASSWORD>";

    public static void main(String[] args) throws Exception {

        final Credentials credentials = WalletUtils.loadCredentials(WALLET_PASSWORD, new File(WALLET_DIRECTORY, WALLET_FILE_NAME));

        //PancakeSwap
        final String pancakeSwapFactory = "0x4e66fda7820c53c1a2f601f84918c375205eac3e";
        final String pancakeSwapRouter = "0x6B011d0d53b0Da6ace2a3F436Fd197A4E35f47EF";

        final String WBNB = "0xbb4CdB9CBd36B01bD1cBaEBF2De08d9173bc095c";
        final String BUSD = "0xe9e7cea3dedca5984780bafc599bd69add087d56";

        final DeFi defi = DeFiSDK.bsc(credentials)
                .setTokenAutoApproveNTimes(3)
                .setDefaultSwapSlippage(0.5)
                .setDefaultSwapDeadlineMinutes(10);

        defi.getGasPrice().thenAccept(log("Gas price")).get();
        defi.getGasBalance().thenAccept(log("Gas balance")).get();
        defi.getTokenPrice(WBNB, BUSD, pancakeSwapFactory).thenAccept(log("WBNB Price")).get();
        defi.getTokenInfo(WBNB, BUSD, pancakeSwapFactory).thenAccept(log("WBNB")).get();
        defi.getTokenInfo(BUSD, BUSD, pancakeSwapFactory).thenAccept(log("BUSD")).get();
        defi.getTokenAllowance(BUSD, pancakeSwapRouter).thenAccept(log("Token allowance")).get();
        //defi.tokenApprove(BUSD, BigDecimal.valueOf(2.433), pancakeSwapRouter).thenAccept(log("Approve Tx")).get();
        //defi.tokenSwap(pancakeSwapRouter, WBNB, BUSD, BigDecimal.valueOf(5.8)).thenAccept(log("Swap Tx")).get();
        //defi.tokenSwapAndAutoApprove(pancakeSwapRouter, WBNB, BUSD, BigDecimal.valueOf(5.8)).thenAccept(log("Swap Tx")).get();
        //defi.tokenTransfer(WBNB, "<Target>", BigDecimal.valueOf(0.0013)).thenAccept(log("Transfer Tx")).get();
        //defi.tokenApprove(WBNB, BigDecimal.TEN, pancakeSwapRouter).thenAccept(log("Token Approve Tx")).get();
        //defi.tokenSwap(pancakeSwapRouter, BUSD, WBNB, defi.getTokenBalance(BUSD).get()).thenAccept(log("Swap Tx")).get();
        //defi.tokenSwap(pancakeSwapRouter, WBNB, BUSD, defi.getTokenBalance(WBNB).get()).thenAccept(log("Swap Tx")).get();
        //defi.fillGas(WBNB, defi.getTokenBalance(WBNB).get()).thenAccept(log("Fill Gas Tx")).get();
        defi.onBlock(block -> log("Block number").accept(block.getNumber()));
    }

    private static <T> Consumer<T> log(final String message) {
        return (T value) -> log.info("{} => {}", message, value);
    }

}
