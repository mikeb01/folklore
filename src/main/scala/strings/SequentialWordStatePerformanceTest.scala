package strings;

class SequentialWordStatePerformanceTest(data: Array[Char]) extends performance.PerformanceTest {

  def getName(): String = {
    return "Sequential, Immutable Divide Conquer"
  }

  def run(iterations: Int) = {
    var o: Object = null;
    
    for (i <- 0 until iterations) {
        o = WordState.words(data)
    }
    
    if (null == o) {
      throw new RuntimeException();
    }
  }
}
