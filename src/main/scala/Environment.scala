package alascala



class Environment(home:AbsolutePath):
  var d = home //keep navigation history? favouritesave functionality?

  def root = 
    d = home
    this
  def cd(s:String):Environment = cd(d./(s))
  def cd(p:AbsolutePath):Environment = 
    import scala.sys.process.{ Process, stringToProcess }
    if s"test -d '${p.toString().trim}'".! == 0 then
      d = p
    else 
      Environment.crash = Environment.realPath(p)
      throw WTFRUDoing(s">>>${p.toString().trim}<<< does not exists")
    this
  def cd(p:RelativePath):Environment = cd(p /: d)
  def <<(i:Int) = d = Environment.paths.toSeq(i)
  def ++ = Environment.paths += d
  override def toString() = s"${d}\\$$/"

object Environment:
  def apply(s:String):Environment = new Environment(Root./(s.trim.stripPrefix("/")))
  def realPath(p:AbsolutePath):AbsolutePath =
    import scala.sys.process.{ Process, stringToProcess }
    if s"test -d '${p.toString().trim}'".! == 0 then p
    else realPath(p.path)
  def ^(using sh:Environment) = sh
  def ^^(using sh:Environment) = 
    sh.cd(sh.d.path)
    sh
  def ^^^ = home.root
  def / = root.root
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
  var crash:AbsolutePath = Root

  given ENV:Environment = home

//combine filters, make filters case class? need some sort of find query object that we can build
object Filter:
  def none = Environment.filter = _ => true
  def endsWith(s:String):Unit = Environment.filter = _.endsWith(s)
