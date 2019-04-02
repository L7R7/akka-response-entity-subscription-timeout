organization := "com.l7r7.lab"
name := "akka-response-entity-subscription-timeout"
version := "0.1"

scalaVersion := "2.12.8"

javacOptions ++= Seq("-source", "1.8", "-target", "1.8", "-Xlint")

scalacOptions ++= Seq("-target:jvm-1.8",
                      "-encoding", "UTF8",
                      "-unchecked",
                      "-deprecation",
                      "-explaintypes",
                      "-Xlint",
                      "-Xlog-implicits",
                      "-Xfuture",
                      "-Ypartial-unification",
                      "-Ybackend-parallelism", "8")

conflictManager := ConflictManager.strict

val akkaVersion = "2.5.21"
val akkaHttpVersion = "10.1.7"
val pureconfigVersion = "0.10.2"
val scalaLoggingVersion = "3.9.2"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-http" % akkaHttpVersion,
  "com.typesafe.akka" %% "akka-http-spray-json" % akkaHttpVersion,
  "com.typesafe.akka" %% "akka-http-xml" % akkaHttpVersion,
  "com.typesafe.akka" %% "akka-slf4j" % akkaVersion,
  "com.typesafe.akka" %% "akka-stream" % akkaVersion,
  "com.github.pureconfig" %% "pureconfig" % pureconfigVersion,
  "com.typesafe.scala-logging" %% "scala-logging" % scalaLoggingVersion,

  "com.typesafe.akka" %% "akka-http-testkit" % akkaHttpVersion % Test,
  "com.typesafe.akka" %% "akka-testkit" % akkaVersion % Test,
  "com.typesafe.akka" %% "akka-stream-testkit" % akkaVersion % Test,
  "org.scalatest" %% "scalatest" % "3.0.5" % Test,

  "ch.qos.logback" % "logback-classic" % "1.2.3" % "runtime"
)

dependencyOverrides ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % akkaVersion,
  "com.typesafe.akka" %% "akka-stream" % akkaVersion,
  "org.scala-lang.modules" %% "scala-xml" % "1.1.1"
)
