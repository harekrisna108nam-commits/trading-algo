package com.example.dhan_rsi_series.utils;

import java.time.LocalDate;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

import com.example.dhan_rsi_series.entity.DhanSubscription;
import com.example.dhan_rsi_series.repository.DhanSubscriptionRepository;

import jakarta.annotation.PostConstruct;

/*@Component
public class DhanSubscriptionStore {

    private final Set<String> instruments = ConcurrentHashMap.newKeySet();

    public void add(String exchange, String securityId) {
        instruments.add(exchange + "|" + securityId);
    }

    public Set<String> snapshot() {
        return Set.copyOf(instruments);
    }

    public boolean isEmpty() {
        return instruments.isEmpty();
    }
}*/

@Component
public class DhanSubscriptionStore {

    private final DhanSubscriptionRepository repo;

    // securityId → optionType
    private final Map<Integer, String> optionMap =
        new ConcurrentHashMap<>();

    public DhanSubscriptionStore(DhanSubscriptionRepository repo) {
        this.repo = repo;
    }

    @PostConstruct
    public void load() {
        repo.findAll().forEach(s ->
            optionMap.put(
                Integer.parseInt(s.getSecurityId()),
                s.getOptionType()
            )
        );
    }

    public void add(
            String exchange,
            String securityId,
            double gamma,
            String optionType,
            int strike,
            LocalDate expiry
    ) {

        if (!repo.existsByExchangeAndSecurityId(exchange, securityId)) {

            DhanSubscription s = new DhanSubscription();
            s.setExchange(exchange);
            s.setSecurityId(securityId);
            s.setGamma(gamma);
            s.setOptionType(optionType);
            s.setStrike(strike);
            s.setExpiryDate(expiry);
            repo.save(s);
        }

        optionMap.put(
            Integer.parseInt(securityId),
            optionType
        );
    }
    
    public void add(
            String exchange,
            String securityId,
            String optionType,
            int strike,
            LocalDate expiry
            
    ) {

        if (!repo.existsByExchangeAndSecurityId(exchange, securityId)) {

            DhanSubscription s = new DhanSubscription();
            s.setExchange(exchange);
            s.setSecurityId(securityId);
            s.setOptionType(optionType);
            s.setStrike(strike);
            s.setExpiryDate(expiry);
            repo.save(s);
        }

        optionMap.put(
            Integer.parseInt(securityId),
            optionType
        );
    }

    public String optionTypeOf(int securityId) {
        return optionMap.get(securityId);
    }

    public Set<DhanSubscription> all() {
        return Set.copyOf(repo.findAll());
    }

}
