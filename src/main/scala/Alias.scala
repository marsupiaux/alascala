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
      case Nil => alacritty
      case _ => Alias((Seq("alacritty", "--working-directory", pwd, "--config-file", Constant.ALACRITTY_CONFIG, "-e") ++ cmd)*)
  private object Sub:
    def apply(cmd:String*) = //thanks to vitalii - https://stackoverflow.com/questions/44896739/is-it-possible-to-open-a-interactive-vim-process-by-scala-repl-shell-command
      System.out.println(s" ${cmd.mkString(" ")} ... executing\n")
      import java.lang.{ Process, ProcessBuilder }
      val ps = new ProcessBuilder(cmd*)
      ps.directory(wd)
      ps.redirectInput(ProcessBuilder.Redirect.INHERIT)
      ps.redirectOutput(ProcessBuilder.Redirect.INHERIT)
      ps.redirectError(ProcessBuilder.Redirect.INHERIT)
      val p = ps.start()
      p.waitFor()
      System.out.print(".")
  def man(s:String) = Sub("man", s)
  def sbt(using sh:Environment) = Alacritty("sbt")
  def v(using sh:Environment) = fs.map(_.pretty).toArray() match
    case Array() => Alacritty("/usr/bin/nvim", pwd)
    case l => Alacritty(("/usr/bin/nvim" +: l)*)
  def alias = Sub("/usr/bin/nvim", s"${Constant.ALASCALA_HOME}/src/main/scala/Alias.scala")
  def packit = Sub("dash", "-c", s"cd ${Constant.ALASCALA_HOME} && sbt package")
  def pacman = Sub("sudo", "pacman", "-Suy")
  def brave = Alias("brave", "--enable-features=UzeOzonePlatform", "--ozone-platform=wayland", "--ignore-gpu-blocklist", "--enable-gpu-rasterization", "--use-gl=egl", "--gtk-version=4")
  def alacritty = Sub("bash", "-l")
  def termite = Alias("termite")
  def free =Sub("free")

  def wd(using sh:Environment) = sh.d
  def pwd(using sh:Environment) = wd.pretty
  def ls(using sh:Environment) = wd.ls
  def cd(i:Int)(using sh:Environment) =
    val p =ls(i)._2
    if p.endsWith("/") then sh.cd(p.stripSuffix("/"))    
    else throw WTFRUDoing(s">>>$p<<< is not a directory")
    sh
  /*def sf(i:Int*)(using sh:Environment) =
    i.foreach{j =>
      val f =ls(j)._2
      if ! f.endsWith("/") then 
        File(f, sh.d).++
    }*/
  def load = Process(Seq("scalac3", s"${Constant.ALASCALA_HOME}/src/main/scala/Script.scala"), Constant.ALASCALA_FILE).!!
  def script = Sub("/usr/bin/nvim", s"${Constant.ALASCALA_HOME}/src/main/scala/Script.scala")
  def ps(s:String*) = Process("scala3" +: s, Constant.ALASCALA_FILE)
  def find = //add check with type, filters, RelativePath..? time...
    println(Find())
    Find().lazyLines_! //.split("\n")
      //.toSeq
      .filter(Environment.filter)
      .map(File(_))
  def find(x:Any*):String = Find(x*)
  def ds = Environment.paths.toSeq
  def fs = Environment.files.filter((f:File) => Environment.filter(f.file)).toSeq
  def hs = Environment.track.toSeq
  def hshow = 
    import alascala.FilePath.display
    Environment.track.display()
  def hscls(relative2:Boolean)(using Environment) =
    import scala.collection.immutable.SortedSet
    import scala.sys.process.{ Process, stringToProcess }
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

//set working directory environment variable to get relative paths? 2do
case class Find(file:Boolean, path:RelativePath, name:Option[String], dt:Option[Int]):
  override def toString():String = Seq(
      "find", (path /: Alias.wd).pretty, 
      name match
        case Some(s) => s"-name '$s'"
        case None => "", 
      dt match
        case Some(i) => s"-amin -$i"
        case None => "",
      "-type", if file then "f" else "d"
    ).filter(_ != "").mkString(" ")
  def <(t:String):Find = this //copy(dt = t)
object Find:
  def apply(x:Any*):String = 
    seek = x.foldLeft(seek){(a, o) => o match
      case p:RelativePath => a.copy(path = p)
      case s:String if s == "" => a.copy(name = None)
      case s:String => a.copy(name = Some(s))
      case i:Int if i == 0 => a.copy(dt = None)
      case i:Int => a.copy(dt = Some(i))
      case b:Boolean => a.copy(file = b)
    }
    seek.toString()
  private var seek:Find = Find(true, Dot, None, None)
