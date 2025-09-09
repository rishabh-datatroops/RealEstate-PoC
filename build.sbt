scalaVersion := "2.13.8"

lazy val root = (project in file("."))
  .settings(
    name := "real-estate-poc",
    version := "0.1.0",
    scalaVersion := "2.13.2",
    libraryDependencies ++= Seq(
      "com.typesafe.akka" %% "akka-actor-typed" % "2.6.20",
      "com.typesafe.akka" %% "akka-stream" % "2.6.20",
      "com.typesafe.akka" %% "akka-http" % "10.2.10",
      "com.typesafe.akka" %% "akka-http-spray-json" % "10.2.10", // for jsonFormatN, toJson, as[T]
      "com.typesafe.akka" %% "akka-slf4j" % "2.6.20",
      "ch.qos.logback" % "logback-classic" % "1.2.12", // logging
      "com.typesafe.slick" %% "slick" % "3.3.3",
      "org.postgresql" % "postgresql" % "42.7.3",
      "com.typesafe.slick" %% "slick-hikaricp" % "3.3.3",
      "org.testcontainers" % "postgresql" % "1.19.7" % Test,
      "org.scalatest" %% "scalatest" % "3.2.19" % Test,
      // Akka Kafka
      "com.typesafe.akka" %% "akka-stream-kafka" % "3.0.1",

      // Apache Kafka client (Producer/ConsumerRecord, Serializer/Deserializer)
      "org.apache.kafka" % "kafka-clients" % "3.6.1",
      "net.debasishg" %% "redisclient" % "3.41",
    )
  )
