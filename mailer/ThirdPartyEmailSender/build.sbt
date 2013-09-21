scalaVersion := "2.10.2"

libraryDependencies ++= Seq(
    "ch.qos.logback" % "logback-classic" % "1.0.13",
    "com.typesafe" %% "scalalogging-slf4j" % "1.0.1",
    "com.typesafe.akka" %% "akka-actor" % "2.1.4",
    "play" %% "anorm" % "2.1.1",
    "com.github.seratch" %% "scalikejdbc" % "[1.6,)",
    "com.github.seratch" %% "scalikejdbc-config" % "[1.6,)",
    "mysql" % "mysql-connector-java" % "5.1.26",
    "org.apache.commons" % "commons-email" % "1.3.1"
)
