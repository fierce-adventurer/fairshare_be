package com.fairshare.expense.engine;

import com.fairshare.shared.exception.BadRequestException;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class SplitCalculator {

    public record AllocationResult(UUID userId, long amountMinor) {}
    public record WeightedUser(UUID userId, double weight) {}

    public List<AllocationResult> allocateByWeights(long total, List<WeightedUser> weighted) {
        if (total < 0) {
            throw new BadRequestException("Total must be a non-negative integer in minor units.");
        }
        if (weighted == null || weighted.isEmpty()) {
            throw new BadRequestException("At least one participant is required.");
        }
        for (WeightedUser wu : weighted) {
            if (Double.isNaN(wu.weight()) || wu.weight() < 0) {
                throw new BadRequestException("Weights must be finite and non-negative.");
            }
        }

        double weightSum = weighted.stream().mapToDouble(WeightedUser::weight).sum();
        if (weightSum <= 0) {
            throw new BadRequestException("At least one weight must be greater than zero.");
        }

        record Entry(UUID userId, long floor, double remainder, int index) {}
        List<Entry> entries = new ArrayList<>();
        for (int i = 0; i < weighted.size(); i++) {
            double exact = (total * weighted.get(i).weight()) / weightSum;
            long floor = (long) Math.floor(exact);
            entries.add(new Entry(weighted.get(i).userId(), floor, exact - floor, i));
        }

        long flooredSum = entries.stream().mapToLong(Entry::floor).sum();
        long remaining = total - flooredSum;

        // Sort by fractional remainder descending, then index ascending for determinism
        List<Entry> sorted = entries.stream()
                .sorted(Comparator.comparingDouble(Entry::remainder).reversed()
                        .thenComparingInt(Entry::index))
                .toList();

        long[] finalAmounts = new long[entries.size()];
        for (Entry e : entries) {
            finalAmounts[e.index()] = e.floor();
        }
        for (int i = 0; i < remaining; i++) {
            finalAmounts[sorted.get(i % sorted.size()).index()] += 1;
        }

        List<AllocationResult> results = new ArrayList<>();
        for (int i = 0; i < entries.size(); i++) {
            results.add(new AllocationResult(weighted.get(i).userId(), finalAmounts[i]));
        }
        return results;
    }

    public List<AllocationResult> splitEqually(long total, List<UUID> userIds) {
        return allocateByWeights(total, userIds.stream().map(id -> new WeightedUser(id, 1.0)).toList());
    }

    public List<AllocationResult> splitByShares(long total, Map<UUID, Integer> shares) {
        return allocateByWeights(total, shares.entrySet().stream()
                .map(e -> new WeightedUser(e.getKey(), e.getValue().doubleValue()))
                .toList());
    }

    public List<AllocationResult> splitByPercentages(long total, Map<UUID, Double> percentages) {
        double sum = percentages.values().stream().mapToDouble(Double::doubleValue).sum();
        if (Math.abs(sum - 100.0) > 0.001) {
            throw new BadRequestException("Percentages must add up to 100.");
        }
        return allocateByWeights(total, percentages.entrySet().stream()
                .map(e -> new WeightedUser(e.getKey(), e.getValue()))
                .toList());
    }

    public void validateExpenseInvariant(long totalMinor, List<AllocationResult> payers, List<AllocationResult> shares) {
        long payerSum = payers.stream().mapToLong(AllocationResult::amountMinor).sum();
        long sharerSum = shares.stream().mapToLong(AllocationResult::amountMinor).sum();

        if (payerSum != totalMinor) {
            throw new BadRequestException(String.format("Payer sum (%d) does not match expense total (%d)", payerSum, totalMinor));
        }
        if (sharerSum != totalMinor) {
            throw new BadRequestException(String.format("Sharer sum (%d) does not match expense total (%d)", sharerSum, totalMinor));
        }
    }
}
