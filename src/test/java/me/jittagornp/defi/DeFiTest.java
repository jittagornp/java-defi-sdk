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

        bsc(credentials);
        //polygon(credentials);
        //bitkub(credentials);
    }

    private static void bsc(final Credentials credentials) throws Exception {

        final String pancakeSwapRouter = "0x10ED43C718714eb63d5aA57B78B54704E256024E";
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
        deFi.onTransfer(BUSD, log("Event Transfer"));
    }

    private static void polygon(final Credentials credentials) throws Exception {

        final String quickSwapRouter = "0xa5e0829caced8ffdd4de3c43696c57f7d7a678ff";
        final String WMATIC = "0x0d500b1d8e8ef31e21c99d1db9a6444d3adf1270";
        final String USDC = "0x2791bca1f2de4661ed88a30c99a7a9449aa84174";

        final DeFi deFi = DeFiSDK.polygonMainnet(credentials)
                .setTokenAutoApproveNTimes(3)
                .setDefaultSwapSlippage(0.5)
                .setDefaultSwapDeadlineMinutes(10);

        deFi.getGasPrice().thenAccept(log("Gas price")).get();
        deFi.getGasBalance().thenAccept(log("Gas balance")).get();
        deFi.getTokenAmountsOut(quickSwapRouter, WMATIC, USDC, BigDecimal.ONE).thenAccept(log("WMATIC Amount out")).get();
        deFi.getTokenPrice(WMATIC, USDC, quickSwapRouter).thenAccept(log("WMATIC Price")).get();
        deFi.getTokenInfo(WMATIC, USDC, quickSwapRouter).thenAccept(log("WMATIC")).get();
        deFi.getTokenInfo(USDC, USDC, quickSwapRouter).thenAccept(log("USDC")).get();
        deFi.getTokenAllowance(USDC, quickSwapRouter).thenAccept(log("Token allowance")).get();
        //deFi.tokenApprove(USDC, BigDecimal.valueOf(2.433), quickSwapRouter).thenAccept(log("Approve Tx")).get();
        //deFi.tokenSwap(quickSwapRouter, WMATIC, USDC, BigDecimal.valueOf(5.8)).thenAccept(log("Swap Tx")).get();
        //deFi.tokenSwapAndAutoApprove(quickSwapRouter, WMATIC, USDC, BigDecimal.valueOf(5.8)).thenAccept(log("Swap Tx")).get();
        //deFi.tokenTransfer(WMATIC, "<Target>", BigDecimal.valueOf(0.0013)).thenAccept(log("Transfer Tx")).get();
        //deFi.tokenApprove(WMATIC, BigDecimal.TEN, quickSwapRouter).thenAccept(log("Token Approve Tx")).get();
        //deFi.tokenSwap(quickSwapRouter, USDC, WMATIC, deFi.getTokenBalance(USDC).get()).thenAccept(log("Swap Tx")).get();
        //deFi.tokenSwap(quickSwapRouter, WMATIC, USDC, deFi.getTokenBalance(WMATIC).get()).thenAccept(log("Swap Tx")).get();
        //deFi.fillGas(WMATIC, deFi.getTokenBalance(WMATIC).get()).thenAccept(log("Fill Gas Tx")).get();
        //deFi.tokenSwapAndFillGas(quickSwapRouter, USDC, WMATIC, BigDecimal.ONE).thenAccept(log("Swap and Fill Gas Tx")).get();
        //deFi.onBlock(block -> log("Block number").accept(block.getNumber()));
        deFi.onTransfer(USDC, log("Event Transfer"));
    }

    private static void bitkub(final Credentials credentials) throws Exception {

        final String vonderRouter = "0x54D851C39fE28b2E24e354B5E8c0f09EfC65B51A";
        final String KKUB = "0x67eBD850304c70d983B2d1b93ea79c7CD6c3F6b5";
        final String kDAI = "0xED7B8606270295d1b3b60b99c051de4D7D2f7ff2";

        final DeFi deFi = DeFiSDK.bitkubMainnet(credentials)
                .setTokenAutoApproveNTimes(3)
                .setDefaultSwapSlippage(0.5)
                .setDefaultSwapDeadlineMinutes(10);

        deFi.getGasPrice().thenAccept(log("Gas price")).get();
        deFi.getGasBalance().thenAccept(log("Gas balance")).get();
        deFi.getTokenAmountsOut(vonderRouter, KKUB, kDAI, BigDecimal.ONE).thenAccept(log("KKUB Amount out")).get();
        deFi.getTokenPrice(KKUB, kDAI, vonderRouter).thenAccept(log("KKUB Price")).get();
        deFi.getTokenInfo(KKUB, kDAI, vonderRouter).thenAccept(log("KKUB")).get();
        deFi.getTokenInfo(kDAI, kDAI, vonderRouter).thenAccept(log("kDAI")).get();
        deFi.getTokenAllowance(kDAI, vonderRouter).thenAccept(log("Token allowance")).get();
        //deFi.tokenApprove(kDAI, BigDecimal.valueOf(2.433), vonderRouter).thenAccept(log("Approve Tx")).get();
        //deFi.tokenSwap(vonderRouter, KKUB, kDAI, BigDecimal.valueOf(5.8)).thenAccept(log("Swap Tx")).get();
        //deFi.tokenSwapAndAutoApprove(vonderRouter, KKUB, kDAI, BigDecimal.valueOf(5.8)).thenAccept(log("Swap Tx")).get();
        //deFi.tokenTransfer(KKUB, "<Target>", BigDecimal.valueOf(0.0013)).thenAccept(log("Transfer Tx")).get();
        //deFi.tokenApprove(KKUB, BigDecimal.TEN, vonderRouter).thenAccept(log("Token Approve Tx")).get();
        //deFi.tokenSwap(vonderRouter, kDAI, KKUB, deFi.getTokenBalance(kDAI).get()).thenAccept(log("Swap Tx")).get();
        //deFi.tokenSwap(vonderRouter, KKUB, kDAI, deFi.getTokenBalance(KKUB).get()).thenAccept(log("Swap Tx")).get();
        //deFi.fillGas(KKUB, deFi.getTokenBalance(KKUB).get()).thenAccept(log("Fill Gas Tx")).get();
        //deFi.tokenSwapAndFillGas(vonderRouter, kDAI, KKUB, BigDecimal.ONE).thenAccept(log("Swap and Fill Gas Tx")).get();
        //deFi.onBlock(block -> log("Block number").accept(block.getNumber()));
        deFi.onTransfer(kDAI, log("Event Transfer"));
    }

    private static <T> Consumer<T> log(final String message) {
        return (T value) -> log.info("{} => {}", message, value);
    }

}
