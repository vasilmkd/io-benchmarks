ThisBuild / scalaVersion := "2.13.4"
ThisBuild / version := "0.0.1"

lazy val ce2 = project.in(file("ce2"))
  .enablePlugins(JmhPlugin)
  .settings(libraryDependencies += "org.typelevel" %% "cats-effect" % "2.3.1")

lazy val ce3 = project.in(file("ce3"))
  .enablePlugins(JmhPlugin)
  .settings(libraryDependencies += "org.typelevel" %% "cats-effect" % "3.0.0-M4")

lazy val monix = project.in(file("monix"))
  .enablePlugins(JmhPlugin)
  .settings(libraryDependencies += "io.monix" %% "monix" % "3.3.0")

lazy val zio = project.in(file("zio"))
  .enablePlugins(JmhPlugin)
  .settings(libraryDependencies += "dev.zio" %% "zio" % "1.0.4")
