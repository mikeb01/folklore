package strings

class ParallelWordStatePerformanceTest(data: Array[Char]) extends performance.PerformanceTest {

  def getName(): String = {
    return "Parallel, Immutable Divide Conquer"
  }

  def run(iterations: Int) = {
    var o: Object = null;
    
    for (i <- 0 until iterations) {
        o = WordState.wordsParallel(data)
    }
    
    if (null == o) {
      throw new RuntimeException();
    }
  }
}