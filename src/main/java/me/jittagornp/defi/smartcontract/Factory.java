package me.jittagornp.defi.smartcontract;

import io.reactivex.Flowable;
import io.reactivex.functions.Function;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.web3j.abi.EventEncoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Event;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.generated.Bytes32;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameter;
import org.web3j.protocol.core.RemoteFunctionCall;
import org.web3j.protocol.core.methods.request.EthFilter;
import org.web3j.protocol.core.methods.response.BaseEventResponse;
import org.web3j.protocol.core.methods.response.Log;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.tx.Contract;
import org.web3j.tx.TransactionManager;
import org.web3j.tx.gas.ContractGasProvider;

/**
 * <p>Auto generated code.
 * <p><strong>Do not modify!</strong>
 * <p>Please use the <a href="https://docs.web3j.io/command_line.html">web3j command line tools</a>,
 * or the org.web3j.codegen.SolidityFunctionWrapperGenerator in the 
 * <a href="https://github.com/web3j/web3j/tree/master/codegen">codegen module</a> to update.
 *
 * <p>Generated with web3j version 1.4.1.
 */
@SuppressWarnings("rawtypes")
public class Factory extends Contract {
    public static final String BINARY = "Bin file was not provided";

    public static final String FUNC_INIT_CODE_PAIR_HASH = "INIT_CODE_PAIR_HASH";

    public static final String FUNC_ALLPAIRS = "allPairs";

    public static final String FUNC_ALLPAIRSLENGTH = "allPairsLength";

    public static final String FUNC_CREATEPAIR = "createPair";

    public static final String FUNC_FEETO = "feeTo";

    public static final String FUNC_FEETOSETTER = "feeToSetter";

    public static final String FUNC_GETPAIR = "getPair";

    public static final String FUNC_SETFEETO = "setFeeTo";

    public static final String FUNC_SETFEETOSETTER = "setFeeToSetter";

    public static final Event PAIRCREATED_EVENT = new Event("PairCreated", 
            Arrays.<TypeReference<?>>asList(new TypeReference<Address>(true) {}, new TypeReference<Address>(true) {}, new TypeReference<Address>() {}, new TypeReference<Uint256>() {}));
    ;

    @Deprecated
    protected Factory(String contractAddress, Web3j web3j, Credentials credentials, BigInteger gasPrice, BigInteger gasLimit) {
        super(BINARY, contractAddress, web3j, credentials, gasPrice, gasLimit);
    }

    protected Factory(String contractAddress, Web3j web3j, Credentials credentials, ContractGasProvider contractGasProvider) {
        super(BINARY, contractAddress, web3j, credentials, contractGasProvider);
    }

    @Deprecated
    protected Factory(String contractAddress, Web3j web3j, TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit) {
        super(BINARY, contractAddress, web3j, transactionManager, gasPrice, gasLimit);
    }

    protected Factory(String contractAddress, Web3j web3j, TransactionManager transactionManager, ContractGasProvider contractGasProvider) {
        super(BINARY, contractAddress, web3j, transactionManager, contractGasProvider);
    }

