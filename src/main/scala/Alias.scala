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
  object Sub:
    def apply(cmd:String*):Int = //thanks to vitalii - https://stackoverflow.com/questions/44896739/is-it-possible-to-open-a-interactive-vim-process-by-scala-repl-shell-command
      System.out.println(s" ${cmd.mkString(" ")} ... executing\n")
      import java.lang.{ Process, ProcessBuilder }
      val ps = new ProcessBuilder(cmd*)
      ps.environment().put("HISTCONTROL", "ignoreboth")
      ps.directory(wd)
      ps.redirectInput(ProcessBuilder.Redirect.INHERIT)
      ps.redirectOutput(ProcessBuilder.Redirect.INHERIT)
      ps.redirectError(ProcessBuilder.Redirect.INHERIT)
      val p = ps.start()
      val err = p.waitFor()
      System.out.println(".")
      return err
  def man(s:String) = Sub("man", s)
  def sbt(using sh:Environment) = Alacritty("sbt")
  def v(using sh:Environment) = sh.fs.map(_.pretty).toArray() match
    case Array() => Alacritty("/usr/bin/nvim", pwd)
    case l => Alacritty(("/usr/bin/nvim" +: l)*)
  extension (f:AbSeg)
    def v(using sh:Environment) = Sub("/usr/bin/nvim", f.pretty)
  def alias = Sub("/usr/bin/nvim", s"${Constant.ALASCALA_HOME}/src/main/scala/Alias.scala")
  def packit = Sub("dash", "-c", s"cd ${Constant.ALASCALA_HOME} && sbt package")
  def pacman = Sub("sudo", "pacman", "-Suy")
  def brave = Alias("brave", "--enable-features=UzeOzonePlatform", "--ozone-platform=wayland", "--ignore-gpu-blocklist", "--enable-gpu-rasterization", "--use-gl=egl", "--gtk-version=4")
  def alacritty = Sub("bash", "-l")
  def termite = Alias("termite")
  def free =Sub("free")

  def scroll = 
    val d = java.io.File(Script.BIN)
    d.mkdirs()
    Process(Seq("scalac3", s"${Constant.ALASCALA_HOME}/src/main/scala/Script.scala"), d).!!
  def script = Sub("/usr/bin/nvim", s"${Constant.ALASCALA_HOME}/src/main/scala/Script.scala")
  def find = //add check with type, filters, RelativePath..? time...
    println(Find())
    Find().lazyLines_! //.split("\n")
      //.toSeq
      .filter(Environment.filter)
      .map(File(_))
  def find(x:Any*):String = Find(x*)
  def grep(search:String)(using sh:Environment) =
    sh.fs.map{a => (a,
      a.f.>> #| Script.grep(search))
    } flatMap {x =>
      val ls = x._2.!!.split("\n").tail
      if ls.length > 0 then
        println(x._1.pretty)
        ls foreach println
        x._1 :: Nil
      else Nil
    }  

//set working directory environment variable to get relative paths? 2do
case class Find(file:Boolean, path:AbsolutePath, name:Option[String], dt:Option[Int]):
  override def toString():String = Seq(
      "find", path.pretty, 
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
  def apply(x:Any*)(using sh:Environment):String = 
    seek = x.foldLeft(seek){(a, o) => o match
      case d:AbsolutePath => a.copy(path = d)
      case d:RelativePath => a.copy(path = d /: sh.d)
      case s:String if s == "" => a.copy(name = None)
      case s:String => a.copy(name = Some(s))
      case i:Int if i == 0 => a.copy(dt = None)
      case i:Int => a.copy(dt = Some(i))
      case b:Boolean => a.copy(file = b)
      case x => throw WTFRUDoing(s">>>$x<<< UFO")
    }
    seek.toString()
  private var seek:Find = Find(true, Root, None, None)
