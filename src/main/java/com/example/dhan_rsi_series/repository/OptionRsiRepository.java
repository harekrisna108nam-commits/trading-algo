package com.example.dhan_rsi_series.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.dhan_rsi_series.entity.OptionRsi;

public interface OptionRsiRepository extends JpaRepository<OptionRsi, Long> {
	List<OptionRsi> findByTimeframeAndOptionType(String timeframe, String optionType);
	
    @Query(value = """
            SELECT close FROM option_rsi
            WHERE security_id = :securityId
            AND timeframe = '1M'
            ORDER BY candle_time DESC
            LIMIT 20
            """, nativeQuery = true)
        List<Double> findRecentCloses(
                @Param("securityId") int securityId,
                @Param("timeframe") String timeframe
        );

    @Query(value = """
    	    SELECT * FROM option_rsi
    	    WHERE timeframe = :timeframe
    	    ORDER BY candle_time DESC
    	    LIMIT 1
    	    """, nativeQuery = true)
    	OptionRsi findLatestByTimeframe(@Param("timeframe") String timeframe);
}