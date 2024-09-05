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

  def pwd(using sh:Environment) = sh.d.toString().trim
  /*def cd(i:Int)(using sh:Environment) = 
    ls.filter(_.endsWith("/"))
      .map(s => Path(sh.d.pwd + s))(i)*/
  /*def cd(f:String*)(using sh:Environment):Environment = 
    sh./(f*).<=(sh)
    sh*/
  /*def ls(using sh:Environment):Seq[String] = 
    sh.d./?
      .filter(f => f.endsWith("/") || Environment.filter(f))*/
  def ds = Environment.paths.toSeq
  def fs = Environment.files.filter((f:File) => Environment.filter(f.toString())).toSeq
  def hs = Environment.track.toSeq
  def hs(i:Int, j:Int):FilePath = Environment.track.toSeq(i)(j)
