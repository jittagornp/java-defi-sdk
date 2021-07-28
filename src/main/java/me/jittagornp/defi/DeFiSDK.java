/*
 * Copyright 2021-Current jittagornp.me
 */
package me.jittagornp.defi;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.reactivex.disposables.Disposable;
import lombok.extern.slf4j.Slf4j;
import me.jittagornp.defi.exception.ResponseErrorException;
import me.jittagornp.defi.model.TokenInfo;
import me.jittagornp.defi.smartcontract.*;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.Web3jService;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.Response;
import org.web3j.protocol.core.methods.request.Transaction;
import org.web3j.protocol.core.methods.response.EthBlock;
import org.web3j.protocol.core.methods.response.EthSendTransaction;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.protocol.http.HttpService;
import org.web3j.tx.Contract;
import org.web3j.tx.RawTransactionManager;
import org.web3j.tx.TransactionManager;
import org.web3j.tx.gas.ContractGasProvider;
import org.web3j.tx.gas.DefaultGasProvider;
import org.web3j.tx.response.EmptyTransactionReceipt;
import org.web3j.utils.Convert;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author jittagornp
 */
@Slf4j
public class DeFiSDK implements DeFi {

    private final Network network;

    private int defaultSwapDeadlineMinutes = 10;
    private double defaultSwapSlippage = 0.5;
    private double tokenAutoApproveNTimes = 3;

    private final Web3j web3j;
    private final Credentials credentials;
    private final TransactionManager transactionManager;
    private final ContractGasProvider gasProvider = new DefaultGasProvider();
    private final Map<String, Object> cached = new HashMap<>();
    private Disposable onBlock;

    protected DeFiSDK(final Network network, final Credentials credentials, final Web3jService web3jService) {
        this.network = network;
        this.credentials = credentials;
        this.web3j = Web3j.build(web3jService);
        this.transactionManager = new RawTransactionManager(web3j, credentials);
        log.info("Wallet address : {}", getWalletShortAddress());
    }

    public static DeFiSDK of(final Network network, final Credentials credentials) {
        if (network == Network.BSC_MAINNET) {
            return bscMainnet(credentials);
        }

        if (network == Network.POLYGON_MAINNET) {
            return polygonMainnet(credentials);
        }

        throw new UnsupportedOperationException("Unsupported network " + network);
    }

    public static DeFiSDK bscMainnet(final Credentials credentials) {
        return new DeFiSDK(Network.BSC_MAINNET, credentials, new HttpService("https://bsc-dataseed1.binance.org"));
    }

    public static DeFiSDK polygonMainnet(final Credentials credentials) {
        return new DeFiSDK(Network.POLYGON_MAINNET, credentials, new HttpService("https://rpc-mainnet.maticvigil.com"));
    }

    @Override
    public Network getNetwork() {
        return network;
    }

    @Override
    public String getWalletAddress() {
        return credentials.getAddress();
    }

    @Override
    public String getWalletShortAddress() {
        final String address = credentials.getAddress();
        return String.format("%s...%s", new String[]{address.substring(0, 6), address.substring(address.length() - 4)});
    }

    @Override
    public DeFi setDefaultSwapDeadlineMinutes(final int defaultSwapDeadlineMinutes) {
        this.defaultSwapDeadlineMinutes = defaultSwapDeadlineMinutes;
        return this;
    }

    @Override
    public DeFi setDefaultSwapSlippage(final double defaultSwapSlippage) {
        this.defaultSwapSlippage = defaultSwapSlippage;
        return this;
    }

    @Override
    public DeFi setTokenAutoApproveNTimes(final double tokenAutoApproveNTimes) {
        this.tokenAutoApproveNTimes = tokenAutoApproveNTimes;
        return this;
    }

    private <T extends Contract> T _newContract(final Class<T> clazz, final String address) {
        try {
            return (T) clazz.getMethod(
                    "load",
                    String.class,
                    Web3j.class,
                    TransactionManager.class,
                    ContractGasProvider.class
            ).invoke(null, address, web3j, transactionManager, gasProvider);
        } catch (Exception e) {
            log.error("New Contract error ", e);
            throw new RuntimeException(e);
        }
    }

    private <T extends Contract> T _loadContract(final Class<T> clazz, final String address) {
        final String key = clazz.getSimpleName() + "." + address;
        T contract = (T) cached.get(key);
        if (contract == null) {
            synchronized (this) {
                contract = _newContract(clazz, address);
                cached.put(key, contract);
            }
        }
        return contract;
    }

