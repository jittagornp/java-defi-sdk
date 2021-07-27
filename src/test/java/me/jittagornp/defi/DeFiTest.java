/*
 * Copyright 2021-Current jittagornp.me
 */
package me.jittagornp.defi;

import lombok.extern.slf4j.Slf4j;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.WalletUtils;

import java.io.File;
import java.math.BigDecimal;
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
        final String pancakeSwapRouter = "0x6B011d0d53b0Da6ace2a3F436Fd197A4E35f47EF";

        final String WBNB = "0xbb4CdB9CBd36B01bD1cBaEBF2De08d9173bc095c";
        final String BUSD = "0xe9e7cea3dedca5984780bafc599bd69add087d56";

        final DeFi deFi = DeFiSDK.bscMainnet(credentials)
                .setTokenAutoApproveNTimes(3)
                .setDefaultSwapSlippage(0.5)
                .setDefaultSwapDeadlineMinutes(10);

        deFi.getGasPrice().thenAccept(log("Gas price")).get();
        deFi.getGasBalance().thenAccept(log("Gas balance")).get();
        deFi.getTokenAmountsOut(pancakeSwapRouter, WBNB, BUSD, BigDecimal.ONE).thenAccept(log("WBNB Amount out")).get();
        deFi.getTokenPrice(WBNB, BUSD, pancakeSwapRouter).thenAccept(log("WBNB Price")).get();
        deFi.getTokenInfo(WBNB, BUSD, pancakeSwapRouter).thenAccept(log("WBNB")).get();
        deFi.getTokenInfo(BUSD, BUSD, pancakeSwapRouter).thenAccept(log("BUSD")).get();
        deFi.getTokenAllowance(BUSD, pancakeSwapRouter).thenAccept(log("Token allowance")).get();
        //deFi.tokenApprove(BUSD, BigDecimal.valueOf(2.433), pancakeSwapRouter).thenAccept(log("Approve Tx")).get();
        //deFi.tokenSwap(pancakeSwapRouter, WBNB, BUSD, BigDecimal.valueOf(5.8)).thenAccept(log("Swap Tx")).get();
        //deFi.tokenSwapAndAutoApprove(pancakeSwapRouter, WBNB, BUSD, BigDecimal.valueOf(5.8)).thenAccept(log("Swap Tx")).get();
        //deFi.tokenTransfer(WBNB, "<Target>", BigDecimal.valueOf(0.0013)).thenAccept(log("Transfer Tx")).get();
        //deFi.tokenApprove(WBNB, BigDecimal.TEN, pancakeSwapRouter).thenAccept(log("Token Approve Tx")).get();
        //deFi.tokenSwap(pancakeSwapRouter, BUSD, WBNB, deFi.getTokenBalance(BUSD).get()).thenAccept(log("Swap Tx")).get();
        //deFi.tokenSwap(pancakeSwapRouter, WBNB, BUSD, deFi.getTokenBalance(WBNB).get()).thenAccept(log("Swap Tx")).get();
        //deFi.fillGas(WBNB, deFi.getTokenBalance(WBNB).get()).thenAccept(log("Fill Gas Tx")).get();
        //deFi.tokenSwapAndFillGas(pancakeSwapRouter, BUSD, WBNB, BigDecimal.ONE).thenAccept(log("Swap and Fill Gas Tx")).get();
        //deFi.onBlock(block -> log("Block number").accept(block.getNumber()));
    }

    private static <T> Consumer<T> log(final String message) {
        return (T value) -> log.info("{} => {}", message, value);
    }

}
