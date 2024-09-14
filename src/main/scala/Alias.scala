package alascala

import scala.sys.process._
import scala.concurrent.{ Future, ExecutionContext }
import scala.util.{ Success, Failure }

import ExecutionContext.Implicits.global

import alascala.Environment
import Environment.{_, given}

object Alias:
  def apply(cmd:String*) = Future(cmd.!)
  private object Alacritty:
    def apply(cmd:String*)(using sh:Environment) = cmd match
      case Nil => Alias("alacritty", "--working-directory", pwd, "--config-file", Constant.ALACRITTY_CONFIG)
      case _ => Alias((Seq("alacritty", "--working-directory", pwd, "--config-file", Constant.ALACRITTY_CONFIG, "-e") ++ cmd)*)
  def man(s:String) = Alacritty("man", s)
  def sbt(using sh:Environment) = Alacritty("sbt")
  def v(using sh:Environment) = fs.map(_.toString().trim()).toArray() match
    case Array() => Alacritty("/usr/bin/nvim", pwd)
    case l => Alacritty(("/usr/bin/nvim" +: l)*)
  def alias = Alacritty("/usr/bin/nvim", s"${Constant.ALASCALA_HOME}/src/main/scala/Alias.scala")
  def packit = Alias("alacritty", "--working-directory", Constant.ALASCALA_HOME, "--config-file", Constant.ALACRITTY_CONFIG, "-e", "sbt", "package")
  def pacman = Alacritty("sudo", "pacman", "-Suy")
  def brave = Alias("brave", "--enable-features=UzeOzonePlatform", "--ozone-platform=wayland", "--ignore-gpu-blocklist", "--enable-gpu-rasterization", "--use-gl=egl", "--gtk-version=4")
  def alacritty = Alacritty()  
  def termite = Alias("termite")

  def wd(using sh:Environment) = sh.d
  def pwd(using sh:Environment) = wd.toString().trim
  def ls(using sh:Environment) = wd.ls
  def cd(i:Int)(using sh:Environment) =
    val p =ls(i)._2
    if p.endsWith("/") then sh.cd(p.stripSuffix("/"))    
    else throw WTFRUDoing(s">>>$p<<< is not a directory")
    sh
  def sf(i:Int*)(using sh:Environment) =
    i.foreach{j =>
      val f =ls(j)._2
      if ! f.endsWith("/") then 
        File(f, sh.d).++
    }
  def find = //add check with type, filters, RelativePath..? time...
    println(Find())
    Find().!!.split("\n")
      .toSeq
      .filter(Environment.filter)
      .map(File(_))
  def find(x:Any*):String = Find(x*)
  def ds = Environment.paths.toSeq
  def fs = Environment.files.filter((f:File) => Environment.filter(f.file)).toSeq
  def hs = Environment.track.toSeq
  def hshow = 
    import alascala.FilePath.display
    Environment.track.display()
  def hscls(relative2:Boolean = false)(using Environment) =
    val t2 = Environment.track.clone()
    Environment.track.clear()
    import scala.sys.process.{ Process, stringToProcess }
    def realPath(r:RelativePath)(using sh:Environment):RelativePath =
      val x =r /: sh.d
      if s"test -d '${x.toString().trim}'".! == 0 then r
      else realPath(r.path)
    t2.foreach{_ match
      case a:AbsolutePath => Environment.realPath(a).track()
      case r:RelativePath => 
        if relative2 then realPath(r).track()
        else r.track()
      case _ => ???
    }
    hshow

//set working directory environment variable to get relative paths? 2do
case class Find(f:Boolean, name:Option[String], dt:Option[Int]):
  override def toString():String = Seq(
      "find", Alias.pwd, name match
        case Some(s) => s"-name '$s'"
        case None => "", dt match
        case Some(i) => s"-amin -$i"
        case None => "",
      "-type", if f then "f" else "d"
    ).mkString(" ")
  def <(t:String):Find = this //copy(dt = t)
object Find:
  def apply(x:Any*):String = 
    seek = x.foldLeft(seek){(a, o) => o match
      case s:String if s == "" => a.copy(name = None)
      case s:String => a.copy(name = Some(s))
      case i:Int if i == 0 => a.copy(dt = None)
      case i:Int => a.copy(dt = Some(i))
      case b:Boolean => a.copy(f = b)
    }
    seek.toString()
  private var seek:Find = Find(true, None, None)