    private BigDecimal _fromWei(final BigInteger value) {
        return Convert.fromWei(value.toString(), Convert.Unit.ETHER);
    }

    private BigInteger _toWei(final BigDecimal value) {
        return Convert.toWei(value.toString(), Convert.Unit.ETHER)
                .toBigInteger();
    }

    private BigDecimal _fromGwei(final BigInteger ether) {
        return Convert.fromWei(ether.toString(), Convert.Unit.GWEI);
    }

    private <T extends Response> T _throwIfError(final String func, final T response) {
        if (response.hasError()) {
            try {
                log.error("{} error {}", func, new ObjectMapper().writeValueAsString(response.getError()));
            } catch (JsonProcessingException e) {
                log.warn("Can't parse json ", e);
            }
            throw new ResponseErrorException(response.getError());
        }
        return response;
    }

    @Override
    public CompletableFuture<TokenInfo> getTokenInfo(final String token, final String tokenPair, final String swapRouter) {
        final ERC20 erc20 = _loadContract(ERC20.class, token);
        final ERC20 erc20Pair = _loadContract(ERC20.class, tokenPair);
        final CompletableFuture<String> name = erc20.name().sendAsync();
        final CompletableFuture<String> symbol = erc20.symbol().sendAsync();
        final CompletableFuture<BigInteger> totalSupply = erc20.totalSupply().sendAsync();
        final CompletableFuture<BigDecimal> balanceOf = erc20.balanceOf(credentials.getAddress()).sendAsync().thenApply(this::_fromWei);
        final CompletableFuture<BigDecimal> price = getTokenPrice(token, tokenPair, swapRouter);
        final CompletableFuture<BigInteger> decimals = erc20.decimals().sendAsync();
        final CompletableFuture<String> pairSymbol = erc20Pair.symbol().sendAsync();
        return CompletableFuture.allOf(name, symbol, totalSupply, balanceOf, price, decimals)
                .thenApply(result -> {
                    final BigDecimal balance = _get(balanceOf);
                    final BigDecimal p = _get(price);
                    return TokenInfo.builder()
                            .address(token)
                            .name(_get(name))
                            .symbol(_get(symbol))
                            .totalSupply(_fromWei(_get(totalSupply)))
                            .balance(_get(balanceOf))
                            .price(_get(price))
                            .decimals(_get(decimals))
                            .value(balance.multiply(p))
                            .valueSymbol(_get(pairSymbol))
                            .build();
                });
    }

