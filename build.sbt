import Dependencies._
import sbt.Keys.{scalacOptions, _}

lazy val commonSettings = Seq(
  organization := "com.ziverge",
  version := "0.0.1-SNAPSHOT",
  scalaVersion := "2.13.2",
  scalacOptions := Seq(
    "-Ymacro-annotations",
    "-deprecation",
    "-encoding",
    "utf-8",
    "-explaintypes",
    "-feature",
    "-unchecked",
    "-language:postfixOps",
    "-language:higherKinds",
    "-language:implicitConversions",
    "-Xcheckinit",
    "-Ywarn-unused:imports",
    "-Xfatal-warnings"
  ),
  testFrameworks := Seq(new TestFramework("zio.test.sbt.ZTestFramework")),
  cancelable in Global := true,
  fork in Global := true
)

lazy val zivStream = project
  .in(file("ziv-stream"))
  .enablePlugins(JavaAppPackaging, PackPlugin)
  .settings(commonSettings)
  .settings(
    name := "ziv-stream",
    libraryDependencies ++= zio ++ http4s ++ circe :+ pureConfig :+ logback
  )

lazy val root = project
  .in(file("."))
  .settings(
    name := "ziverge-challenge",
    skip in publish := true,
    Global / onChangedBuildSource := ReloadOnSourceChanges
  )
  .aggregate(zivStream)
