/*
 * package com.example.dhan_rsi_series.utils;
 * 
 * 
 * import java.io.StringReader; import java.time.LocalDate; import
 * java.time.temporal.ChronoUnit; import java.util.HashMap; import
 * java.util.Map;
 * 
 * import org.springframework.stereotype.Service;
 * 
 * import com.example.dhan_rsi_series.model.Instrument; import
 * com.opencsv.CSVReader;
 * 
 * @Service public class InstrumentParser {
 * 
 * public Map<String, Instrument> parse(String csvData) {
 * 
 * Map<String, Instrument> instrumentMap = new HashMap<>();
 * 
 * try {
 * 
 * CSVReader reader = new CSVReader(new StringReader(csvData));
 * 
 * String[] line; reader.readNext(); // skip header
 * 
 * while ((line = reader.readNext()) != null ) {//&& !line[13].isEmpty()
 * 
 * String exchId = line[0]; String segment = line[1]; String securityId =
 * line[2]; String instr = line[4]; String symbolName = line[7]; String
 * expiryDateStr = line[12]; double strikePrice = Double.parseDouble(line[13]);
 * String optionTypeRaw = line[14]; String optionType =
 * mapOptionType(optionTypeRaw); String exchangeSegment =
 * getExchangeSegment(exchId, segment);
 * 
 * // Convert String → LocalDate LocalDate expiryDate =
 * LocalDate.parse(expiryDateStr);
 * 
 * // Current date LocalDate today = LocalDate.now();
 * 
 * // Calculate days remaining long daysToExpiry =
 * ChronoUnit.DAYS.between(today, expiryDate); // (strikePrice<=20000 &&
 * strikePrice>=30000) && if (exchangeSegment.equalsIgnoreCase("NSE_FNO") &&
 * (strikePrice>=20000 && strikePrice<=26000) &&
 * instr.equalsIgnoreCase("OPTIDX") && daysToExpiry<=7) { Instrument instrument
 * = new Instrument( securityId, symbolName, strikePrice, optionType,
 * expiryDateStr, exchangeSegment );
 * 
 * instrumentMap.put(securityId, instrument); } }
 * 
 * } catch (Exception e) { e.printStackTrace(); }
 * 
 * return instrumentMap; }
 * 
 * private String getExchangeSegment(String exchId, String segment) {
 * 
 * if ("D".equals(segment)) { return exchId + "_FNO"; }
 * 
 * if ("C".equals(segment)) { return exchId + "_EQ"; }
 * 
 * if ("M".equals(segment)) { return exchId + "_COMM"; }
 * 
 * return exchId + "_" + segment; }
 * 
 * private String mapOptionType(String optionType) {
 * 
 * if ("CE".equalsIgnoreCase(optionType)) { return "CALL"; }
 * 
 * if ("PE".equalsIgnoreCase(optionType)) { return "PUT"; }
 * 
 * return "NA"; } }
 */