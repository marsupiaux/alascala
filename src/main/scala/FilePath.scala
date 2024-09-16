package alascala

import alascala.Environment
import Environment.given

import scala.collection.mutable.SortedSet

//type SomePath = AbsolutePath|RelativePath 
//type AnyPath <: FilePath[SomePath]
type SomePath = FilePath[?]

sealed trait FilePath[A<:FilePath[A]]:
  def this2A():A = this match 
    case a:A => a
    case _ => ???
  def somePath():SomePath = this match
    case a:AbsolutePath => a
    case r:RelativePath => r
    case _ => ???
  def apply(x:Int):A = 
    require(x >= 0)
    if x < depth then path(x) else this2A()
  val file:String
  val path:A
  val depth:Int
  lazy val ps = path.toString() + file + "/"
  override def toString() = ps
  def toEnvironment(using sh:Environment):String  
  def seg(s:String, g:A):A
  def /(i:Int):SomePath
  def /(f:String*):A =
    f.flatMap(_.split("/", -1))
      .foldLeft[A](this2A()){(a, f) => f match
        case ".." => a.^^
        case "." => a
        case "" => ???
        case _ => seg(f, a)
      }.track()
  def /(f:RelativePath):A = /:(f)
  def /:(o:RelativePath):A  
  /*def /:(o:SomePath):A = o match
    case r:RelativePath => /:(r)
    case _ => throw WTFRUDoing(s">>>${o.toString().trim}<<< can only add RelativePath")*/
  def /? = Seq[String]()
  def * ={}
  def ^(using sh:Environment):Environment
  def ^^ = if file == ".." then seg("..", this2A()) else path
  def ++ ={} 

  def ls = 0 to 100 zip(/?)
  def track():A =
    val f = toString()
    Environment.track
      .find(_.toString().startsWith(f)) match
        case Some(p) => p(depth)
        case None =>
          Environment.track
            .filter(p => f.startsWith(p.toString()))
            .foreach(p => Environment.track -= p)
          Environment.track += somePath()
    this2A()
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
    str(this2A())

object FilePath:
  def apply(f:String*)(using sh:Environment):AbsolutePath = sh.d./(f*)

  given Ordering[SomePath] = Ordering.by(_.toString)
  given Ordering[AbsolutePath] = Ordering.by(_.toString)
  given Conversion[SomePath, java.io.File] = x => java.io.File(x.toString().trim)
  given Conversion[Environment, AbsolutePath] = _.d
  given Conversion[RelativePath, AbsolutePath] = _ /: summon[Environment].d
  given Conversion[SomePath, AbsolutePath] = _ match
    case a:AbsolutePath => a
    case r:RelativePath => summon[Environment] /: r
    case f:File => ???
  given string2Path:Conversion[String, SomePath] = _ match
    case "/" => Root
    case p if p.startsWith("/") => Root./(p.stripPrefix("/"))
    case r => Dot./(r)
  extension (ss:Seq[SomePath])
    def apply(i:Int*):Seq[SomePath] = i.map(ss(_))
    def * = 
      ss.foreach(_.++)
      val s:SortedSet[SomePath] = SortedSet() ++ ss
      s.display()
  extension (ss:SortedSet[SomePath])
    def display():Unit =
      import scala.collection.mutable.Set
      val xp:Set[Any] = Set()
      ss.foreach{s =>        
        println(s.toString2(xp).stripSuffix("/"))
      }
      

sealed trait RelativePath extends FilePath[RelativePath]:
  override val path:RelativePath
  def toAbsolute = this /: summon[Environment].d
  def toEnvironment(using sh:Environment):String = toAbsolute.toString()
  override def seg(s:String, g:RelativePath):RelativePath = Path2(s, g)
  override def /(i:Int):SomePath = toAbsolute./(i)
  override def /? = toAbsolute./?
  override def * = toAbsolute.*
  override def ++ = toAbsolute.++
  def <=(sh:Environment):Environment =
    sh.cd(this)
    sh
  override def ^(using sh:Environment) = sh.cd(this) //(this /: sh.d).<=(sh)
  def /:(sh:Environment):AbsolutePath = this /: sh.d
  override def /:(o:RelativePath):RelativePath = o match
    case Dot => this2A()
    case _ => /:(o.path)./(o.file)

