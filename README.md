# Java DeFi-SDK

![](./DeFi.jpeg)

> Java SDK สำหรับ Decentralized Finance (Blockchain)

# Getting Started

### 1. สร้าง Wallet (Generate Your Wallet)

Run [code](src/test/java/me/jittagornp/defi/GenerateWallet.java)

```
/src/test/java/me/jittagornp/defi/GenerateWallet.java
```

ก็จะได้ Wallet เป็นไฟล์ .json ขึ้นมาอยู่ใน Path ~/crypto-wallet/ (Home Directory)

***หมายเหตุ***

อย่าลืมเปลี่ยน Wallet Password ก่อน Run code น่ะ

### 2. ทดสอบ

Run [code](src/test/java/me/jittagornp/defi/DeFiTest.java)

```
/src/test/java/me/jittagornp/defi/DeFiTest.java
```

จะแสดงค่า Token ต่าง ๆ ออกมา

# การใช้งาน (How to use)

```java
final String WALLET_DIRECTORY = System.getProperty("user.home") + "/crypto-wallet";
final String WALLET_FILE_NAME = "UTC--2021-07-08....json";
final String WALLET_PASSWORD = "<YOUR_WALLET_PASSWORD>";
final Credentials credentials = WalletUtils.loadCredentials(WALLET_PASSWORD, new File(WALLET_DIRECTORY, WALLET_FILE_NAME));
//
// ตอนนี้รองรับแค่ Network BSC (Binance Smart Chain)
final DeFi defi = DeFiSDK.bsc(credentials)
```

# Functions

> Functions/Methods ทั้งหมด Default เป็น Asynchronous โดยใช้ Java Future

### Get Gas Price

ดูราคา Gas ปัจจุบัน ว่าราคาเท่าไหร่ (หน่วย gwei)

```java
CompletableFuture<BigDecimal> getGasPrice();
```

### Get Gas Balance

ดู Gas ใน Wallet ว่าเหลืออยู่เท่าไหร่

```java
CompletableFuture<BigDecimal> getGasBalance();
```

### Get Token Balance

ดูว่า Token (Address) นี้เหลืออยู่ใน Wallet เราเท่าไหร่

```java
CompletableFuture<BigDecimal> getTokenBalance(final String token);
```

### Get Token Price

ดูราคา Token A เทียบกับ Token B บน Factory (DEX/AMM)

```java
CompletableFuture<BigDecimal> getTokenPrice(final String tokenA, final String tokenB, final String factory);
```

### Get Token Info

ดูข้อมูล Token + เทียบราคากับ Token Pair บน Factory (DEX/AMM)

```java
CompletableFuture<TokenInfo> getTokenInfo(final String token, final String tokenPair, final String factory);
```

### Get Token Info List

ดูข้อมูล Token ทีละหลาย ๆ อัน + เทียบราคากับ Token Pair บน Factory (DEX/AMM)

```java
CompletableFuture<List<TokenInfo>> getTokenInfoList(final List<String> tokens, final Function<String, String> tokenPair, final Function<String, String> tokenFactory);
```

### Get Token Allowance

ดูจำนวน Token ที่เคย Allow ไว้ให้ Smart Contract นึงเข้าถึงได้  

```java
CompletableFuture<BigDecimal> getTokenAllowance(final String token, final String contractAddress);
```

### Token Transfer 

การโอน Token ไปยัง Wallet Address ปลายทาง (Recipient)

```java
CompletableFuture<TransactionReceipt> tokenTransfer(final String token, String recipient, final BigDecimal amount);
```

### Token Approve

การ Approve จำนวน Token ให้ Smart Contract นึงสามารถเข้าถึงได้เท่าไหร่

```java
CompletableFuture<TransactionReceipt> tokenApprove(final String token, final BigDecimal amount, final String contractAddress);
```

### Token Swap

การแลกเปลี่ยน (Swap) Token จาก A -> B บน Router (DEX/AMM)

```java
CompletableFuture<TransactionReceipt> tokenSwap(final String router, final String tokenA, final String tokenB, final BigDecimal amount, final double slippage, final int deadlineMinutes);
```

### Fill Gas

เติม Gas
```java
CompletableFuture<TransactionReceipt> fillGas(final String token, final BigDecimal amount);
```

### On Block

เรียกเมื่อมี Block เกิดใหม่

```java
void onBlock(final Consumer<EthBlock.Block> consumer);
```

ลองเอาไปประยุกต์ใช้ตามความต้องการตัวเองดูน่ะ

# คำศัพท์ 

DeFi - Decentralized Finance  
DEX - Decentralized Exchange  
AMM - Automated Money Maker   

# Credit 

Code ชุดนี้ผมได้แรงบันดาลใจมาจาก GitHub Repository นี้ [https://github.com/earthchie/DeFi-SDK](https://github.com/earthchie/DeFi-SDK)  
  
ขอขอบคุณ[คุณเอิร์ธ](https://github.com/earthchie) CEO DomeCloud ด้วยครับ ที่ทำให้ผมได้มีตัวอย่างเรียนรู้การเขียน Code เพื่อเชื่อมต่อกับ Smart Contract จนได้เขียน Code ชุดนี้ขึ้นมา ขอบคุณมาก ๆ ครับ
