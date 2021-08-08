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
import me.jittagornp.defi.smartcontract.ERC20;
import me.jittagornp.defi.smartcontract.Router;
import me.jittagornp.defi.smartcontract.Wrapped;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.RemoteFunctionCall;
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
    private Map<String, Disposable> onTransferMap = new HashMap<>();

    protected DeFiSDK(final Network network, final Credentials credentials) {
        this.network = network;
        this.credentials = credentials;
        this.web3j = Web3j.build(new HttpService(network.getRpcURL()));
        this.transactionManager = new RawTransactionManager(web3j, credentials, network.getChainId());
        log.info("Wallet address : {}", getWalletShortAddress());
    }

    public static DeFiSDK of(final Network network, final Credentials credentials) {
        if (network == Network.BSC_MAINNET) {
            return bscMainnet(credentials);
        }

        if (network == Network.POLYGON_MAINNET) {
            return polygonMainnet(credentials);
        }

        if (network == Network.BITKUB_MAINNET) {
            return bitkubMainnet(credentials);
        }

        throw new UnsupportedOperationException("Unsupported network " + network);
    }

    public static DeFiSDK bscMainnet(final Credentials credentials) {
        return new DeFiSDK(Network.BSC_MAINNET, credentials);
    }

    public static DeFiSDK polygonMainnet(final Credentials credentials) {
        return new DeFiSDK(Network.POLYGON_MAINNET, credentials);
    }

    public static DeFiSDK bitkubMainnet(final Credentials credentials) {
        return new DeFiSDK(Network.BITKUB_MAINNET, credentials);
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

    private BigDecimal _fromWei(final BigInteger value, final BigInteger decimals) {
        final BigDecimal val = new BigDecimal(value.toString());
        final int d = decimals.intValue();
        return val.divide(BigDecimal.TEN.pow(d));
    }

    private BigInteger _toWei(final BigDecimal value, final BigInteger decimals) {
        final int d = decimals.intValue();
        return value.multiply(BigDecimal.TEN.pow(d)).toBigInteger();
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

    private CompletableFuture<BigDecimal> _getTokenTotalSupply(final String token, final BigInteger decimals) {
        return _loadContract(ERC20.class, token)
                .totalSupply().sendAsync()
                .thenApply(totalSupply -> _fromWei(totalSupply, decimals));
    }

    private <T> CompletableFuture<T> _cacheValue(final String key, final RemoteFunctionCall<T> functionCall) {
        final T value = (T) cached.get(key);
        if (value == null) {
            return functionCall.sendAsync()
                    .thenApply(val -> {
                        cached.put(key, val);
                        return val;
                    });
        } else {
            return CompletableFuture.completedFuture(value);
        }
    }

    private CompletableFuture<BigInteger> _getDecimals(final String token) {
        final String key = token + ".decimals";
        return _cacheValue(key, _loadContract(ERC20.class, token).decimals());
    }

    private CompletableFuture<String> _getName(final String token) {
        final String key = token + ".name";
        return _cacheValue(key, _loadContract(ERC20.class, token).name());
    }

    private CompletableFuture<String> _getSymbol(final String token) {
        final String key = token + ".symbol";
        return _cacheValue(key, _loadContract(ERC20.class, token).symbol());
    }

    @Override
    public CompletableFuture<TokenInfo> getTokenInfo(final String token, final String tokenPair, final String swap) {
        final CompletableFuture<BigInteger> decimals = _getDecimals(token);
        final CompletableFuture<BigInteger> pairDecimals = _getDecimals(tokenPair);
        return CompletableFuture.allOf(decimals, pairDecimals)
                .thenCompose(none -> {
                    final CompletableFuture<String> name = _getName(token);
                    final CompletableFuture<String> symbol = _getSymbol(token);
                    final CompletableFuture<BigDecimal> totalSupply = _getTokenTotalSupply(token, _get(decimals));
                    final CompletableFuture<BigDecimal> balanceOf = _getTokenBalance(token, _get(decimals));
                    final CompletableFuture<BigDecimal> price = _getTokenPrice(token, _get(decimals), tokenPair, _get(pairDecimals), swap);
                    final CompletableFuture<String> pairSymbol = _getSymbol(tokenPair);
                    return CompletableFuture.allOf(name, symbol, totalSupply, balanceOf, price, pairSymbol)
                            .thenApply(result -> {
                                final BigDecimal balance = _get(balanceOf);
                                final BigDecimal p = _get(price);
                                return TokenInfo.builder()
                                        .address(token)
                                        .name(_get(name))
                                        .symbol(_get(symbol))
                                        .totalSupply(_get(totalSupply))
                                        .balance(_get(balanceOf))
                                        .price(_get(price))
                                        .decimals(_get(decimals))
                                        .value(balance.multiply(p))
                                        .valueSymbol(_get(pairSymbol))
                                        .build();
                            });
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
    public CompletableFuture<List<TokenInfo>> getTokenInfoList(final List<String> tokens, final Function<String, String> tokenPair, final Function<String, String> tokenRouter) {
        final List<CompletableFuture<TokenInfo>> list = tokens.stream()
                .map(token -> getTokenInfo(token, tokenPair.apply(token), tokenRouter.apply(token)))
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

    public CompletableFuture<BigDecimal> _getTokenBalance(final String token, final BigInteger decimals) {
        return _loadContract(ERC20.class, token)
                .balanceOf(credentials.getAddress())
                .sendAsync()
                .thenApply(balanceOf -> _fromWei(balanceOf, decimals));
    }

    @Override
    public CompletableFuture<BigDecimal> getTokenBalance(final String token) {
        return _getDecimals(token)
                .thenCompose(decimals -> _getTokenBalance(token, decimals));
    }

    public CompletableFuture<BigDecimal> _getTokenAmountsOut(final String swapRouter, final String tokenA, final BigInteger tokenADecimals, final String tokenB, final BigInteger tokenBDecimals, final BigDecimal amount) {
        if (Objects.equals(tokenA, tokenB)) {
            return CompletableFuture.completedFuture(BigDecimal.ONE);
        }
        final BigInteger amountIn = _toWei(amount, tokenADecimals);
        final List<String> path = Arrays.asList(tokenA, tokenB);
        return _loadContract(Router.class, swapRouter)
                .getAmountsOut(amountIn, path)
                .sendAsync()
                .thenApply(amounts -> (BigInteger) amounts.get(1))
                .thenApply(amountOut -> _fromWei(amountOut, tokenBDecimals));
    }

    @Override
    public CompletableFuture<BigDecimal> getTokenAmountsOut(final String swapRouter, final String tokenA, final String tokenB, final BigDecimal amount) {
        final CompletableFuture<BigInteger> tokenADecimals = _getDecimals(tokenA);
        final CompletableFuture<BigInteger> tokenBDecimals = _getDecimals(tokenB);
        return CompletableFuture.allOf(tokenADecimals, tokenBDecimals)
                .thenCompose(none -> _getTokenAmountsOut(swapRouter, tokenA, _get(tokenADecimals), tokenB, _get(tokenBDecimals), amount));

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

    private CompletableFuture<BigDecimal> _getTokenPrice(final String tokenA, final BigInteger tokenADecimals, final String tokenB, final BigInteger tokenBDecimals, final String swapRouter) {
        return _getTokenAmountsOut(swapRouter, tokenA, tokenADecimals, tokenB, tokenBDecimals, BigDecimal.ONE);
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
        final BigInteger val = _toWei(value, BigInteger.valueOf(18)); //TODO : Fixed value
        return Transaction.createFunctionCallTransaction(from, nonce, gasPrice, gasLimit, to, val, data);
    }

    private CompletableFuture<TransactionReceipt> _sendTransaction(final String contractAddress, final String data, final BigDecimal value, final String func) {
        return web3j.ethEstimateGas(_createTransaction(contractAddress, data, value))
                .sendAsync()
                .thenApply(resp -> _throwIfError("ethEstimateGas", resp))
                .thenCompose(resp -> {
                    try {
                        final BigInteger gasPrice = _get(_getGasPrice());
                        log.info("Tx \"{}\" : Estimate gas limit = {}, gas price = {}", func, resp.getAmountUsed(), gasPrice);
                        final EthSendTransaction tx = transactionManager.sendTransaction(
                                gasPrice,
                                resp.getAmountUsed(),
                                contractAddress,
                                data,
                                _toWei(value, BigInteger.valueOf(18)) //TODO : Fixed value
                        );
                        _throwIfError("transactionManager.sendTransaction", tx);
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
        return _getDecimals(token)
                .thenCompose(decimals -> {
                    return _sendTransaction(
                            token,
                            _loadContract(ERC20.class, token)
                                    .approve(contractAddress, _toWei(amount, decimals))
                                    .encodeFunctionCall(),
                            BigDecimal.ZERO,
                            "ERC20.approve(spender, amount)"
                    );
                });
    }

    private BigDecimal getAmountOutMin(final BigDecimal amount, final double slippage) {
        final BigDecimal n100 = BigDecimal.valueOf(100);
        final BigDecimal base = amount.divide(n100).multiply(BigDecimal.valueOf(slippage));
        return amount.subtract(base);
    }

    private CompletableFuture<TransactionReceipt> _swap(final String swapRouter, final String tokenA, final String tokenB, final BigDecimal amount, final double slippage, final int deadlineMinutes) {
        log.info("_swap(swapRouter, tokenA, tokenB, amount)");
        final CompletableFuture<BigInteger> tokenADecimals = _getDecimals(tokenA);
        final CompletableFuture<BigInteger> tokenBDecimals = _getDecimals(tokenB);
        return CompletableFuture.allOf(tokenADecimals, tokenBDecimals)
                .thenCompose(none -> {
                    final BigInteger aDecimals = _get(tokenADecimals);
                    final BigInteger bDecimals = _get(tokenBDecimals);
                    return _getTokenAmountsOut(swapRouter, tokenA, aDecimals, tokenB, bDecimals, amount)
                            .thenCompose(receiveAmount -> {
                                final BigInteger amountOut = _toWei(getAmountOutMin(receiveAmount, slippage), bDecimals);
                                final BigInteger deadline = BigInteger.valueOf(Instant.now().plusSeconds(60 * deadlineMinutes).toEpochMilli());
                                final BigInteger amountIn = _toWei(amount, aDecimals);
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
                });
    }

    @Override
    public CompletableFuture<BigDecimal> getTokenAllowance(final String token, final String contractAddress) {
        return _getDecimals(token)
                .thenCompose(decimals -> {
                    return _loadContract(ERC20.class, token)
                            .allowance(credentials.getAddress(), contractAddress)
                            .sendAsync()
                            .thenApply(allowance -> _fromWei(allowance, decimals));
                });
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
                .thenApply(resp -> _fromWei(resp.getBalance(), BigInteger.valueOf(18))); //TODO : Fixed value
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
        return _getDecimals(token)
                .thenCompose(decimals -> {
                    return _sendTransaction(
                            token,
                            _loadContract(ERC20.class, token)
                                    .transfer(recipient, _toWei(amount, decimals))
                                    .encodeFunctionCall(),
                            BigDecimal.ZERO,
                            "ERC20.transfer(recipient, amount)"
                    );
                });
    }

    @Override
    public CompletableFuture<TransactionReceipt> fillGas(final BigDecimal amount) {
        final String gasToken = network.getGasWrappedToken();
        return _getDecimals(gasToken)
                .thenCompose(decimals -> {
                    return _sendTransaction(
                            gasToken,
                            _loadContract(Wrapped.class, gasToken)
                                    .withdraw(_toWei(amount, decimals))
                                    .encodeFunctionCall(),
                            BigDecimal.ZERO,
                            "Wrapped.withdraw(wad)"
                    );
                });
    }

    @Override
    public CompletableFuture<TransactionReceipt> tokenSwapAndFillGas(final String swapRouter, final String token, final BigDecimal amount) {
        final String gasToken = network.getGasWrappedToken();
        return getTokenAmountsOutMin(swapRouter, token, gasToken, amount)
                .thenCompose(amountOut -> tokenSwapAndAutoApprove(swapRouter, token, gasToken, amount)
                        .thenCompose(tx -> fillGas(amountOut))
                );
    }

    @Override
    public void onBlock(final Consumer<EthBlock.Block> consumer, final long throttleMillisec) {
        if (onBlock != null) {
            onBlock.dispose();
        }
        onBlock = web3j.blockFlowable(false)
                .throttleWithTimeout(throttleMillisec, TimeUnit.MILLISECONDS)
                .subscribe(ethBlock -> {
                    if (!ethBlock.hasError()) {
                        consumer.accept(ethBlock.getBlock());
                    }
                });
    }

    @Override
    public void onBlock(final Consumer<EthBlock.Block> consumer) {
        onBlock(consumer, 300);
    }

    @Override
    public void onTransfer(final String token, final Consumer<TransferEvent> consumer) {
        Disposable disposable = onTransferMap.get(token);
        if (disposable != null) {
            disposable.dispose();
        }
        disposable = _loadContract(ERC20.class, token)
                .transferEventFlowable(DefaultBlockParameterName.LATEST, DefaultBlockParameterName.LATEST)
                .onErrorReturnItem(new ERC20.TransferEventResponse())
                .filter(event -> Objects.equals(event.from, getWalletAddress()) || Objects.equals(event.to, getWalletAddress()))
                .map(event -> TransferEvent.builder()
                        .token(token)
                        .from(event.from)
                        .to(event.to)
                        .value(event.value)
                        .log(event.log)
                        .build()
                )
                .subscribe(event -> {
                    log.info("Transfer => from \"{}\" to \"{}\" value {} log {}", event.getFrom(), event.getTo(), event.getValue(), event.getLog());
                    consumer.accept(event);
                });
        onTransferMap.put(token, disposable);
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
            if (transactionHash == null || transactionHash.isEmpty()) {
                throw new IllegalArgumentException("Required transactionHash");
            }
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