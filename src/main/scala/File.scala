package alascala


trait PathSeg[+A<:PathSeg[A]]:
  def apply(i:Int) = this
  val file:String
  val path:A
  lazy val ps = path.toString() + file + "/"
  override def toString() = ps
  def pretty = toString().trim
  def exists:Boolean = ???
  def f:File = throw WTFRUDoing(s">>>${pretty}<<< is not a File")
  def d:AbsolutePath = throw WTFRUDoing(s">>>${pretty}<<< is not an AbsolutePath")
  def r:RelativePath = throw WTFRUDoing(s">>>${pretty}<<< is not an AbsolutePath")

//use this class with path like movements to emulate drag&drop?
case class File(file:String, path:AbsolutePath) extends PathSeg[AbsolutePath]:
  override lazy val ps = path.toString() + file
  override def f = this
  def >> = 
    val j:java.io.File = this
    import scala.sys.process._
    Process.cat(j)
  def write(s: String):Unit =
    val pw = new java.io.PrintWriter(new java.io.File(pretty))
    try pw.write(s) finally pw.close()
  override def exists =
    import scala.sys.process.{ Process, stringToProcess }
    s"test -f $pretty".! == 0
  def parse[A](f:SomePath => A) =
    import scala.io.Source
    import FilePath.string2Path
    if exists then
      Source.fromFile(pretty).getLines
        //.filter(_ != "")
        .map{f(_)
          //_.f
        }.toSeq
    else ???
object File:
  def apply(f:String):File =
    val p:AbsolutePath =
      if f.startsWith("/") then Root
      else summon[Environment].d
    val ls = f.trim.stripPrefix("/").split("/")
    val (d,_f) = ls.splitAt(ls.length - 1)
    File(_f(0), p./(d*))
  given Ordering[File] = Ordering.by(_.toString())
  /*extension (ss:SortedSet[File])
    def display():Unit =
      import scala.collection.mutable.Set
      val xp:Set[Any] = Set()
      ss.foreach{s =>        
        println(s.toString(xp).stripSuffix("/"))
      }*/

/*
  def using[A <: {def close(): Unit}, B](param: A)(f: A => B): B =
    try { f(param) } finally { param.close() }

  def writeToFile(fileName:String, data:String) = 
    using (new FileWriter(fileName)) {
      fileWriter => fileWriter.write(data)
    }

  def appendToFile(fileName:String, textData:String) =
    using (new FileWriter(fileName, true)){ 
      fileWriter => using (new PrintWriter(fileWriter)) {
        printWriter => printWriter.println(textData)
      }
    }*///convert to File.append(txt)
