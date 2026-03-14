package com.example.dhan_rsi_series.controller;

import java.util.List;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.dhan_rsi_series.model.HistoricalCandleRequest;
import com.example.dhan_rsi_series.model.RsiCandle;
import com.example.dhan_rsi_series.service.RsiSeriesService;
import com.example.dhan_rsi_series.utils.RsiExcelGenerator;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/rsi")
@RequiredArgsConstructor
public class RsiSeriesController {

    private final RsiSeriesService rsiSeriesService;

    @GetMapping("/all")
    public Mono<List<RsiCandle>> getAllRsi(
    		@RequestParam String securityId,
            @RequestParam(defaultValue = "1") int interval,
            @RequestParam(defaultValue = "14") int period) {

        return rsiSeriesService
                .calculateRsiSeries(securityId, interval, period);
    }
    
    @PostMapping(value = "/excel", produces = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    public Mono<ResponseEntity<byte[]>> downloadRsiExcel(
            @RequestBody HistoricalCandleRequest request) {

        return rsiSeriesService
                .calculateRsiSeries(request)
                .map(rsiList -> {

                    byte[] excelBytes =
                            RsiExcelGenerator.generateExcel(rsiList);

                    return ResponseEntity.ok()
                            .header(HttpHeaders.CONTENT_DISPOSITION,
                                    "attachment; filename=rsi_" + request.getSecurityId() + ".xlsx")
                            .contentType(MediaType.APPLICATION_OCTET_STREAM)
                            .contentLength(excelBytes.length)
                            .body(excelBytes);
                });
    }
}

