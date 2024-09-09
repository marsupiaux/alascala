# Alascala

alascala is an integration of the Scala 3 REPL / scala-cli into Alacritty for daily use as a command line shell.

capitalise on the REPL's follow on syntax to make a drill down path navigation system

## Installation

1. configure ~/.config/alacritty/alacritty.toml to use Scala 3:
```bash
cd ~/.config/alacritty
mv alacritty.toml config.toml
nvim alacritty.toml
```

```toml
import = ["~/.config/alacritty/config.toml"]

[shell]
program = "/usr/bin/scala3"
args = ["-classpath", "/path/of/this/project/.jar"]
```
2. configure Constant.scala with the home of this project & to use ~/.config/alacritty/config.toml as the main config when calling Alacritty:
```bash
cd /path/of/alascala
nvim src/main/scala/Constants.scala
mv src/main/scala/Constants.scala src/main/scala/Constant.scala
```
3. use SBT to package this project into a .jar file:
```bash
cd /path/of/alascala
sbt package
```
4. make a link from the project's home directory to the alascala .jar to make Step 1 above easier to maintain:
```bash
cd /path/of/alascala
find . -name "*.jar"
ln -s /path/of/this/project/s/jar .jar
```

## Usage

When the shell starts up you can import the functions in Alias & Environment:

```scala
import alascala.Alias._
import alascala.Environment._
import alascala.FilePath.stringToPath

import scala.sys.process._
```

Add new functions with the shortcut:

```scala
alias
```

& repackage the changes for usage when you start a new shell with:

```scala
packit
```

Then use commands that emulate bash:
```scala
^                       //is your current work environment
"../scala-project" <= ^ //will take you to that place
"src/main/scala".*      //now you have a handle to all the files in that relative directory
```

and then the aliases will use that environment:
```scala
alacritty               //opens in the current working directory `^` opened by the cd command in nvim
v                       //opens nvim w the files in `fs` or using `pwd`
```

alternative ways to open a new directory for aliases:
```scala
^^ <= ^
/ / "home" <= ^
"/home" <= ^

v(using /^)
```

keep references to folders for easy reference
```scala
^.++
"/home"./?              //show listing
"/home".++(0,1)         //add files & folders by index
fs                      //view file listing

ds                      //view saved directories
^ << 0                  //goto 1st of the saved directory

hs                      //view directory history
hs(2,1)                 //take the 3rd path & rm 1 segment
```

## ToDo

0. update java.lang.Exception -> alascala.WTFRUDoing
1. capitalise on REPL follow on functionality (may need to get that restored 1st???)
2. check Exists & save as File if appropriate
3. integrate find, etc. commands
4. explore & provide seemless integration with hyaoi's os package
