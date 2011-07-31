package strings

class MutableWordStatePerformanceTest(data: Array[Char]) extends performance.PerformanceTest {

  def getName(): String = {
    return "Parallel, Mutable Divide Conquer"
  }

  def run(iterations: Int) = {
    var o: Object = null;
    
    for (i <- 0 until iterations) {
        o = MutableWordState.wordsParallel(data)
    }
    
    if (null == o) {
      throw new RuntimeException();
    }
  }
}