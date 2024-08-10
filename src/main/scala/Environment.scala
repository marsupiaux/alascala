package alascala

//\t to get listing of new directory
//implicit creation from "some/path" / -> Path("some/path")

class Path(home:String):
  private var wd = home.trim().stripSuffix("/")
  def pwd = wd
  infix def / = 
    Path(home.trim)
      match
        case Some(p) => p
        case None => this
  infix def /(f:String) = 
    Path(wd + "/" + f.trim().stripPrefix("/"))
      match
        case Some(p) => p
        case None => this
  infix def ^ = this
  infix def ^^ = 
    Path("/" + wd.split("/").drop(1).dropRight(1).mkString("/")) 
      match
        case Some(p) => p
        case None => this
  def str =
    import Console._
    s"$WHITE${wd}$BLUE/\\$RED$$$BLUE/$WHITE"
  override def toString() = pwd

object Path:
  def apply(d:String):Option[Path] = 
    import scala.sys.process.{ Process, stringToProcess }
    if s"ls $d".! == 0 then
      Some(new Path(d))
    else 
      println(s"$d/ not found by system ls call")
      None
  /*given Conversion[String, Path] = s => 
    Path(s) match
      case Some(p) => p
      case None => throw Exception("path not found by system ls call")
  given Conversion[Environment, Path] = e => e.d
  given Conversion[Environment, String] = e =>
    import Console._
    s"$WHITE${e.d}$BLUE/\\$RED$$$BLUE/$WHITE"*/



class Environment(home:Path):
  var d = home //keep navigation history? favouritesave functionality?

  infix def cd(f:String*):Path =
    d = f match
      case Nil =>
        d
      case _ =>
        import Path.given
        f.foldLeft(d){(a, f) => 
          a / f
        }
    d
  override def toString() = s"$d$$"

object Environment:
  def apply(s:String = "/"):Environment = Path(s) match
    case Some(p) => new Environment(p)
    case None => Environment()
  def ^(using Environment) = pwd
  def pwd(using sh:Environment) = sh.d
  infix def cd(f:String*)(using sh:Environment):Path = sh.cd(f*)
  given Conversion[String, Environment] = s => Environment(s)
