package alascala

import alascala.Environment
import Environment.given

import scala.collection.mutable.SortedSet

trait FilePath:
  def apply(x:Int):FilePath = if x < 1 then this else path(x - 1)
  val file:String
  val path:FilePath
  lazy val ps = path.toString() + file + "/"
  override def toString() = ps
  def toEnvironment(using sh:Environment) = (sh /: this).toString()
  //def /(o:FilePath):FilePath = /(o.toString())
  def /(f:String*):FilePath =
    f.flatMap(_.split("/", -1))
      .foldLeft(this){(a, f) => a./:(f) }
  //def +(o:Dot) = ???
  def ++(s:String):(SortedSet[AbsolutePath]|SortedSet[File]) =
    if s.endsWith("/") then
      Environment.paths += Path(s.stripSuffix("/"), this)
    else Environment.files += File(s, this)
  def ++(i:Int*):Unit =
    val ls = /?
    i.foreach{x => ++(ls(x))}
  def * = /?.foreach{++(_)}
  def /:(sh:Environment):FilePath = this./:(sh.d)
  def /:(o:FilePath):FilePath = path match
    case Root => Root
    case Dot => o./:(file)
    case _ => file /:(path./:(o))
  def /:(s:String):FilePath ={
    s.trim match //<-- doesn't allow for files names surrounded by spaces ... who does that anyway?
      case ".." => ^^
      case "." => this
      case "" => Root
      case o if o.contains("/") => /(o)
      case _ => Path(s, this)
    }.track()
  def ^^ = if file == ".." then Path("..", this) else path
  def /? =
    import scala.sys.process.{ Process, stringToProcess }
    s"ls ${Constant.LSOPS} '${toString().trim}'".!!
      .split("\n")
  def / = /?.filter(_.endsWith("/")).map{Path(_, this)}
  def ^ = /?.filter(! _.endsWith("/")).map{File(_, this)}

  def <=(sh:Environment):Environment =
    sh.d = sh /: this match
      case abp:AbsolutePath => abp
      case x => throw Exception(s">>>$x<<<-- Environment requires an AbsolutePath")
    sh
  def track():FilePath =
    val f = toString()
    Environment.track
      .find(_.toString().startsWith(f)) match
        case Some(_) => Environment.track
        case None =>
          Environment.track
            .filter(p => f.startsWith(p.toString()))
            .foreach(p => Environment.track -= p)
          Environment.track += this
    this
object FilePath:
  def apply(f:String*)(using sh:Environment):FilePath = sh.d./(f*)

  given Ordering[FilePath] = Ordering.by(_.toString)
  given Ordering[AbsolutePath] = Ordering.by(_.toString)
  given string2Path:Conversion[String, FilePath] = s => Dot./(s)

case object Dot extends FilePath:
  val file = "."
  val path = Dot
  override def toString() = "\n./"
  override def /(f:String*):FilePath = 
    super./(f*)
    summon[Environment].d./(f*)
  override def ^^ = Path("..", Dot)



trait AbsolutePath extends FilePath:
  def -- = Environment.paths -= this
  def ++ = Environment.paths += this
  override def <=(sh:Environment):Environment =
    sh.d = this
    sh

case object Root extends AbsolutePath:
  val file = "/"
  val path = Root
  override def toString() = "\n/"
  override def ^^ = this

case class Path(file:String, path:FilePath) extends AbsolutePath


//verifiedPath, absolutePath, relativePath, filePath ... others supported by the underlying filesystem
/*
class Path(home:String):
  val pwd = home.trim().stripSuffix("/") + "/"
  def /(f:String*) = Path(f*)(this)
  infix def ^^ = 
    Path("/" + pwd.stripPrefix("/").split("/").dropRight(1).mkString("/")) 
  def str =
    import Console._
    s"$WHITE${pwd.stripSuffix("/")}$BLUE/\\$RED$$$BLUE/$WHITE"
  override def toString() = s"\n$pwd"
  //override def equals(d2:Path) = d2.pwd == pwd
  def apply(i:Int*)(using Environment) = ++(i*)

object Path:
  def root = new Path("/")
  def apply(f:String*)(using sh:Environment):Path = f match
    case Nil => sh.d
    case h +: _ if h.startsWith("/") => Path.apply(f*)()
    case _ => Path.apply(f*)(sh.d)
  def apply(f:String*)(p:Path = Path.root):Path = //use Environment for relative Paths...or wait for check system to mature
    f.flatMap(_.split("/"))
      .foldLeft(p){(a, f) =>
        f match
          case "" => Path.root
          case "." => a
          case ".." => a.^^
          case _ =>             
            import scala.sys.process.{ Process, stringToProcess }
            val d = a.pwd + f.trim().stripPrefix("/")
            if s"test -d '${d}'".! == 0 then
              new Path(d)
            else
              import Console._
              println(s"$RED${d}/$WHITE not found by system$BLUE test -d$WHITE call")
              a <= Environment.crash
              throw Exception(s"<<<${f}>>> not found in ${a.pwd}")
      }
  given Ordering[Path] = Ordering.by(_.toString())
  given string2Path:Conversion[String, Path] = s => Path(s)
*/


//need to separate links for file manipulation & access (maybe as a directory)
case class File(file:String, path:FilePath) extends FilePath:
  def ++ = Environment.files += this
  def -- = Environment.files -= this

  override lazy val ps = path.toString() + file
  override def /(s:String*) = path./(s*)
  override def track() = 
    Environment.files += this
    this

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
