val scala3Version = "3.6.3"
val sparkVersion = "3.5.5"

lazy val root = project
  .in(file("."))
  .settings(
    name := "console",
    version := "0.0.0-SNAPSHOT",

    scalaVersion := scala3Version,

    /*artifactName := { (sv: ScalaVersion, module: ModuleID, artifact: Artifact) =>
      artifact.name + "_" + sv.binary + "-" + sparkVersion + "_" + module.revision + "." + artifact.extension
    }*/

    //libraryDependencies += "org.scalameta" %% "munit" % "1.0.0" % Test
    libraryDependencies ++= Seq(
      ("org.apache.spark" %% "spark-core" % sparkVersion).cross(CrossVersion.for3Use2_13),
      ("org.apache.spark" %% "spark-sql" % sparkVersion).cross(CrossVersion.for3Use2_13),
      "io.github.vincenzobaz" %% "spark-scala3-encoders" % "0.2.6",
      "io.github.vincenzobaz" %% "spark-scala3-udf" % "0.2.6"
    )
  )
