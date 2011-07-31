package performance;

import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;

public class PerfRunner
{
    private final int iterations;
    private final int stableRuns;
    private final int warmUpRuns;

    public PerfRunner(int interations, int warmUpRuns, int stableRuns)
    {
        this.iterations = interations;
        this.warmUpRuns = warmUpRuns;
        this.stableRuns = stableRuns;
    }
    
    public void run(int toRun, PerformanceTest... tests)
    {
        for (int j = 0, n = tests.length; j < n; j++)
        {
            if (((1 << j) & toRun) == 0)
            {
                continue;
            }
            
            PerformanceTest test = tests[j];
            System.out.printf("%s%nWarmup ", test.getName());

            for (int i = 0; i < warmUpRuns; i++)
            {
                test.run(iterations);
                System.out.print('.');
            }
            
            System.out.println();
            
            System.out.printf("Running ", test.getName());
            long[] times = new long[stableRuns];
            
            for (int i = 0; i < stableRuns; i++)
            {
                long t0 = System.currentTimeMillis();
                test.run(iterations);
                long t1 = System.currentTimeMillis();
                
                times[i] = (t1 - t0);
                System.out.print('.');
            }
            System.out.println();
            
            DescriptiveStatistics stableStats = new DescriptiveStatistics(stableRuns);
            for (long time : times)
            {
                stableStats.addValue(time);
            }
            double average = stableStats.getMean();
            double ops = iterations * 1000 / average;
            
            System.out.printf("Operations: %.3f per second%n", ops);
        }
    }
}
