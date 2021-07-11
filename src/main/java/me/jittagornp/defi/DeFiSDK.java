/*
 * Copyright 2021-Current jittagornp.me
 */
package me.jittagornp.defi;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import me.jittagornp.defi.exception.ResponseErrorException;
import me.jittagornp.defi.model.TokenInfo;
import lombok.extern.slf4j.Slf4j;
import me.jittagornp.defi.smartcontract.*;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.Web3jService;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.Response;
import org.web3j.protocol.core.methods.request.Transaction;
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
import java.math.MathContext;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author jittagornp
 */
@Slf4j
public class DeFiSDK implements DeFi {

    private int defaultSwapDeadlineMinutes = 10;
    private double defaultSwapSlippage = 0.5;
    private double tokenAutoApproveNTimes = 3;

    private final Web3j web3j;
    private final Credentials credentials;
    private final TransactionManager transactionManager;
    private final ContractGasProvider gasProvider = new DefaultGasProvider();
    private final Map<String, Object> cached = new HashMap<>();

    protected DeFiSDK(final Credentials credentials, final Web3jService web3jService) {
        this.credentials = credentials;
        this.web3j = Web3j.build(web3jService);
        this.transactionManager = new RawTransactionManager(web3j, credentials);
        final String address = credentials.getAddress();
        log.info("Wallet address : {}...{}", address.substring(0, 6), address.substring(address.length() - 4));
    }

