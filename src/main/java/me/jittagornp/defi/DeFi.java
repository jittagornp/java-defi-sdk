/*
 * Copyright 2021-Current jittagornp.me
 */
package me.jittagornp.defi;

import lombok.Getter;
import me.jittagornp.defi.model.TokenInfo;
import org.web3j.protocol.core.methods.response.EthBlock;
import org.web3j.protocol.core.methods.response.TransactionReceipt;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * @author jittagornp
 */
public interface DeFi {

    @Getter
    enum Network {
        BSC_MAINNET(
                "Binance Smart Chain",
                56L,
                "https://bsc-dataseed.binance.org",
                "BNB",
                "https://bscscan.com"
        ),
        POLYGON_MAINNET(
                "Polygon (POS) Chain",
                137L,
                "https://rpc-mainnet.maticvigil.com",
                "MATIC",
                "https://polygonscan.com"
        ),
        BITKUB_MAINNET(
                "Bitkub Chain",
                96L,
                "https://rpc.bitkubchain.io",
                "KUB",
                "https://bkcscan.com"
        );

        private final String name;

        private final long chainId;

        private final String rpcURL;

        private final String gasSymbol;

        private final String explorerURL;

        private Network(
                final String name,
                final long chainId,
                final String rpcURL,
                final String gasSymbol,
                final String explorerURL
        ) {
            this.name = name;
            this.chainId = chainId;
            this.rpcURL = rpcURL;
            this.gasSymbol = gasSymbol;
            this.explorerURL = explorerURL;
        }

    }

    Network getNetwork();

    String getWalletAddress();

    String getWalletShortAddress();

    DeFi setDefaultSwapDeadlineMinutes(final int swapDeadlineMinutes);

    DeFi setDefaultSwapSlippage(final double swapSlippage);

    DeFi setTokenAutoApproveNTimes(double tokenAutoApproveNTimes);

    CompletableFuture<BigDecimal> getGasBalance();

    CompletableFuture<BigDecimal> getGasPrice();

    CompletableFuture<BigDecimal> getTokenBalance(final String token);

    CompletableFuture<TransactionReceipt> tokenTransfer(final String token, String recipient, final BigDecimal amount);

    CompletableFuture<BigDecimal> getTokenAmountsOut(final String swapRouter, final String tokenA, final String tokenB, final BigDecimal amount);

    CompletableFuture<BigDecimal> getTokenAmountsOutMin(final String swapRouter, final String tokenA, final String tokenB, final BigDecimal amount, final double slippage);

    CompletableFuture<BigDecimal> getTokenAmountsOutMin(final String swapRouter, final String tokenA, final String tokenB, final BigDecimal amount);

    CompletableFuture<BigDecimal> getTokenPrice(final String tokenA, final String tokenB, final String swapRouter);

    CompletableFuture<TokenInfo> getTokenInfo(final String token, final String tokenPair, final String swapRouter);

    CompletableFuture<List<TokenInfo>> getTokenInfoList(final List<String> tokens, final Function<String, String> tokenPair, final Function<String, String> tokenSwapRouter);

    CompletableFuture<List<TokenInfo>> getTokenInfoList(final List<String> tokens, final String tokenPair, final String swapRouter);

    CompletableFuture<BigDecimal> getTokenAllowance(final String token, final String contractAddress);

    CompletableFuture<TransactionReceipt> tokenApprove(final String token, final BigDecimal amount, final String contractAddress);

    CompletableFuture<TransactionReceipt> tokenSwap(final String swapRouter, final String tokenA, final String tokenB, final BigDecimal amount, final double slippage, final int deadlineMinutes);

    CompletableFuture<TransactionReceipt> tokenSwap(final String swapRouter, final String tokenA, final String tokenB, final BigDecimal amount, final double slippage);

    CompletableFuture<TransactionReceipt> tokenSwap(final String swapRouter, final String tokenA, final String tokenB, final BigDecimal amount);

    CompletableFuture<TransactionReceipt> tokenSwapAndAutoApprove(final String swapRouter, final String tokenA, final String tokenB, final BigDecimal amount, final double slippage, final int deadlineMinutes);

    CompletableFuture<TransactionReceipt> tokenSwapAndAutoApprove(final String swapRouter, final String tokenA, final String tokenB, final BigDecimal amount, final double slippage);

    CompletableFuture<TransactionReceipt> tokenSwapAndAutoApprove(final String swapRouter, final String tokenA, final String tokenB, final BigDecimal amount);

    CompletableFuture<TransactionReceipt> fillGas(final String gasToken, final BigDecimal amount);

    CompletableFuture<TransactionReceipt> tokenSwapAndFillGas(final String swapRouter, final String token, final String gasToken, final BigDecimal amount);

    void onBlock(final Consumer<EthBlock.Block> consumer, final long throttleMillisecond);

    void onBlock(final Consumer<EthBlock.Block> consumer);
}