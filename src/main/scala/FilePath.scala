package alascala

import alascala.Environment
import Environment.given

import scala.collection.mutable.SortedSet

type SomePath = AbsolutePath|RelativePath 
//FilePath[?]

sealed trait FilePath[+A<:FilePath[A]]:
  def this2A():A = this match 
    case a:A => a
  def somePath():SomePath = this match
    case a:AbsolutePath => a
    case r:RelativePath => r
    case _ => ???
  def apply(x:Int):A = 
    if x < 1 then this2A() else path(x - 1)
  val file:String
  val path:A
  lazy val ps = path.toString() + file + "/"
  override def toString() = ps
  def toEnvironment(using sh:Environment):String  
  def seg[B>:A](s:String, g:B):B = ???
  def /(f:String*):A  
  def ++(i:Int*):Unit = ???
  def /:(o:RelativePath):A  
  def /:(o:SomePath):A = o match
    case r:RelativePath => /:(r)
    case _ => throw Exception(s">>>${o.toString().trim}<<< can only add RelativePath")
  //def ^^():A = path

  def <=(sh:Environment):Environment = 
    this match
      case a:AbsolutePath => sh.cd(a)
      case r:RelativePath => sh.cd(r)
      case _ => ???
    sh
  def track():A =
    val f = toString()
    Environment.track
      .find(_.toString().startsWith(f)) match
        case Some(_) => Environment.track
        case None =>
          Environment.track
            .filter(p => f.startsWith(p.toString()))
            .foreach(p => Environment.track -= p)
          Environment.track += somePath()
    this2A()
object FilePath:
  def apply(f:String*)(using sh:Environment):AbsolutePath = sh.d./(f*)

  given Ordering[SomePath] = Ordering.by(_.toString)
  given Ordering[AbsolutePath] = Ordering.by(_.toString)
  given some2AbsolutePath:Conversion[SomePath, AbsolutePath] = _ match
    case a:AbsolutePath => a
    case r:RelativePath => summon[Environment] /: r
    //case f:File => ???
  given string2Path:Conversion[String, SomePath] = _ match
    case s if s == "/" => Root
    case p if p.startsWith("/") => Root./(p.stripPrefix("/"))
    case r => Dot./(r)
  extension (ss:SortedSet[SomePath])
    def display():Unit =
      import scala.collection.mutable.Set
      val xp:Set[SomePath] = Set()
      ss.foreach{sp =>
        def string(x:SomePath):String =
          val sx = x match
            case Root => "/"
            case Dot => "./"
            case p => string(p.path) + p.file + "/"
          if xp.contains(x) then 
            sx.map(_ => ' ')
              .toString()
          else 
            xp += x
            sx
        println(string(sp))
      }
      

trait RelativePath extends FilePath[RelativePath]:
  override val path:RelativePath
  def toEnvironment(using sh:Environment):String = (this /: sh.d).toString()
  //override def seg(s:String, g:RelativePath):RelativePath = Path2(s, g)
  def ^^ = if file == ".." then Path2("..", this) else path
  override def /(f:String*):RelativePath =
    f.flatMap(_.split("/", -1))
      .foldLeft[RelativePath](this){(a, f) => f match
        case ".." => a.^^
        case "." => a
        case "" => ???
        case _ => Path2(f, a)
      }.track()
  def /:(sh:Environment):AbsolutePath = this /: sh.d
  def /:(o:RelativePath):RelativePath = o match
    case Dot => this2A()
    case _ => /:(o.path)./(o.file)

case object Dot extends RelativePath:
  val file = ""
  val path = Dot
  override def toString() = "\n./"
  override def ^^ = Path2("..", Dot)

case class Path2(file:String, path:RelativePath) extends RelativePath



trait AbsolutePath extends FilePath[AbsolutePath]:
  override val path:AbsolutePath
  def toEnvironment(using sh:Environment):String = toString()
  //override def seg(s:String, g:AbsolutePath):AbsolutePath = Path(s, g)
  def -- = Environment.paths -= this
  def ++ = Environment.paths += this
  override def ++(i:Int*):Unit =
    val ls = /?
    i.foreach{x => 
      ls(x) match
        case s if s.endsWith("/") => Path(s.stripSuffix("/"), this).++
        case f => File(f, this).++
    }
  def /? =
    import scala.sys.process.{ Process, stringToProcess }
    s"ls ${Constant.LSOPS} '${toString().trim}'".!!
      .split("\n")
      .filter(Environment.filter)
  def * = /?.foreach{_ match
    case s if s.endsWith("/") => /(s.stripSuffix("/")).++
    case s => File(s, this).++
  }
  def ls = 0 to 100 zip(/?)
  override def <=(sh:Environment):Environment =
    sh.d = this
    sh
  def ^(using sh:Environment) = <=(sh)
  def ^^ = path
  override def /(f:String*):AbsolutePath =
    f.flatMap(_.split("/", -1))
      .foldLeft[AbsolutePath](this){(a, f) => f match
        case ".." => a.^^
        case "." => a
        case "" => ??? //Root
        case _ => Path(f, a)
      }.track()
  def /:(o:RelativePath):AbsolutePath = o match
    case Dot => this2A()
    case _ => /:(o.path)./(o.file)

case object Root extends AbsolutePath:
  val file = "/"
  val path = Root
  override def toString() = "\n/"

case class Path(file:String, path:AbsolutePath) extends AbsolutePath with PathCmd

//case class LinkPath(file:String, path:FilePath) extends Path(file, path) with FileCmd



//case class LinkFile(file:String, path:FilePath) extends File(file, path)

case class File(file:String, path:AbsolutePath) extends FilePath[AbsolutePath] with FileCmd:
  override lazy val ps = path.toString() + file
  def toEnvironment(using sh:Environment):String = toString()
  def ++ = Environment.files += this
  def -- = Environment.files -= this

  override def /:(o:RelativePath):AbsolutePath = ???
  override def /(s:String*) = ???
    /*val p = path./(s.flatMap(_.split("/"))*)
    File(p.file, p.path)*///<-- ???
  override def track() = 
    Environment.files += this
    this2A()

object File:
  def apply(f:String, p:Path):File =
    val file = s"${p}${f.trim.stripPrefix("/")}"
    import scala.sys.process.{ Process, stringToProcess }
    if s"test -f '$file'".! == 0 then
      val ls = f.trim.stripPrefix("/").split("/")
      val (d,_f) = ls.splitAt(ls.length - 1)
      new File(_f(0), p./(d*))
    else
      import Console._
      println(s"$RED${file}$WHITE not found by system$BLUE test -f$WHITE call")
      throw Exception(s"<<<${f}>>> not found in ${p}")
  given Ordering[File] = Ordering.by(_.toString())



trait PathCmd

trait FileCmd //some commands are only avaible to files or paths but all are available to links?
