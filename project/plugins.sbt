resolvers += Resolver.typesafeRepo("releases")

resolvers += "scm-manager releases repository" at "http://maven.scm-manager.org/nexus/content/groups/public"

addSbtPlugin("com.byteground" %% "byteground-sbt-community-settings" % "3.5.0")