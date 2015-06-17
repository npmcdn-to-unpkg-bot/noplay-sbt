resolvers += Resolver.typesafeRepo("releases")

resolvers += "scm-manager releases repository" at "http://maven.scm-manager.org/nexus/content/groups/public"

resolvers += "ByTeGround Maven Public Releases Repository" at "http://maven.byteground.com/public/releases"

addSbtPlugin("com.byteground" %% "byteground-sbt-community-settings" % "3.16.0")