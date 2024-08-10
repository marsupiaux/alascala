# Alascala

alascala is an integration of the Scala 3 REPL into Alacritty for daily use as a command line shell.

## Installation

1. configure ~/.config/alacritty/alacritty.toml to use Scala 3:
```toml
import = ["~/.config/alacritty/config.toml"]

[shell]
program = "/usr/bin/scala3"
args = ["-classpath", "/path/of/this/project/.jar"]
```
2. configure the Constant.scala file of this project to use ~/.config/alacritty/config.toml as the main config when calling Alacritty:
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
4. make a link from the home directory to the alascala .jar to make Step 1 above easier to maintain:
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
cd("..")
```

and then the aliases will use that environment:
```scala
v //opens the directory opened by the cd command in nvim
```

## ToDo

1. clean up reduntant parts of the path
