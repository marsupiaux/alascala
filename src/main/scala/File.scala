package alascala


trait PathSeg[+A<:PathSeg[A]]:
  val file:String
  val path:A
  lazy val ps = path.toString() + file + "/"
  override def toString() = ps
  def ++ ={} 

//use this class with path like movements to emulate drag&drop?
case class File(file:String, path:AbsolutePath) extends PathSeg[AbsolutePath]:
  override lazy val ps = path.toString() + file
  override def ++ = Environment.files = Environment.files + this
  def -- = Environment.files = Environment.files - this
object File:
  def apply(f:String):File =
    val p:AbsolutePath =
      if f.startsWith("/") then Root
      else summon[Environment].d
    val ls = f.trim.stripPrefix("/").split("/")
    val (d,_f) = ls.splitAt(ls.length - 1)
    File(_f(0), p./(d*))
  given Ordering[File] = Ordering.by(_.toString())
  extension (ss:Seq[Any])
    def apply(i:Int*):Seq[Any] = i.map(ss(_))
    def * = ss.foreach{_ match
      case a:AbsolutePath => a.++
      case f:File => f.++
      case _ =>
    }
  /*extension (ss:SortedSet[File])
    def display():Unit =
      import scala.collection.mutable.Set
      val xp:Set[Any] = Set()
      ss.foreach{s =>        
        println(s.toString(xp).stripSuffix("/"))
      }*/