    public static DeFiSDK bsc(final Credentials credentials) {
        return new DeFiSDK(credentials, new HttpService("https://bsc-dataseed1.binance.org"));
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
    public CompletableFuture<TokenInfo> getTokenInfo(final String token, final String tokenPair, final String factory) {
        final ERC20 erc20 = _loadContract(ERC20.class, token);
        final CompletableFuture<String> name = erc20.name()
                .sendAsync();
        final CompletableFuture<String> symbol = erc20.symbol()
                .sendAsync();
        final CompletableFuture<BigInteger> totalSupply = erc20.totalSupply()
                .sendAsync();
        final CompletableFuture<BigDecimal> balanceOf = erc20.balanceOf(credentials.getAddress())
                .sendAsync()
                .thenApply(balance -> _fromWei(balance));
        final CompletableFuture<BigDecimal> price = getTokenPrice(token, tokenPair, factory);
        final CompletableFuture<BigInteger> decimals = erc20.decimals()
                .sendAsync();
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
    public CompletableFuture<List<TokenInfo>> getTokenInfoList(final List<String> tokens, final Function<String, String> tokenPair, final Function<String, String> tokenFactory) {
        final List<CompletableFuture<TokenInfo>> list = tokens.stream()
                .map(token -> getTokenInfo(token, tokenPair.apply(token), tokenFactory.apply(token)))
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
    public CompletableFuture<List<TokenInfo>> getTokenInfoList(final List<String> tokens, final String tokenPair, final String factory) {
        return getTokenInfoList(tokens, (token) -> tokenPair, (token) -> factory);
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
                .thenApply(balance -> _fromWei(balance));
    }

    @Override
    public CompletableFuture<BigDecimal> getTokenPrice(final String tokenA, final String tokenB, final String factory) {
        if (Objects.equals(tokenA, tokenB)) {
            return CompletableFuture.supplyAsync(() -> BigDecimal.ONE);
        }
        return _getPair(factory, tokenA, tokenB)
                .thenCompose(pair -> _loadContract(Pairs.class, pair).getReserves().sendAsync())
                .thenApply(resp -> {
                    final BigDecimal a = new BigDecimal(resp.component1());
                    final BigDecimal b = new BigDecimal(resp.component2());
                    return b.divide(a, MathContext.DECIMAL32);
                });
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
                .thenApply(resp -> {
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
                        return new EmptyTransactionReceipt(tx.getTransactionHash());
                    } catch (IOException e) {
                        log.error("Send Transaction error ", e);
                        throw new RuntimeException(e);
                    }
                });
    }

    @Override
    public CompletableFuture<TransactionReceipt> tokenApprove(final String token, final BigDecimal amount, final String contractAddress) {
        final BigInteger amountIn = _toWei(amount);
        return _sendTransaction(
                token,
                _loadContract(ERC20.class, token)
                        .approve(contractAddress, amountIn)
                        .encodeFunctionCall(),
                BigDecimal.ZERO,
                "ERC20.approve(spender, amount)"
        );
    }

    private CompletableFuture<TransactionReceipt> _swap(final String router, final String tokenA, final String tokenB, final BigDecimal amount, final double slippage, final int deadlineMinutes) {
        log.info("_swap(router, tokenA, tokenB, amount)");
        final BigInteger amountIn = _toWei(amount);
        final List<String> path = Arrays.asList(tokenA, tokenB);
        final Router contract = _loadContract(Router.class, router);
        return contract
                .getAmountsOut(amountIn, path)
                .sendAsync()
                .thenCompose(amounts -> {
                    final BigDecimal receiveAmount = _fromWei((BigInteger) amounts.get(1));
                    final BigDecimal n100 = BigDecimal.valueOf(100);
                    final BigDecimal base = receiveAmount.divide(n100).multiply(BigDecimal.valueOf(slippage));
                    final BigDecimal min = receiveAmount.subtract(base);
                    final BigInteger amountOutMin = _toWei(min);
                    final BigInteger deadline = BigInteger.valueOf(Instant.now().plusSeconds(60 * deadlineMinutes).toEpochMilli());
                    return _sendTransaction(
                            router,
                            contract.swapExactTokensForTokens(
                                    amountIn,
                                    amountOutMin,
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
                .thenApply(allowance -> _fromWei(allowance));
    }

    @Override
    public CompletableFuture<TransactionReceipt> swapToken(final String router, final String tokenA, final String tokenB, final BigDecimal amount, final double slippage, final int deadlineMinutes) {
        return getTokenAllowance(tokenA, router)
                .thenCompose(allowance -> {
                    log.info("Allowance Token \"{}\" amount {} for Contract \"{}\"", tokenA, allowance, router);
                    boolean isLessThan = allowance.compareTo(amount) < 0;
                    if (isLessThan) {
                        throw new RuntimeException("Please call .tokenApprove(token, amount, contractAddress) before swap");
                    }
                    return _swap(router, tokenA, tokenB, amount, slippage, deadlineMinutes);
                });
    }

    @Override
    public CompletableFuture<TransactionReceipt> swapToken(final String router, final String tokenA, final String tokenB, final BigDecimal amount, final double slippage) {
        return swapToken(router, tokenA, tokenB, amount, slippage, defaultSwapDeadlineMinutes);
    }

    @Override
    public CompletableFuture<TransactionReceipt> swapToken(final String router, final String tokenA, final String tokenB, final BigDecimal amount) {
        return swapToken(router, tokenA, tokenB, amount, defaultSwapSlippage);
    }

    @Override
    public CompletableFuture<TransactionReceipt> swapTokenAndAutoApprove(final String router, final String tokenA, final String tokenB, final BigDecimal amount, final double slippage, final int deadlineMinutes) {
        return getTokenAllowance(tokenA, router)
                .thenCompose(allowance -> {
                    log.info("Allowance Token \"{}\" amount {} for Contract \"{}\"", tokenA, allowance, router);
                    boolean isLessThan = allowance.compareTo(amount) < 0;
                    if (isLessThan) {
                        final BigDecimal times = BigDecimal.valueOf(tokenAutoApproveNTimes);
                        final BigDecimal approvedAmount = times.multiply(amount);
                        log.info("Approved amount = {}", approvedAmount);
                        return tokenApprove(tokenA, approvedAmount, router)
                                .thenCompose(tx -> _swap(router, tokenA, tokenB, amount, slippage, deadlineMinutes));
                    }
                    return _swap(router, tokenA, tokenB, amount, slippage, deadlineMinutes);
                });
    }

    @Override
    public CompletableFuture<TransactionReceipt> swapTokenAndAutoApprove(final String router, final String tokenA, final String tokenB, final BigDecimal amount, final double slippage) {
        return swapTokenAndAutoApprove(router, tokenA, tokenB, amount, slippage, defaultSwapDeadlineMinutes);
    }

    @Override
    public CompletableFuture<TransactionReceipt> swapTokenAndAutoApprove(final String router, final String tokenA, final String tokenB, final BigDecimal amount) {
        return swapTokenAndAutoApprove(router, tokenA, tokenB, amount, defaultSwapSlippage);
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
                .thenApply(gasPrice -> _fromGwei(gasPrice));
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
    public CompletableFuture<TransactionReceipt> fillGas(final String token, final BigDecimal amount) {
        return _sendTransaction(
                token,
                _loadContract(Wrapped.class, token)
                        .withdraw(_toWei(amount))
                        .encodeFunctionCall(),
                BigDecimal.ZERO,
                "Wrapped.withdraw(wad)"
        );
    }

}
