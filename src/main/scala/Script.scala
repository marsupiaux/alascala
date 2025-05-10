package alascala {

object Script:
  val BIN = "/home/emajin/scala/alascala/script/"

  def stdin = Iterator.continually(scala.io.StdIn.readLine()).takeWhile(s => s != null)

  def ps(s:String*) = scala.sys.process.Process("scala3" +: s, java.io.File(Script.BIN))

  def grep(s:String, f:String = "{lNo.} - {line}\n") = ps("grep", s, f)
}
@main def grep(search:String, format:String = "{lNo.} - {line}\n") =
  var lNo = 0
  alascala.Script.stdin.foreach{s =>
      if s.contains(search) then
        print(format.replace("{lNo.}", lNo.toString()).replace("{line}", s))
      lNo = lNo + 1
    }
