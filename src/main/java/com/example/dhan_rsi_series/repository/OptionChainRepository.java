package com.example.dhan_rsi_series.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.example.dhan_rsi_series.entity.OptionChain;

public interface OptionChainRepository extends JpaRepository<OptionChain, String> {
	@Query(value = """
	        SELECT oc.*
	        FROM option_chain oc
	        INNER JOIN (
	            SELECT security_id, MAX(candle_time) AS max_time
	            FROM option_chain
	            GROUP BY security_id
	        ) latest
	        ON oc.security_id = latest.security_id
	        AND oc.candle_time = latest.max_time
	        """, nativeQuery = true)
	    List<OptionChain> findLatestRecordForEachSecurityId();
}
