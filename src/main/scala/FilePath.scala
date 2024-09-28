package alascala

import alascala.Environment
import Environment.given

import scala.collection.immutable.SortedSet

type SomePath = FilePath[?]
type AbSeg = PathSeg[AbsolutePath]

sealed trait FilePath[+A<:FilePath[A]] extends PathSeg[A]:
  this:A =>
  val depth:Int = 0
  def apply(x:Int):A = 
    require(x >= 0)
    if x < depth then path(x) else this
  def seg[B>:A](s:String, g:B):A
  def / = Seq[AbSeg]()
  def /(f:String*):A =
    f.flatMap(_.split("/", -1))
      .foldLeft[A](this){(a, f) => f match
        case ".." => a.^^
        case "." => a
        case "" => ???
        case _ => seg(f, a)
      }.track()
  //def /(f:RelativePath):A = /:(f)
  def /:(o:RelativePath):A = o match
    case Dot => this
    case _ => /:(o.path)./(o.file)
  def /? = Seq[String]()
  def ^(using sh:Environment):Environment
  def ^^ = if file == ".." then seg("..", this) else path
  def * ={}

  def ls = 0 to 100 zip(/?)
  def track():A =
    val f = toString()
    Environment.track
      .find(_.toString().startsWith(f)) match
        case Some(p:A) => p(depth)
        case _ =>
          Environment.track
            .filter(p => f.startsWith(p.toString()))
            .foreach(p => Environment.track -= p)
          Environment.track += this
          this
  def toString2(xp:scala.collection.mutable.Set[Any]) =
    def str(x:A):String =
      val sx = x match
        case Root => "/"
        case Dot => ""
        case p => str(p.path) + p.file + "/"
      if xp.contains(x) then 
        sx.map(_ => ' ')
          .toString()
      else 
        xp += x
        sx
    str(this)

object FilePath:
  given Ordering[SomePath] = Ordering.by(_.toString)
  given Ordering[AbsolutePath] = Ordering.by(_.toString)
  given Conversion[AbSeg, java.io.File] = x => java.io.File(x.pretty)
  given string2Path:Conversion[String, SomePath] = _ match
    case "/" => Root
    case p if p.startsWith("/") => Root./(p.stripPrefix("/"))
    case r => Dot./(r)
  extension (ss:Seq[AbSeg])
    def apply(i:Int*):Seq[AbSeg] = i.map(ss(_))
    def * = ss.foreach{ _.++ }
  extension (ss:SortedSet[SomePath])
    def display():Unit =
      import scala.collection.mutable.Set
      val xp:Set[Any] = Set()
      ss.foreach{s =>        
        println(s.toString2(xp).stripSuffix("/"))
      }
      

sealed trait RelativePath extends FilePath[RelativePath]:
  override val path:RelativePath
  override def seg[B>:RelativePath](s:String, g:B):RelativePath = g match
    case r:RelativePath => Path2(s, r)
  def <=(sh:Environment):Environment =
    sh.cd(this)
    sh
  override def ^(using sh:Environment) = sh.cd(this) //(this /: sh.d).<=(sh)
  override def / = (this /: summon[Environment].d)./ //2do...won't consider derivatives until i pass a incompatible parameter (nothing ignored)
  def /:(sh:Environment):AbsolutePath = this /: sh.d

case object Dot extends RelativePath:
  override val file = "."
  override val path = Dot //???
  override lazy val ps = "\n"
  override def ^^ = Path2("..", Dot)

case class Path2(file:String, path:RelativePath) extends RelativePath:
  override val depth = path.depth + 1



sealed trait AbsolutePath extends FilePath[AbsolutePath]:
  override val path:AbsolutePath
  override def seg[B>:AbsolutePath](s:String, g:B):AbsolutePath = g match
    case a:AbsolutePath => Path(s, a)
  override def d = this
  def -- = Environment.paths = Environment.paths - this
  override def ++ = Environment.paths = Environment.paths + this
  override def / = /?.map{_ match
      case s if s.endsWith("/") => super./(s.stripSuffix("/")) //why super???
      case s => File(s, this)
    }
  override def /? =
    import scala.sys.process.{ Process, stringToProcess }
    s"ls ${Constant.LSOPS} '$pretty'".!!
      .split("\n")
      .filter(Environment.filter)
  override def * = /?.foreach{_ match
        case s if s.endsWith("/") => seg(s.stripSuffix("/"), this).++
        case s => File(s, this).++ // <= this includes symbolic links @
      }
    /*import AbsolutePath.display
    val ss:SortedSet[SomePath] = SortedSet() ++ Environment.paths ++ Environment.files
    ss.display()*/
  def <=(sh:Environment):Environment =
    sh.cd(this)
    sh
  override def ^(using sh:Environment) = <=(sh)
object AbsolutePath:
  given Conversion[SomePath, AbsolutePath] = _ match
    case a:AbsolutePath => a
    case r:RelativePath => r /: summon[Environment].d
  given Conversion[Environment, AbsolutePath] = _.d
  given Conversion[RelativePath, AbsolutePath] = _ /: summon[Environment].d
  extension (ss:Seq[AbsolutePath])
    def apply(i:Int*):Seq[AbsolutePath] = i.map(ss(_))
    def * = 
      ss.foreach(_.++)
      /*Environment.paths.display()
  extension (ss:scala.collection.mutable.Set[AbsolutePath])
    def display():Unit =
      import scala.collection.mutable.Set
      val xp:Set[Any] = Set()
      ss.foreach{s =>        
        println(s.toString2(xp))
      }*/


case object Root extends AbsolutePath:
  override val file = "/"
  override val path = Root
  override def toString() = "\n/"

case class Path(file:String, path:AbsolutePath) extends AbsolutePath with PathCmd:
  override val depth = path.depth + 1

//case class LinkPath(file:String, path:FilePath) extends Path(file, path) with FileCmd



//case class LinkFile(file:String, path:FilePath) extends File(file, path)


trait PathCmd

trait FileCmd //some commands are only avaible to files or paths but all are available to links?
