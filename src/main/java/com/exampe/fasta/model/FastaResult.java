package com.exampe.fasta.model;

import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Provides result of .fasta file computation.
 *
 * @author rdanilov
 * @since 21.10.2018
 */
public class FastaResult {

    private Map<Integer, Long> baseCounts;
    private long sequenceCount;

    /**
     * Constructs an instance of {@link FastaResult}
     * @param baseCounts number of each base separately
     * @param sequenceCount number of sequences
     */
    public FastaResult(Map<Integer, Long> baseCounts, long sequenceCount) {
        this.baseCounts = baseCounts;
        this.sequenceCount = sequenceCount;
    }

    /**
     * @return number of each base separately
     */
    public Map<Integer, Long> getBaseCounts() {
        return baseCounts;
    }

    /**
     * @return number of sequences
     */
    public long getSequenceCount() {
        return sequenceCount;
    }


    /**
     * Merges two {@link FastaResult} into the first one.
     *
     * @param result1 the first result which will be merged in
     * @param result2 the second result to be merged
     * @return result of merged results as result1
     */
    public static FastaResult merge(FastaResult result1, FastaResult result2) {
        Map<Integer, Long> mergedMap = Stream.of(result1.getBaseCounts(), result2.getBaseCounts())
                .flatMap(m -> m.entrySet().stream())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, Long::sum));
        result1.baseCounts = new TreeMap<>(mergedMap);
        result1.sequenceCount = result1.getSequenceCount() + result2.getSequenceCount();
        return result1;
    }
}
