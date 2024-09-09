package alascala



class Environment(home:AbsolutePath):
  var d = home //keep navigation history? favouritesave functionality?

  def root = 
    d = home
    this
  def cd(s:String) = d = d./(s)
  def cd(p:AbsolutePath) = d = p
  def cd(p:RelativePath) = d = p /: d
  /*def / = alascala.Alias.ls(using this).filter(_.endsWith("/"))
    .flatMap{f =>
      Path(s"${d.pwd}${f}") match
        case Some(p) => Seq(p)
        case None => Nil
    }
  def /(f:String*):Path = d./(f*) // what about File?
  / *def ++(i:Int) = Environment.paths += this./(i)
  def +^(i:Int) = 
    (this.^(i)).++
    Environment.files* /
  def ^ = alascala.Alias.ls(using this).filter(! _.endsWith("/"))
    .flatMap{
      File(_)(using this) match
        case Some(f) => Seq(f)
        case None => Nil
    }
  def ^(f:String):File =
    File(f)(using this) match
      case Some(file) => file
      case None => throw Exception(s"/$f not found in ${d.pwd}")*/
  //def ^^ = d.path
  //def <=(sh:Environment) = sh.d = d
  def <<(i:Int) = d = Environment.paths.toSeq(i)
  def ++ = Environment.paths += d
  override def toString() = s"${d}\\$$/"

object Environment:
  def apply(s:String):Environment = new Environment(Root./(s.trim.stripPrefix("/")))
  def ^(using sh:Environment) = sh
  def ^^(using sh:Environment) = 
    sh.cd(sh.d.path)
    sh
  def ^^^ = home.root
  def / = root.root
  //def /(f:String*):AbsolutePath = Environment./.d./(f*)
  def /^ = root
  def /! = crash
  def >>(i:Int) = 
    val p = paths.toSeq(i)
    paths -= p
    p

  import scala.collection.mutable.SortedSet
  val paths:SortedSet[AbsolutePath] = SortedSet()
  val files:SortedSet[File] = SortedSet()
  val track:SortedSet[SomePath] = SortedSet()
  var filter:String => Boolean = _ => true

  import scala.sys.process.{ Process, stringToProcess }
  val home:Environment = Environment("pwd".!!)
  val root =new Environment(Root)
  val crash = root

  given ENV:Environment = home
