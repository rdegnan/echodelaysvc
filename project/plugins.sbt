logLevel := Level.Warn

addSbtPlugin("io.spray" % "sbt-revolver" % "0.7.2")

addSbtPlugin("org.scalastyle" %% "scalastyle-sbt-plugin" % "0.8.0")

addSbtPlugin("org.scoverage" % "sbt-scoverage" % "1.5.0")

addSbtPlugin("org.xerial.sbt" % "sbt-pack" % "0.7.9")

addSbtPlugin("se.marcuslonnberg" % "sbt-docker" % "1.5.0")

addSbtPlugin("com.thesamet" % "sbt-protoc" % "0.99.12")

libraryDependencies += "com.trueaccord.scalapb" %% "compilerplugin" % "0.6.6"
