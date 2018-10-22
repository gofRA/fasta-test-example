import com.example.fasta.Main;
import com.example.fasta.model.FastaResult;
import com.example.fasta.service.FastaFileService;
import org.hamcrest.core.StringStartsWith;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static com.example.fasta.service.FastaFileService.reportFileName;
import static com.example.fasta.service.FastaFileService.sequenceFileName;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * @author rdanilov
 * @since 21.10.2018
 */
public class FastaTest {

    private static final String pathToResources = "src/test/resources/";
    private static final String expectedReportName = "expectedRp.txt";
    private static final String actualReportName = "report.txt";
    private static final String expectedGzName = "expectedGz.gz";
    private static final String actualGzName = "sequence.fasta.gz";

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void testIntegration() throws Exception {
        List<String> fileNames = Arrays.asList(
                "test1.fasta.gz",
                "test2.fasta.gz",
                "test3.fasta.gz",
                "test4.fasta.gz"
        );
        FastaFileService service = new FastaFileService(pathToResources);
        service.processFastaFiles(fileNames);
        File expectedReport = new File(getClass().getResource(expectedReportName).getFile());
        File expectedGz = new File(getClass().getResource(expectedGzName).getFile());

        File actualReport = new File(pathToResources + actualReportName);
        assertNotNull(actualReport);
        assertTrue(actualReport.exists());

        File actualGz = new File(pathToResources + actualGzName);
        assertNotNull(actualGz);
        assertTrue(actualGz.exists());

        assertTrue(TestUtils.isContentEquals(expectedReport, actualReport, false));
        assertTrue(TestUtils.isContentEquals(expectedGz, actualGz, true));
    }

    @Test
    public void testEmptyArguments() {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("You must specify at least one argument");
        Main.main(new String[]{});
    }

    @Test
    public void testWrongFileName() {
        thrown.expect(IllegalStateException.class);
        thrown.expectMessage("Unable to read some files");
        FastaFileService service = new FastaFileService(pathToResources);
        service.processFastaFiles(Collections.singletonList("wrongFileName"));
    }

    @Test
    public void testSequenceWriterIsIllegalState() throws IOException {
        thrown.expect(IllegalStateException.class);
        thrown.expectMessage(StringStartsWith.startsWith("Unable to create"));
        File file = new File(pathToResources + sequenceFileName);
        if (!file.exists()) {
            assertTrue(file.createNewFile());
        }
        try (InputStream ignored = new FileInputStream(file)) {
            FastaFileService service = new FastaFileService(pathToResources);
            service.processFastaFiles(Collections.singletonList("test1.fasta.gz"));
        }
    }

    @Test
    public void testReportWriterIsIllegalState() throws IOException {
        thrown.expect(IllegalStateException.class);
        thrown.expectMessage(StringStartsWith.startsWith("Unable to write into"));
        File file = new File(pathToResources + reportFileName);
        if (!file.exists()) {
            assertTrue(file.createNewFile());
        }
        try (InputStream ignored = new FileInputStream(file)) {
            FastaFileService service = new FastaFileService(pathToResources);
            service.processFastaFiles(Collections.singletonList("test1.fasta.gz"));
        }
    }

    @Test
    public void testMergeResult() {
        Map<Integer, Long> map1 = new TreeMap<>();
        map1.put(65, 2L);
        map1.put(66, 4L);
        FastaResult fasta1 = new FastaResult(map1, 10);

        Map<Integer, Long> map2 = new TreeMap<>();
        map2.put(66, 6L);
        map2.put(67, 3L);
        map2.put(68, 5L);
        FastaResult fasta2 = new FastaResult(map2, 15);

        FastaResult.merge(fasta1, fasta2);

        Map<Integer, Long> expectedMap = new TreeMap<>();
        expectedMap.put(65, 2L);
        expectedMap.put(66, 10L);
        expectedMap.put(67, 3L);
        expectedMap.put(68, 5L);

        assertEquals(4, fasta1.getBaseCounts().size());
        assertEquals(25, fasta1.getSequenceCount());
        assertEquals(expectedMap, fasta1.getBaseCounts());
    }

}
