package strings;

import performance.PerformanceTest;

public class BruteForcePerformanceTest implements PerformanceTest
{
    private final String data;

    public BruteForcePerformanceTest(String data)
    {
        this.data = data;
    }
    
    @Override
    public String getName()
    {
        return "Simple Single Threaded";
    }
    
    @Override
    public void run(int iterations)
    {
        Object o = null;
        for (int i = 0; i < iterations; i++)
        {
            o = BruteForce.words(data);
        }
        
        if (null == o)
        {
            throw new RuntimeException();
        }
    }
}
