package alascala


class Path(home:String):
  private var wd = home.trim()
  def pwd = wd
  infix def /(f:String*):Path = 
    f.flatMap(_.split("/"))
      .foldLeft(this){(a, f) =>
        f match
          case "" => a
          case "." => a
          case ".." => a.^^
          case _ => Path(a.pwd.stripSuffix("/") + "/" + f.trim().stripPrefix("/")) match
            case Some(p) => p
            case None => throw Exception(s"/$f/ not found in ${a.pwd}/")
      }
  infix def ^ = this
  infix def ^^ = 
    Path("/" + wd.split("/").drop(1).dropRight(1).mkString("/")) 
      match
        case Some(p) => p
        case None => this
  def str =
    import Console._
    s"$WHITE${pwd}$BLUE/\\$RED$$$BLUE/$WHITE"
  override def toString() = pwd

object Path:
  def apply(d:String):Option[Path] = 
    import scala.sys.process.{ Process, stringToProcess }
    if s"test -d $d".! == 0 then
      Some(new Path(d))
    else 
      import Console._
      println(s"$RED${d}/$WHITE not found by system$BLUE test -d$WHITE call")
      None



class Environment(home:Path):
  var d = home //keep navigation history? favouritesave functionality?

  infix def cd(f:String*):Environment = 
    d = d./(f*)
    this
  def root = 
    d = home
    this
  def /(f:String*) = cd(f*)
  def ^^ =
    d = d.^^
    this
  def <=(sh:Environment) = sh.d = d
  override def toString() = s"$d$$"
  def ls(ops:String = "-ph --color=auto --indicator=classify --file-type") =
    import scala.sys.process.{ Process, stringToProcess }
    s"ls $ops ${d.pwd}"
      .!!
      .split("\n")

object Environment:
  import scala.sys.process.{ Process, stringToProcess }
  val home:Environment = "pwd".!!
  val root = Environment()
  def apply(s:String = "/"):Environment = Path(s) match
    case Some(p) => new Environment(p)
    case None => Environment()
  def ^(using sh:Environment) = sh
  def ^^(using sh:Environment) = sh.^^
  def ^^^ = home.root
  def / = root.root
  def /^ = root
  def pwd(using sh:Environment) = sh.d.pwd
  def cd(f:String*)(using sh:Environment):Environment = sh.cd(f*)
  def ls(using sh:Environment) = sh.ls("-ph --color=auto --indicator=classify --file-type")
  given Conversion[String, Environment] = s => Environment(s)
  given ENV:Environment = home
