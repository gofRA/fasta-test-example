package com.exampe.fasta.concurrent;

import com.exampe.fasta.model.FastaResult;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.Callable;
import java.util.logging.Logger;

/**
 * Worker that performs operation of reading .fasta file
 * and writing into sequence.fasta.gz
 *
 * @author rdanilov
 * @since 21.10.2018
 */
public class FastaFileWorker implements Callable<FastaResult> {

    private static final Logger log = Logger.getLogger(FastaFileWorker.class.getName());

    private static final String descriptionPrefix = ">";
    private BufferedReader fileReader;
    private BufferedWriter fileWriter;
    private Map<Integer, Long> baseCounts;
    private long sequenceCount;
    private final FastaSynchronizer sync;


    /**
     * Constructs an instance of {@link FastaFileWorker} and adds it to the
     * {@link FastaSynchronizer} tasks list.
     *
     * @param sync synchronizer to write sequences in right order
     * @param fileReader buffered reader of .fasta file
     * @param fileWriter buffered writer of sequence.fasta.gz file
     */
    public FastaFileWorker(FastaSynchronizer sync, BufferedReader fileReader, BufferedWriter fileWriter) {
        this.fileReader = fileReader;
        this.fileWriter = fileWriter;
        this.baseCounts = new TreeMap<>();
        this.sequenceCount = 0;
        this.sync = sync;
        this.sync.add(this);
    }

    /**
     * Runs in separate trade, reads a .fasta file, processes the results,
     * and writes sequences to a sequence.fasta.gz file in right order.
     *
     * @return result of reading file
     */
    @Override
    public FastaResult call() {
        synchronized (sync) {
            fileReader.lines()
                    .filter(line -> !line.isEmpty() && !line.startsWith(descriptionPrefix))
                    .forEach(line -> {
                        try {
                            while (!sync.isMyTurn(this)) {
                                sync.wait();
                            }
                            if (sync.isRowChanged()) {
                                fileWriter.append("\n>")
                                        .append(String.valueOf(sync.getCurrentRowNum()))
                                        .append("\n");
                            }
                            fileWriter.append(line);
                            processLine(line);
                            sync.moveOrder();
                            sync.notifyAll();
                        } catch (InterruptedException | IOException e) {
                            log.severe("Something went wrong: " + e.getMessage());
                        }
                    });
            sync.remove(this);
        }
        return new FastaResult(baseCounts, sequenceCount);
    }

    private void processLine(String line) {
        sequenceCount++;
        line.chars().forEach(c -> baseCounts.compute(Character.toUpperCase(c), (key, val) -> val != null ? val + 1 : 1));
    }

}
