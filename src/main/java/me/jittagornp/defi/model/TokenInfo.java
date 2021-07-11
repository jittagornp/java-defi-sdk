/*
 * Copyright 2021-Current jittagornp.me
 */
package me.jittagornp.defi.model;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.math.BigInteger;

/**
 * @author jittagornp
 */
@Data
@Builder
public class TokenInfo {

    private String address;

    private String name;

    private String symbol;

    private BigDecimal totalSupply;

    private BigDecimal balance;

    private BigDecimal price;

    private BigInteger decimals;

    private BigDecimal value;

}
