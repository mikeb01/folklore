package performance;

public interface PerformanceTest
{
    String getName();
    void run(int iterations);
}
