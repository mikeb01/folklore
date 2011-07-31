package strings;

class FastList[S] extends collection.mutable.MutableList[S] {

  def ++(that: FastList[S]): FastList[S] = append(that)

  def append(that: FastList[S]): FastList[S] = {
    if (len == 0) that
    else {
      last0.next = that.first0
      last0 = that.last0;
      len = len + that.len
      this
    }
  }

}

object FastList {
  def empty[S]: FastList[S] = new FastList[S]

  def apply[S](s: S) = {
    val l = new FastList[S]
    l += s
  }
}