    public List<PairCreatedEventResponse> getPairCreatedEvents(TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = extractEventParametersWithLog(PAIRCREATED_EVENT, transactionReceipt);
        ArrayList<PairCreatedEventResponse> responses = new ArrayList<PairCreatedEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            PairCreatedEventResponse typedResponse = new PairCreatedEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.token0 = (String) eventValues.getIndexedValues().get(0).getValue();
            typedResponse.token1 = (String) eventValues.getIndexedValues().get(1).getValue();
            typedResponse.pair = (String) eventValues.getNonIndexedValues().get(0).getValue();
            typedResponse.a = (BigInteger) eventValues.getNonIndexedValues().get(1).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public Flowable<PairCreatedEventResponse> pairCreatedEventFlowable(EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(new Function<Log, PairCreatedEventResponse>() {
            @Override
            public PairCreatedEventResponse apply(Log log) {
                Contract.EventValuesWithLog eventValues = extractEventParametersWithLog(PAIRCREATED_EVENT, log);
                PairCreatedEventResponse typedResponse = new PairCreatedEventResponse();
                typedResponse.log = log;
                typedResponse.token0 = (String) eventValues.getIndexedValues().get(0).getValue();
                typedResponse.token1 = (String) eventValues.getIndexedValues().get(1).getValue();
                typedResponse.pair = (String) eventValues.getNonIndexedValues().get(0).getValue();
                typedResponse.a = (BigInteger) eventValues.getNonIndexedValues().get(1).getValue();
                return typedResponse;
            }
        });
    }

    public Flowable<PairCreatedEventResponse> pairCreatedEventFlowable(DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(PAIRCREATED_EVENT));
        return pairCreatedEventFlowable(filter);
    }

    public RemoteFunctionCall<byte[]> INIT_CODE_PAIR_HASH() {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(FUNC_INIT_CODE_PAIR_HASH, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Bytes32>() {}));
        return executeRemoteCallSingleValueReturn(function, byte[].class);
    }

    public RemoteFunctionCall<String> allPairs(BigInteger a) {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(FUNC_ALLPAIRS, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Uint256(a)), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Address>() {}));
        return executeRemoteCallSingleValueReturn(function, String.class);
    }

    public RemoteFunctionCall<BigInteger> allPairsLength() {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(FUNC_ALLPAIRSLENGTH, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>() {}));
        return executeRemoteCallSingleValueReturn(function, BigInteger.class);
    }

    public RemoteFunctionCall<TransactionReceipt> createPair(String tokenA, String tokenB) {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(
                FUNC_CREATEPAIR, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(160, tokenA), 
                new org.web3j.abi.datatypes.Address(160, tokenB)), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<String> feeTo() {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(FUNC_FEETO, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Address>() {}));
        return executeRemoteCallSingleValueReturn(function, String.class);
    }

    public RemoteFunctionCall<String> feeToSetter() {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(FUNC_FEETOSETTER, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Address>() {}));
        return executeRemoteCallSingleValueReturn(function, String.class);
    }

    public RemoteFunctionCall<String> getPair(String a, String b) {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(FUNC_GETPAIR, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(160, a),
                new org.web3j.abi.datatypes.Address(160, b)),
                Arrays.<TypeReference<?>>asList(new TypeReference<Address>() {}));
        return executeRemoteCallSingleValueReturn(function, String.class);
    }

    public RemoteFunctionCall<TransactionReceipt> setFeeTo(String _feeTo) {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(
                FUNC_SETFEETO, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(160, _feeTo)), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<TransactionReceipt> setFeeToSetter(String _feeToSetter) {
        final org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(
                FUNC_SETFEETOSETTER, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(160, _feeToSetter)), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    @Deprecated
    public static Factory load(String contractAddress, Web3j web3j, Credentials credentials, BigInteger gasPrice, BigInteger gasLimit) {
        return new Factory(contractAddress, web3j, credentials, gasPrice, gasLimit);
    }

    @Deprecated
    public static Factory load(String contractAddress, Web3j web3j, TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit) {
        return new Factory(contractAddress, web3j, transactionManager, gasPrice, gasLimit);
    }

    public static Factory load(String contractAddress, Web3j web3j, Credentials credentials, ContractGasProvider contractGasProvider) {
        return new Factory(contractAddress, web3j, credentials, contractGasProvider);
    }

    public static Factory load(String contractAddress, Web3j web3j, TransactionManager transactionManager, ContractGasProvider contractGasProvider) {
        return new Factory(contractAddress, web3j, transactionManager, contractGasProvider);
    }

    public static class PairCreatedEventResponse extends BaseEventResponse {
        public String token0;

        public String token1;

        public String pair;

        public BigInteger a;
    }
}
