package com.exampe.fasta.service;

import com.exampe.fasta.concurrent.FastaFileWorker;
import com.exampe.fasta.concurrent.FastaSynchronizer;
import com.exampe.fasta.model.FastaResult;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Service processing *.fasta files.
 *
 * @author rdanilov
 * @since 21.10.2018
 */
public class FastaFileService {

    private static final Logger log = Logger.getLogger(FastaFileService.class.getName());

    public static final String reportFileName = "report.txt";
    public static final String sequenceFileName = "sequence.fasta.gz";

    private final String pathToFolder;
    private FastaSynchronizer sync = new FastaSynchronizer();


    /**
     * Constructs an instance with default path to files folder.
     * Current folder will be used
     */
    public FastaFileService() {
        this.pathToFolder = "";
    }

    /**
     * Constructs an instance with given path to files folder
     *
     * @param pathToFolder path used to read files and create reports
     */
    public FastaFileService(String pathToFolder) {
        this.pathToFolder = pathToFolder;
    }


    /**
     * Processes .fasta files with given names of files.
     * Method creates instances of {@link FastaFileWorker}. Each .fasta file corresponds to one
     * {@link FastaFileWorker} that runs in a separate thread in {@link java.util.concurrent.ForkJoinPool}.
     * Results of computation are collected and are written as reports.
     *
     * @param fileNames names of files to be read
     */
    public void processFastaFiles(List<String> fileNames) {

        try (BufferedWriter sequenceWriter = getSequenceFileWriter()) {

            if (sequenceWriter == null) {
                throw new IllegalStateException("Unable to create " + sequenceFileName);
            }

            ExecutorService service = Executors.newWorkStealingPool();
            List<FastaFileWorker> tasks = fileNames.stream()
                    .map(fileName -> pathToFolder + fileName)
                    .map(Paths::get)
                    .filter(Files::exists)
                    .map(Path::toFile)
                    .map(file -> getFastaFileWorker(file, sequenceWriter))
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            if (tasks.size() < fileNames.size()) {
                throw new IllegalStateException("Unable to read some files");
            }

            writeFirstDescription(sequenceWriter);

            FastaResult result = service.invokeAll(tasks).stream()
                    .map(this::getResult)
                    .filter(Objects::nonNull)
                    .reduce(FastaResult::merge)
                    .orElseThrow(() -> new IllegalStateException("Unable to process some files"));

            service.shutdown();

            writeReportFile(result, fileNames.size());
        } catch (IOException | InterruptedException e) {
            log.severe("Something went wrong: " + e.getMessage());
        }
    }

    private FastaFileWorker getFastaFileWorker(File file, BufferedWriter sequenceWriter) {
        try {
            GZIPInputStream gzipStream = new GZIPInputStream(new FileInputStream(file));
            BufferedReader reader = new BufferedReader(new InputStreamReader(gzipStream));
            return new FastaFileWorker(sync, reader, sequenceWriter);
        } catch (IOException e) {
            log.severe("Something went wrong: " + e.getMessage());
            return null;
        }
    }

    private FastaResult getResult(Future<FastaResult> result) {
        try {
            return result.get();
        } catch (InterruptedException | ExecutionException e) {
            log.severe("Something went wrong: " + e.getMessage());
            return null;
        }
    }

    private BufferedWriter getSequenceFileWriter() {
        Path sequencePath = Paths.get(pathToFolder + sequenceFileName);
        try {
            Files.deleteIfExists(sequencePath);
            File file = sequencePath.toFile();
            GZIPOutputStream outputStream = new GZIPOutputStream(new FileOutputStream(file));
            return new BufferedWriter(new OutputStreamWriter(outputStream));
        } catch (IOException e) {
            log.severe("Something went wrong: " + e.getMessage());
            return null;
        }
    }

    private BufferedWriter getReportFileWriter() {
        Path reportPath = Paths.get(pathToFolder + reportFileName);
        try {
            Files.deleteIfExists(reportPath);
            File file = reportPath.toFile();
            return new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file)));
        } catch (IOException e) {
            log.severe("Something went wrong: " + e.getMessage());
            return null;
        }
    }

    private void writeReportFile(FastaResult result, int filesCount) throws IOException {
        try (BufferedWriter writer = getReportFileWriter()) {
            if (writer == null) {
                throw new IllegalStateException("Unable to write into " + reportFileName);
            }
            long baseCounts = result.getBaseCounts().values().stream().reduce(0L, Long::sum);
            writer.append("FILE_CNT\t").append(String.valueOf(filesCount)).append("\n");
            writer.append("SEQUENCE_CNT\t").append(String.valueOf(result.getSequenceCount())).append("\n");
            writer.append("BASE_CNT\t").append(String.valueOf(baseCounts)).append("\n");
            for (Map.Entry<Integer, Long> entry : result.getBaseCounts().entrySet()) {
                writer.append((char) entry.getKey().intValue())
                        .append("\t")
                        .append(String.valueOf(entry.getValue()))
                        .append("\n");
            }
        }
    }

    private void writeFirstDescription(BufferedWriter sequenceWriter) throws IOException {
        sequenceWriter.append(">1\n");
    }
}
