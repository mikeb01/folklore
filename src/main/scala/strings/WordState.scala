package strings

object WordState {
    
    def maybeWord(s:String) = if (s.isEmpty) FastList.empty[String] else FastList(s)
    
    def processChar(c:Char): WordState = if (c != ' ') Chunk("" + c) else Segment.empty
    
    def processChar2(a: WordState, c:Char): WordState = if (c != ' ') a.assoc(c) else a.assoc(Segment.empty);
    
    def compose(a: WordState, b: WordState) = a.assoc(b)
    
    def wordsParallel(s:Array[Char]): FastList[String] = { 
        s.par.aggregate(Chunk.empty)(processChar2, compose).toList()
    }
    
    def words(s:Array[Char]) : FastList[String] = { 
        val wordStates = s.map(processChar).toArray
        wordStates.foldRight(Chunk.empty)((x, y) => x.assoc(y)).toList()
    }
}

trait WordState {
    def assoc(other: WordState): WordState
    def assoc(other: Char): WordState
    def toList(): FastList[String]
}

case class Chunk(part: String) extends WordState {
    override def assoc(other: WordState) = {
        other match {
            case c:Chunk => Chunk(part + c.part)
            case s:Segment => Segment(part + s.prefix, s.words, s.trailer)
        }
    }
    
    override def assoc(other: Char) = Chunk(part + other)
    
    override def toList() = WordState.maybeWord(part)
}

object Chunk {
    val empty:WordState = Chunk("")
}

case class Segment(prefix: String, words: FastList[String], trailer: String) extends WordState {
    override def assoc(other: WordState) = {

        other match {
            case c:Chunk => Segment(prefix, words, trailer + c.part)
            case s:Segment => Segment(prefix, words ++ WordState.maybeWord(trailer + s.prefix) ++ s.words, s.trailer)
        }
    }
    
    override def assoc(other: Char) = Segment(prefix, words, trailer + other)
    override def toList() = WordState.maybeWord(prefix) ++ words ++ WordState.maybeWord(trailer)
}

object Segment {
    val empty:WordState = Segment("", FastList.empty[String], "")
}