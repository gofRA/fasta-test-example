package com.example.fasta;

import com.example.fasta.service.FastaFileService;

import java.util.Arrays;

/**
 * @author rdanilov
 * @since 21.10.2018
 */
public class Main {

    public static void main(String[] args) {
        if (args.length > 0) {
            FastaFileService service = new FastaFileService();
            service.processFastaFiles(Arrays.asList(args));
        } else {
            throw new IllegalArgumentException("You must specify at least one argument");
        }
    }
}
