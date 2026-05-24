package com.example.dhan_rsi_series.utils;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

import com.example.dhan_rsi_series.entity.DhanSubscription;
import com.example.dhan_rsi_series.repository.DhanSubscriptionRepository;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;

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

/*@Component
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

}*/


@Slf4j
@Component
public class DhanSubscriptionStore {

    private final DhanSubscriptionRepository repo;

    // =========================================================
    // MEMORY CACHE
    // =========================================================

    // securityId -> full subscription
    private final Map<Integer, DhanSubscription> securityMap =
            new ConcurrentHashMap<>();

    // expiry ordering
    private final Set<LocalDate> expirySet =
            ConcurrentHashMap.newKeySet();

    public DhanSubscriptionStore(
            DhanSubscriptionRepository repo
    ) {
        this.repo = repo;
    }

    // =========================================================
    // LOAD ALL SUBSCRIPTIONS
    // =========================================================

    @PostConstruct
    public void load() {

        List<DhanSubscription> list =
                repo.findAll();

        for (DhanSubscription s : list) {

            int securityId =
                    Integer.parseInt(
                            s.getSecurityId()
                    );

            securityMap.put(
                    securityId,
                    s
            );

            if (s.getExpiryDate() != null && !optionTypeOf(securityId).equalsIgnoreCase("FUTURE")) {

                expirySet.add(
                        s.getExpiryDate()
                );
            }
        }

        log.info(
                "✅ Loaded {} subscriptions",
                securityMap.size()
        );
    }

    // =========================================================
    // ADD WITH GAMMA
    // =========================================================

    public void add(
            String exchange,
            String securityId,
            double gamma,
            String optionType,
            int strike,
            LocalDate expiry
    ) {

        DhanSubscription existing =
                repo.findByExchangeAndSecurityId(
                        exchange,
                        securityId
                ).orElse(null);

        if (existing == null) {

            DhanSubscription s =
                    new DhanSubscription();

            s.setExchange(exchange);
            s.setSecurityId(securityId);
            s.setGamma(gamma);
            s.setOptionType(optionType);
            s.setStrike(strike);
            s.setExpiryDate(expiry);

            existing = repo.save(s);
        }

        int secId =
                Integer.parseInt(securityId);

        securityMap.put(
                secId,
                existing
        );

        if (expiry != null && !optionType.equalsIgnoreCase("FUTURE")) {
            expirySet.add(expiry);
        }
    }

    // =========================================================
    // ADD WITHOUT GAMMA
    // =========================================================

    public void add(
            String exchange,
            String securityId,
            String optionType,
            int strike,
            LocalDate expiry
    ) {

        DhanSubscription existing =
                repo.findByExchangeAndSecurityId(
                        exchange,
                        securityId
                ).orElse(null);

        if (existing == null) {

            DhanSubscription s =
                    new DhanSubscription();

            s.setExchange(exchange);
            s.setSecurityId(securityId);
            s.setOptionType(optionType);
            s.setStrike(strike);
            s.setExpiryDate(expiry);

            existing = repo.save(s);
        }

        int secId =
                Integer.parseInt(securityId);

        securityMap.put(
                secId,
                existing
        );

        if (expiry != null && !optionType.equalsIgnoreCase("FUTURE")) {
            expirySet.add(expiry);
        }
    }

    // =========================================================
    // FAST LOOKUPS
    // =========================================================

    public String optionTypeOf(int securityId) {

        DhanSubscription s =
                securityMap.get(securityId);

        return s != null
                ? s.getOptionType()
                : null;
    }

    public DhanSubscription subscription(
            int securityId
    ) {
        return securityMap.get(securityId);
    }

    public LocalDate expiryOf(
            int securityId
    ) {

        DhanSubscription s =
                securityMap.get(securityId);

        return s != null
                ? s.getExpiryDate()
                : null;
    }

    public Integer strikeOf(
            int securityId
    ) {

        DhanSubscription s =
                securityMap.get(securityId);

        return s != null
                ? s.getStrike()
                : null;
    }

    // =========================================================
    // ALL SUBSCRIPTIONS
    // =========================================================

    public Set<DhanSubscription> all() {

        return new HashSet<>(
                securityMap.values()
        );
    }

    // =========================================================
    // SORTED EXPIRIES
    // =========================================================

    public List<LocalDate> allExpiriesSorted() {

        return expirySet.stream()

                .sorted()

                .toList();
    }

    // =========================================================
    // CURRENT EXPIRY
    // =========================================================

    public LocalDate currentExpiry() {

        List<LocalDate> list =
                allExpiriesSorted();

        return list.isEmpty()
                ? null
                : list.get(0);
    }

    // =========================================================
    // NEXT EXPIRY
    // =========================================================

    public LocalDate nextExpiry() {

        List<LocalDate> list =
                allExpiriesSorted();

        return list.size() > 1
                ? list.get(1)
                : null;
    }

    // =========================================================
    // FAR EXPIRY
    // =========================================================

    public LocalDate farExpiry() {

        List<LocalDate> list =
                allExpiriesSorted();

        return list.size() > 2
                ? list.get(2)
                : null;
    }

    // =========================================================
    // CHECKS
    // =========================================================

    public boolean isCurrentExpiry(
            LocalDate expiry
    ) {

        LocalDate current =
                currentExpiry();

        return current != null &&
               current.equals(expiry);
    }

    public boolean isNextExpiry(
            LocalDate expiry
    ) {

        LocalDate next =
                nextExpiry();

        return next != null &&
               next.equals(expiry);
    }

    public boolean isFarExpiry(
            LocalDate expiry
    ) {

        LocalDate far =
                farExpiry();

        return far != null &&
               far.equals(expiry);
    }
}
