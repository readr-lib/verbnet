import sbt._
import Keys._
import sbtassembly.AssemblyPlugin.autoImport._

object libBuild extends Build {

  lazy val commonSettings = Seq(
    version := "1.2-SNAPSHOT",
    organization := "com.readr.lib",
    scalaVersion := "2.11.4",
    publishTo := Some(Resolver.file("file", new File(System.getProperty("user.home") + "/tmp/repo")))
  )

  lazy val proj =  (project in file(".")).
    settings(commonSettings: _*).
    settings(
      // your settings here
      name := "verbnet",
      libraryDependencies ++= Seq(
        "com.readr" %% "model" % "1.2-SNAPSHOT",
        "com.readr" %% "client" % "1.2-SNAPSHOT"
      ),
      resolvers ++= Seq[Resolver](
        "Readr snapshots" at "http://snapshots.mvn-repo.readr.com",
        "Readr releases" at "http://releases.mvn-repo.readr.com"
      ),
      assemblyJarName in assembly := name.value + "-assembly.jar",
      artifact in (Compile, assembly) := {
        val art = (artifact in(Compile, assembly)).value
        art.copy(`classifier` = Some("assembly"))
      }
    ).
    settings(
      addArtifact(artifact in (Compile, assembly), assembly):_*
    )
}
