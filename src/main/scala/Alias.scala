package alascala

import scala.sys.process._
import scala.concurrent.{ Future, ExecutionContext }

import ExecutionContext.Implicits.global

import alascala.Environment
import Environment.{_, given}

object Alias:
  def apply(cmd:String*) = Future(cmd.!)
  private object Alacritty:
    def apply(cmd:String*)(using sh:Environment) = cmd match
      case Nil => Alias("alacritty", "--working-directory", Environment.pwd, "--config-file", Constant.ALACRITTY_CONFIG)
      case _ => Alias((Seq("alacritty", "--working-directory", sh.d.pwd, "--config-file", Constant.ALACRITTY_CONFIG, "-e") ++ cmd)*)

  def man(s:String) = Alacritty("man", s)
  def sbt(using sh:Environment) = Alacritty("sbt")
  def v(using sh:Environment) = Alacritty("/usr/bin/nvim", sh.d.pwd)
  def alias = Alacritty("/usr/bin/nvim", s"${Constant.ALASCALA_HOME}/src/main/scala/Alias.scala")
  def packit = Alias("alacritty", "--working-directory", Constant.ALASCALA_HOME, "--config-file", Constant.ALACRITTY_CONFIG, "-e", "sbt", "package")
  def pacman = Alacritty("sudo", "pacman", "-Suy")
  def brave = Alias("brave", "--enable-features=UzeOzonePlatform", "--ozone-platform=wayland", "--ignore-gpu-blocklist", "--enable-gpu-rasterization", "--use-gl=egl", "--gtk-version=4")
  def alacritty = Alacritty()  
  def termite = Alias("termite")

