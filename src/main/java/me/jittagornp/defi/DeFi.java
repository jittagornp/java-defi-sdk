/*
 * Copyright 2021-Current jittagornp.me
 */
package me.jittagornp.defi;

import me.jittagornp.defi.model.TokenInfo;
import org.web3j.protocol.core.methods.response.TransactionReceipt;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

/**
 * @author jittagornp
 */
public interface DeFi {

    DeFi setDefaultSwapDeadlineMinutes(final int swapDeadlineMinutes);

    DeFi setDefaultSwapSlippage(final double swapSlippage);

    DeFi setTokenAutoApproveNTimes(double tokenAutoApproveNTimes);

    CompletableFuture<BigDecimal> getGasBalance();

    CompletableFuture<BigDecimal> getGasPrice();

    CompletableFuture<BigDecimal> getTokenBalance(final String token);

    CompletableFuture<TransactionReceipt> tokenTransfer(final String token, String recipient, final BigDecimal amount);

    CompletableFuture<BigDecimal> getTokenPrice(final String tokenA, final String tokenB, final String factory);

    CompletableFuture<TokenInfo> getTokenInfo(final String token, final String tokenPair, final String factory);

    CompletableFuture<List<TokenInfo>> getTokenInfoList(final List<String> tokens, final Function<String, String> tokenPair, final Function<String, String> tokenFactory);

    CompletableFuture<List<TokenInfo>> getTokenInfoList(final List<String> tokens, final String tokenPair, final String factory);

    CompletableFuture<BigDecimal> getTokenAllowance(final String token, final String contractAddress);

    CompletableFuture<TransactionReceipt> tokenApprove(final String token, final BigDecimal amount, final String contractAddress);

    CompletableFuture<TransactionReceipt> tokenSwap(final String router, final String tokenA, final String tokenB, final BigDecimal amount, final double slippage, final int deadlineMinutes);

    CompletableFuture<TransactionReceipt> tokenSwap(final String router, final String tokenA, final String tokenB, final BigDecimal amount, final double slippage);

    CompletableFuture<TransactionReceipt> tokenSwap(final String router, final String tokenA, final String tokenB, final BigDecimal amount);

    CompletableFuture<TransactionReceipt> tokenSwapAndAutoApprove(final String router, final String tokenA, final String tokenB, final BigDecimal amount, final double slippage, final int deadlineMinutes);

    CompletableFuture<TransactionReceipt> tokenSwapAndAutoApprove(final String router, final String tokenA, final String tokenB, final BigDecimal amount, final double slippage);

    CompletableFuture<TransactionReceipt> tokenSwapAndAutoApprove(final String router, final String tokenA, final String tokenB, final BigDecimal amount);

    CompletableFuture<TransactionReceipt> fillGas(final String token, final BigDecimal amount);

}
