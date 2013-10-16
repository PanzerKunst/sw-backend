scalaVersion := "2.10.2"

libraryDependencies ++= Seq(
    "ch.qos.logback" % "logback-classic" % "1.0.13",
    "com.typesafe" %% "scalalogging-slf4j" % "1.0.1",
    "com.typesafe.akka" %% "akka-actor" % "2.1.4",
    "com.github.seratch" %% "scalikejdbc" % "[1.6,)",
    "com.github.seratch" %% "scalikejdbc-config" % "[1.6,)",
    "mysql" % "mysql-connector-java" % "5.1.26"
)

seq(com.github.retronym.SbtOneJar.oneJarSettings: _*)