case object Dot extends RelativePath:
  val file = "."
  val path = Dot
  val depth = 0
  override def toString() = "\n./"
  override def ^^ = Path2("..", Dot)

case class Path2(file:String, path:RelativePath) extends RelativePath:
  val depth = path.depth + 1



sealed trait AbsolutePath extends FilePath[AbsolutePath]:
  override val path:AbsolutePath
  def toEnvironment(using sh:Environment):String = toString()
  override def seg(s:String, g:AbsolutePath):AbsolutePath = Path(s, g)
  def -- = Environment.paths -= this
  override def ++ = Environment.paths += this
  override def /(i:Int):SomePath = /?(i) match
    case s if s.endsWith("/") => /(s.stripSuffix("/"))
    case s => File(s, this)
  override def /? =
    import scala.sys.process.{ Process, stringToProcess }
    s"ls ${Constant.LSOPS} '${toString().trim}'".!!
      .split("\n")
      .filter(Environment.filter)
  override def * = /?.foreach{_ match
        case s if s.endsWith("/") => /(s.stripSuffix("/")).++
        case s => File(s, this).++ // <= this includes symbolic links @
      }
    /*import AbsolutePath.display
    val ss:SortedSet[SomePath] = SortedSet() ++ Environment.paths ++ Environment.files
    ss.display()*/
  def <=(sh:Environment):Environment =
    sh.cd(this)
    sh
  override def ^(using sh:Environment) = <=(sh)
  override def /:(o:RelativePath):AbsolutePath = o match
    case Dot => this2A()
    case _ => /:(o.path)./(o.file)
object AbsolutePath:
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
  val file = "/"
  val path = Root
  val depth = 0
  override def toString() = "\n/"
  extension (ss:Seq[AbsolutePath])
    def apply(i:Int*):Seq[AbsolutePath] = i.map(ss(_))
    def * = 
      ss.foreach(_.++)
      //Environment.paths.display()

case class Path(file:String, path:AbsolutePath) extends AbsolutePath with PathCmd:
  val depth = path.depth + 1

//case class LinkPath(file:String, path:FilePath) extends Path(file, path) with FileCmd



//case class LinkFile(file:String, path:FilePath) extends File(file, path)

//use this class with path like movements to emulate drag&drop?
case class File(file:String, path:AbsolutePath) extends FilePath[AbsolutePath] with FileCmd:
  val depth = path.depth + 1
  override lazy val ps = path.toString() + file
  def toEnvironment(using sh:Environment):String = toString()
  override def seg(s:String, f:AbsolutePath):AbsolutePath = File(s, f)
  override def ++ = Environment.files += this
  def -- = Environment.files -= this

  override def ^(using Environment) = ???
  override def /:(o:RelativePath):AbsolutePath = ???
  override def /(i:Int) = ???
  override def /(s:String*) = ???
  override def track() = 
    Environment.files += this
    this2A()

object File:
  def apply(f:String):File =
    val p:AbsolutePath =
      if f.startsWith("/") then Root
      else summon[Environment].d
    val ls = f.trim.stripPrefix("/").split("/")
    val (d,_f) = ls.splitAt(ls.length - 1)
    File(_f(0), p./(d*))
  given Ordering[File] = Ordering.by(_.toString())
  /*extension (ss:Seq[File])
    def apply(i:Int*):Seq[File] = i.map(ss(_))
    def * = 
      ss.foreach(_.++)
      Environment.files.display()
  extension (ss:SortedSet[File])
    def display():Unit =
      import scala.collection.mutable.Set
      val xp:Set[Any] = Set()
      ss.foreach{s =>        
        println(s.toString(xp).stripSuffix("/"))
      }*/



trait PathCmd

trait FileCmd //some commands are only avaible to files or paths but all are available to links?
