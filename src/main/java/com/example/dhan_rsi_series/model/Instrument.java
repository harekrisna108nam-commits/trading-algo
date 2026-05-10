package com.example.dhan_rsi_series.model;

public class Instrument {

    private String securityId;
    private String symbolName;
    private Integer strikePrice;
    private String optionType;
    private String expiryDate;
    private String exchangeSegment;

    public Instrument(String securityId,
                      String symbolName,
                      int strikePrice,
                      String optionType,
                      String expiryDate,
                      String exchangeSegment) {

        this.securityId = securityId;
        this.symbolName = symbolName;
        this.strikePrice = strikePrice;
        this.optionType = optionType;
        this.expiryDate = expiryDate;
        this.exchangeSegment = exchangeSegment;
    }

    public String getSecurityId() { return securityId; }
    public String getSymbolName() { return symbolName; }
    public int getStrikePrice() { return strikePrice; }
    public String getOptionType() { return optionType; }
    public String getExpiryDate() { return expiryDate; }
    public String getExchangeSegment() { return exchangeSegment; }
}