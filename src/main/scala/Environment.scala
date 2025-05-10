package alascala

import scala.collection.immutable.SortedSet
import scala.sys.process.{ Process, stringToProcess }


class Environment(home:AbsolutePath):
  var d = home

  def root = 
    d = home
    this
  def ^^ =
    d = d.path
    this
  def /(i:Int) = cd(i)
  def /(f:String) = cd(f)
  def ls = d.ls
  infix def cd(i:Int):Environment =
    val p =ls(i)._2
    if p.endsWith("/") then cd(p.stripSuffix("/"))    
    else throw WTFRUDoing(s">>>$p<<< is not a directory")
    this
  infix def cd(s:String):Environment = cd(d./(s))
  infix def cd(p:AbsolutePath):Environment = 
    if s"test -d '${p.pretty}'".! == 0 then
      d = p
      load
    else 
      Environment.crash = Environment.realPath(p)
      throw WTFRUDoing(s">>>${p.pretty}<<< does not exists")
  def cd(p:RelativePath):Environment = cd(p /: d)
  /*def <<(i:Int) = d = paths.toSeq(i)
  def >>(i:Int) = 
    val p = paths.toSeq(i)
    paths = paths - p
    p*/
  def ++ = paths = paths + d
  def lock:Unit =
    base_path.mkdir
    //val tank:SortedSet[AbsolutePath] = tanks + d
      /*if tank_path.exists then 
        tank_path.parse(_.d) + d
      else SortedSet(d)*/
    tank_path.write{
      (d :: tanks_d)
        .map{ _.toString }
        .reduce{ _ + _ }
        .tail
    }
    //log current settings
    env_path.mkdir
    if !files.isEmpty then
      fs_path.write{
        files
          .map{ _.toString }
          .reduce{ _ + _ }
          .tail
      }
    if !paths.isEmpty then
      ds_path.write{
        paths
          .map{ _.toString }
          .reduce{ _ + _ }
          .tail
      }
  def load:Environment =
    if fs_path.exists then
      files = files ++ fs_path.parse[File](_.f)
    if ds_path.exists then
      paths = paths ++ ds_path.parse[AbsolutePath](_.d)
    //if s"test -f '${f.pretty}/find'".! == 0 then
    this
  def zero =
    fs0
    ds0
    load
  def boom:Unit =
    if d == home then throw WTFRUDoing("this is the home base, do you want to destroy everything?!?")
    else
      if d.rm_r == 0 then
        tank_path.write{
          tanks_d
            .map{ _.toString }
            .reduce{ _ + _ }
            .tail
        }
        Environment.^^^
  def tanks_d =
    if tank_path.exists then
      tank_path
        .parse(_.d)
        .toList
        .rm(d)
    else List.empty
    
  val base_path = home / "..."
  val tank_path = File("...", base_path)
  def env_path = d / "..."
  def fs_path = File("fs", env_path)
  def ds_path = File("ds", env_path)
  
  var paths:SortedSet[AbsolutePath] = SortedSet.empty()
  var files:SortedSet[File] = SortedSet.empty() //should these be local to ea env?

  def ds = paths.toSeq
  def ds0 = 
    paths = SortedSet.empty()
    paths.toSeq
  def fs = files.filter((f:File) => Environment.filter(f.file)).toSeq
    //make filter using to allow local override???
  def fs0 = 
    files = SortedSet.empty()
    files.toSeq

  override def toString() = s"${d}\\$$/"

object Environment:

  def apply(s:String):Environment = new Environment(Root./(s.trim.stripPrefix("/")))
  def realPath(p:AbsolutePath):AbsolutePath = if p.exists then p else realPath(p.path)
  def ^(using sh:Environment) = sh
  def ^^(using sh:Environment) = sh.^^
  def ^^^(using sh:Environment) = 
    sh.root.tank_path.parse(_.d) match
      case p :: Nil => 
        sh.cd(p)
        sh.tank_path.parse(_.d) //considering taking tanks local
      case x => x
  def /^ = root
  def /! = crash

  var track:SortedSet[SomePath] = SortedSet.empty()

  val root =new Environment(Root)
  val home:Environment = Environment("pwd".!!)

  given ENV:Environment = home

  def wd(using sh:Environment) = sh.d
  def pwd(using sh:Environment) = wd.pretty
  def ls(using sh:Environment) = sh.ls
  def cd(i:Int)(using sh:Environment) = sh.cd(i)
  def cd(f:String)(using sh:Environment) = sh.cd(f)
  def mkdir(f:String)(using sh:Environment) = Path(f, sh.d).mkdir
  def dp(i:Int)(using sh:Environment) = ls(i)._2 match
    case d if d.endsWith("/") => Path(d.stripSuffix("/"), sh.d)
    case f => File(f, sh.d)
  def lock(using sh:Environment) = sh.lock
  def load(using sh:Environment) = sh.load
  def zer0(using sh:Environment) = sh.zero
  def boom(using sh:Environment) = sh.boom
  def ds(using sh:Environment) = sh.ds
  def ds0(using sh:Environment) = sh.ds0
  def fs(using sh:Environment) = sh.fs
  def fs0(using sh:Environment) = sh.fs0

  def hs = Environment.track.toSeq //... gets tracked but .../fs does not (because it's a File?)
  def hshow = 
    import alascala.FilePath.display
    Environment.track.display()
  def hscls(relative2:Boolean) = //(using Environment) =
    var t:SortedSet[SomePath] = SortedSet.empty()
    def actualPath(r:RelativePath)(using sh:Environment):RelativePath =
      val x =r /: sh.d
      if s"test -d '${x.pretty}'".! == 0 then r
      else actualPath(r.path)
    Environment.track.foreach{_ match
      case a:AbsolutePath => t = t + Environment.realPath(a)
      case r:RelativePath => 
        if relative2 then t = t + actualPath(r)
        else t = t + r
    }
    Environment.track = t
    hshow
  var crash:AbsolutePath = Root

  var filter:String => Boolean = _ => true //mv filter into class & save in hub???
//combine filters, make filters case class? need some sort of find query object that we can build
object Filter:
  def none = Environment.filter = _ => true
  def endsWith(s:String):Unit = Environment.filter = _.endsWith(s)
