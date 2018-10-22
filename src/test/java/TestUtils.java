import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.zip.GZIPInputStream;

import static org.junit.Assert.assertEquals;

/**
 * Test utils to help to compare contents of files in tests
 *
 * @author rdanilov
 * @since 21.10.2018
 */
final class TestUtils {

    private TestUtils(){}

    static boolean isContentEquals(File expected, File actual, boolean isGzip) throws IOException {
        if (isGzip) {
            try (BufferedReader readerExp = new BufferedReader(new InputStreamReader(new GZIPInputStream(new FileInputStream(expected))));
                 BufferedReader readerAct = new BufferedReader(new InputStreamReader(new GZIPInputStream(new FileInputStream(actual))))) {
                compareContent(readerExp, readerAct);
            }
        } else {
            try (BufferedReader readerExp = new BufferedReader(new InputStreamReader(new FileInputStream(expected)));
                 BufferedReader readerAct = new BufferedReader(new InputStreamReader(new FileInputStream(actual)))) {
                compareContent(readerExp, readerAct);
            }
        }
        return true;
    }

    private static void compareContent(BufferedReader readerExp, BufferedReader readerAct) throws IOException {
        String tmp;
        while ((tmp = readerExp.readLine()) != null) {
            assertEquals("Content must be the same", tmp, readerAct.readLine());
        }
    }
}
