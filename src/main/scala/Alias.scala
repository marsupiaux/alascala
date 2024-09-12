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
    else throw Exception(s">>>$p<<< is not a directory")
    sh
  def sf(i:Int*)(using sh:Environment) =
    i.foreach{j =>
      val f =ls(j)._2
      if ! f.endsWith("/") then 
        File(f, sh.d).++
    }
  def ds = Environment.paths.toSeq
  def fs = Environment.files.filter((f:File) => Environment.filter(f.file)).toSeq
  def hs = Environment.track.toSeq
  def hshow = 
    import alascala.FilePath.display
    Environment.track.display()
  def hsclean =   
    val t2 = Environment.track.clone()
    Environment.track.clear()
    import scala.sys.process.{ Process, stringToProcess }
    def realPath(p:AbsolutePath):SomePath =
      if s"test -e '${p.toString().trim}'".! == 0 then p
      else realPath(p.path)
    t2.foreach{_ match
      case a:AbsolutePath => realPath(a).track()
      case r:RelativePath => r.track() //<-- do we want to clean these up?
    }
    hshow