    private <T> T _get(final Future future) {
        try {
            return (T) future.get();
        } catch (Exception e) {
            log.error("Future.get() error ", e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public CompletableFuture<List<TokenInfo>> getTokenInfoList(final List<String> tokens, final Function<String, String> tokenPair, final Function<String, String> tokenSwapRouter) {
        final List<CompletableFuture<TokenInfo>> list = tokens.stream()
                .map(token -> getTokenInfo(token, tokenPair.apply(token), tokenSwapRouter.apply(token)))
                .collect(Collectors.toList());
        final CompletableFuture<TokenInfo>[] arr = new CompletableFuture[list.size()];
        list.toArray(arr);
        return CompletableFuture.allOf(arr)
                .thenApply(none -> list.stream()
                        .map((token) -> (TokenInfo) _get(token))
                        .collect(Collectors.toList())
                );
    }

    @Override
    public CompletableFuture<List<TokenInfo>> getTokenInfoList(final List<String> tokens, final String tokenPair, final String swapRouter) {
        return getTokenInfoList(tokens, (token) -> tokenPair, (token) -> swapRouter);
    }

    private CompletableFuture<String> _getPair(final String factory, final String tokenA, final String tokenB) {
        return _loadContract(Factory.class, factory)
                .getPair(tokenA, tokenB)
                .sendAsync();
    }

    @Override
    public CompletableFuture<BigDecimal> getTokenBalance(final String token) {
        return _loadContract(ERC20.class, token)
                .balanceOf(credentials.getAddress())
                .sendAsync()
                .thenApply(this::_fromWei);
    }

    @Override
    public CompletableFuture<BigDecimal> getTokenAmountsOut(final String swapRouter, final String tokenA, final String tokenB, final BigDecimal amount) {
        if (Objects.equals(tokenA, tokenB)) {
            return CompletableFuture.completedFuture(BigDecimal.ONE);
        }
        final BigInteger amountIn = _toWei(amount);
        final List<String> path = Arrays.asList(tokenA, tokenB);
        return _loadContract(Router.class, swapRouter)
                .getAmountsOut(amountIn, path)
                .sendAsync()
                .thenApply(amounts -> _fromWei((BigInteger) amounts.get(1)));
    }

    @Override
    public CompletableFuture<BigDecimal> getTokenAmountsOutMin(final String swapRouter, final String tokenA, final String tokenB, final BigDecimal amount, final double slippage) {
        return getTokenAmountsOut(swapRouter, tokenA, tokenB, amount)
                .thenApply(out -> getAmountOutMin(out, slippage));
    }

    @Override
    public CompletableFuture<BigDecimal> getTokenAmountsOutMin(final String swapRouter, final String tokenA, final String tokenB, final BigDecimal amount) {
        return getTokenAmountsOutMin(swapRouter, tokenA, tokenB, amount, defaultSwapSlippage);
    }

    @Override
    public CompletableFuture<BigDecimal> getTokenPrice(final String tokenA, final String tokenB, final String swapRouter) {
        return getTokenAmountsOut(swapRouter, tokenA, tokenB, BigDecimal.ONE);
    }

    private Transaction _createTransaction(final String contractAddress, final String data, final BigDecimal value) {
        final String from = credentials.getAddress();
        final int RANDOM_RANGE = 100000;
        final BigInteger nonce = BigInteger.valueOf((long) (Math.random() * RANDOM_RANGE) % RANDOM_RANGE);
        final BigInteger gasPrice = _get(_getGasPrice());
        final BigInteger gasLimit = gasProvider.getGasLimit(null);
        final String to = contractAddress;
        final BigInteger val = _toWei(value);
        return Transaction.createFunctionCallTransaction(from, nonce, gasPrice, gasLimit, to, val, data);
    }

    private CompletableFuture<TransactionReceipt> _sendTransaction(final String contractAddress, final String data, final BigDecimal value, final String func) {
        return web3j.ethEstimateGas(_createTransaction(contractAddress, data, value))
                .sendAsync()
                .thenApply(resp -> _throwIfError("ethEstimateGas", resp))
                .thenCompose(resp -> {
                    try {
                        log.info("Tx \"{}\" : Estimate gas limit = {}", func, resp.getAmountUsed());
                        final EthSendTransaction tx = transactionManager.sendTransaction(
                                _get(_getGasPrice()),
                                resp.getAmountUsed(),
                                contractAddress,
                                data,
                                _toWei(value)
                        );
                        log.info("Tx \"{}\" : Hash = {}", func, tx.getTransactionHash());
                        return new SchedulerGetTransactionReceipt(tx.getTransactionHash()).get();
                    } catch (IOException e) {
                        log.error("Send Transaction error ", e);
                        throw new RuntimeException(e);
                    }
                });
    }

    @Override
    public CompletableFuture<TransactionReceipt> tokenApprove(final String token, final BigDecimal amount, final String contractAddress) {
        return _sendTransaction(
                token,
                _loadContract(ERC20.class, token)
                        .approve(contractAddress, _toWei(amount))
                        .encodeFunctionCall(),
                BigDecimal.ZERO,
                "ERC20.approve(spender, amount)"
        );
    }

    private BigDecimal getAmountOutMin(final BigDecimal amount, final double slippage) {
        final BigDecimal n100 = BigDecimal.valueOf(100);
        final BigDecimal base = amount.divide(n100).multiply(BigDecimal.valueOf(slippage));
        return amount.subtract(base);
    }

    private CompletableFuture<TransactionReceipt> _swap(final String swapRouter, final String tokenA, final String tokenB, final BigDecimal amount, final double slippage, final int deadlineMinutes) {
        log.info("_swap(swapRouter, tokenA, tokenB, amount)");
        return getTokenAmountsOut(swapRouter, tokenA, tokenB, amount)
                .thenCompose(receiveAmount -> {
                    final BigInteger amountOut = _toWei(getAmountOutMin(receiveAmount, slippage));
                    final BigInteger deadline = BigInteger.valueOf(Instant.now().plusSeconds(60 * deadlineMinutes).toEpochMilli());
                    final BigInteger amountIn = _toWei(amount);
                    final List<String> path = Arrays.asList(tokenA, tokenB);
                    log.info("amount = {}", amount);
                    log.info("slippage = {}", slippage);
                    log.info("receiveAmount = {}", receiveAmount);
                    log.info("amountOut = {}", amountOut);
                    log.info("deadline = {}", deadline);
                    log.info("path = {}", path);
                    return _sendTransaction(
                            swapRouter,
                            _loadContract(Router.class, swapRouter)
                                    .swapExactTokensForTokens(
                                            amountIn,
                                            amountOut,
                                            path,
                                            credentials.getAddress(),
                                            deadline
                                    ).encodeFunctionCall(),
                            BigDecimal.ZERO,
                            "Router.swapExactTokensForTokens(amountIn, amountOutMin, path, to, deadline)"
                    );
                });
    }

    @Override
    public CompletableFuture<BigDecimal> getTokenAllowance(final String token, final String contractAddress) {
        return _loadContract(ERC20.class, token)
                .allowance(credentials.getAddress(), contractAddress)
                .sendAsync()
                .thenApply(this::_fromWei);
    }

    @Override
    public CompletableFuture<TransactionReceipt> tokenSwap(final String swapRouter, final String tokenA, final String tokenB, final BigDecimal amount, final double slippage, final int deadlineMinutes) {
        return getTokenAllowance(tokenA, swapRouter)
                .thenCompose(allowance -> {
                    log.info("Allowance Token \"{}\" amount {} for Contract \"{}\"", tokenA, allowance, swapRouter);
                    boolean isLessThan = allowance.compareTo(amount) < 0;
                    if (isLessThan) {
                        throw new RuntimeException("Please call .tokenApprove(token, amount, contractAddress) before swap");
                    }
                    return _swap(swapRouter, tokenA, tokenB, amount, slippage, deadlineMinutes);
                });
    }

    @Override
    public CompletableFuture<TransactionReceipt> tokenSwap(final String swapRouter, final String tokenA, final String tokenB, final BigDecimal amount, final double slippage) {
        return tokenSwap(swapRouter, tokenA, tokenB, amount, slippage, defaultSwapDeadlineMinutes);
    }

    @Override
    public CompletableFuture<TransactionReceipt> tokenSwap(final String swapRouter, final String tokenA, final String tokenB, final BigDecimal amount) {
        return tokenSwap(swapRouter, tokenA, tokenB, amount, defaultSwapSlippage);
    }

    @Override
    public CompletableFuture<TransactionReceipt> tokenSwapAndAutoApprove(final String swapRouter, final String tokenA, final String tokenB, final BigDecimal amount, final double slippage, final int deadlineMinutes) {
        return getTokenAllowance(tokenA, swapRouter)
                .thenCompose(allowance -> {
                    log.info("Allowance Token \"{}\" amount {} for Contract \"{}\"", tokenA, allowance, swapRouter);
                    boolean isLessThan = allowance.compareTo(amount) < 0;
                    if (isLessThan) {
                        final BigDecimal times = BigDecimal.valueOf(tokenAutoApproveNTimes);
                        final BigDecimal approvedAmount = times.multiply(amount);
                        log.info("Approved amount = {}", approvedAmount);
                        return tokenApprove(tokenA, approvedAmount, swapRouter)
                                .thenCompose(tx -> _swap(swapRouter, tokenA, tokenB, amount, slippage, deadlineMinutes));
                    }
                    return _swap(swapRouter, tokenA, tokenB, amount, slippage, deadlineMinutes);
                });
    }

    @Override
    public CompletableFuture<TransactionReceipt> tokenSwapAndAutoApprove(final String swapRouter, final String tokenA, final String tokenB, final BigDecimal amount, final double slippage) {
        return tokenSwapAndAutoApprove(swapRouter, tokenA, tokenB, amount, slippage, defaultSwapDeadlineMinutes);
    }

    @Override
    public CompletableFuture<TransactionReceipt> tokenSwapAndAutoApprove(final String swapRouter, final String tokenA, final String tokenB, final BigDecimal amount) {
        return tokenSwapAndAutoApprove(swapRouter, tokenA, tokenB, amount, defaultSwapSlippage);
    }

    @Override
    public CompletableFuture<BigDecimal> getGasBalance() {
        return web3j.ethGetBalance(credentials.getAddress(), DefaultBlockParameterName.LATEST)
                .sendAsync()
                .thenApply(resp -> _throwIfError("ethGetBalance", resp))
                .thenApply(resp -> _fromWei(resp.getBalance()));
    }

    private CompletableFuture<BigInteger> _getGasPrice() {
        return web3j.ethGasPrice()
                .sendAsync()
                .thenApply(resp -> _throwIfError("ethGasPrice", resp))
                .thenApply(resp -> resp.getGasPrice());
    }

    @Override
    public CompletableFuture<BigDecimal> getGasPrice() {
        return _getGasPrice()
                .thenApply(this::_fromGwei);
    }

    @Override
    public CompletableFuture<TransactionReceipt> tokenTransfer(final String token, String recipient, final BigDecimal amount) {
        return _sendTransaction(
                token,
                _loadContract(ERC20.class, token)
                        .transfer(recipient, _toWei(amount))
                        .encodeFunctionCall(),
                BigDecimal.ZERO,
                "ERC20.transfer(recipient, amount)"
        );
    }

    @Override
    public CompletableFuture<TransactionReceipt> fillGas(final String gasToken, final BigDecimal amount) {
        return _sendTransaction(
                gasToken,
                _loadContract(Wrapped.class, gasToken)
                        .withdraw(_toWei(amount))
                        .encodeFunctionCall(),
                BigDecimal.ZERO,
                "Wrapped.withdraw(wad)"
        );
    }

    @Override
    public CompletableFuture<TransactionReceipt> tokenSwapAndFillGas(final String swapRouter, final String token, final String gasToken, final BigDecimal amount) {
        return getTokenAmountsOutMin(swapRouter, token, gasToken, amount)
                .thenCompose(amountOut -> tokenSwapAndAutoApprove(swapRouter, token, gasToken, amount)
                        .thenCompose(tx -> fillGas(gasToken, amountOut))
                );
    }

    @Override
    public void onBlock(final Consumer<EthBlock.Block> consumer, final long throttleMillisecond) {
        if (onBlock != null) {
            onBlock.dispose();
        }
        onBlock = web3j.blockFlowable(false)
                .throttleWithTimeout(throttleMillisecond, TimeUnit.MILLISECONDS)
                .doOnNext(new io.reactivex.functions.Consumer<EthBlock>() {
                    @Override
                    public void accept(final EthBlock ethBlock) throws Exception {
                        if (ethBlock.getError() == null) {
                            consumer.accept(ethBlock.getBlock());
                        }
                    }
                })
                .subscribe();
    }

    @Override
    public void onBlock(final Consumer<EthBlock.Block> consumer) {
        onBlock(consumer, 300);
    }

    private class SchedulerGetTransactionReceipt {

        private final String transactionHash;
        private String id;
        private long waitMilliseconds;
        private long expiresMilliseconds;

        public SchedulerGetTransactionReceipt(final String transactionHash) {
            this(transactionHash, 1000 * 5L, 1000 * 60 * 20L);
        }

        public SchedulerGetTransactionReceipt(final String transactionHash, final long waitMilliseconds, final long expiresMilliseconds) {
            this.transactionHash = transactionHash;
            this.waitMilliseconds = waitMilliseconds;
            this.expiresMilliseconds = expiresMilliseconds;
            this.id = "scheduler-" + UUID.randomUUID().toString().substring(0, 5);
        }

        private CompletableFuture<TransactionReceipt> get() {
            final CompletableFuture future = new CompletableFuture();
            final Thread thread = new Thread(run(future));
            thread.setName(id);
            thread.start();
            return future;
        }

        private Runnable run(final CompletableFuture future) {
            return () -> {
                int round = 0;
                while (true) {
                    long waitTime = round * waitMilliseconds;
                    if (waitTime >= expiresMilliseconds) {
                        log.info("{} : Expired Tx = {}", id, transactionHash);
                        future.complete(new EmptyTransactionReceipt(transactionHash));
                        break;
                    }
                    try {
                        final TransactionReceipt txReceipt = web3j.ethGetTransactionReceipt(transactionHash)
                                .send()
                                .getResult();
                        if (txReceipt != null && future.complete(txReceipt)) {
                            log.info("{} : SUCCESS Tx = {} in {} milliseconds, {}", id, transactionHash, waitTime, txReceipt);
                            break;
                        }
                        round = round + 1;
                        Thread.sleep(waitMilliseconds);
                    } catch (IOException | InterruptedException e) {
                        log.warn("{} : get() error ", id, e);
                    }
                }
            };
        }
    }
}