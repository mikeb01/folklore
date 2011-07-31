package performance;

import java.io.IOException;
import java.io.RandomAccessFile;

public class Main
{
    public static void main(String[] args) throws IOException
    {
        int runs = Integer.parseInt(System.getProperty("runs", "5"));
        int tests = Integer.parseInt(System.getProperty("tests", "15"));
        
        RandomAccessFile file = new RandomAccessFile("src/main/resources/pg19033.txt", "r");
        byte[] data = new byte[(int) file.length()];
        file.readFully(data);
        String dataAsString = new String(data, "UTF-8");
        char[] dataAsArray = dataAsString.toCharArray();
        
        PerformanceTest performanceTest0 = new strings.SequentialWordStatePerformanceTest(dataAsArray);
        PerformanceTest performanceTest1 = new strings.ParallelWordStatePerformanceTest(dataAsArray);
        PerformanceTest performanceTest3 = new strings.BruteForcePerformanceTest(dataAsString);
        
        PerfRunner runner = new PerfRunner(100, runs, runs);
        runner.run(tests, performanceTest0, performanceTest1, performanceTest3);
    }
}
