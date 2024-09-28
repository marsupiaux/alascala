package alascala.scripts

def stdin = Iterator.continually(scala.io.StdIn.readLine()).takeWhile(s => s != null)

@main def grep(search:String) =
  stdin.foreach{s =>
      if s.contains(search) then
        println(s)
    }
