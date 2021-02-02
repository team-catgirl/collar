# collar

Online services for minecraft mods.

Including:
* Sharing coordinates with a group
* Faction groups (persistent groups of players)
* Messaging (private, faction & group)
* Shared textures

## Building
To build run you will need to have Maven 3 installed. 

Execute:
`mvn clean install`

## Using in Forge

We need to shadow the client dependency in your Jar and relocate the `team.catgirl.collar.coordshare` package
in order to avoid conflicts with other mods.

```
repositories {
  mavenLocal()
}
dependencies {
  compile group: 'team.catgirl.collar.collar', name: 'client', version: '1.0-SNAPSHOT'
}
apply plugin: 'com.github.johnrengelman.shadow'
shadowJar {
  // Only shadow fluent-hc
  dependencies {
    include(dependency('team.catgirl.collar:client:.*'))
  }

  // Replace com.yourpackage with your mods package
  relocate 'team.catgirl.collar', 'com.yourpackage.team.catgirl.collar'

  classifier '' // Replace the default JAR
}
reobf {
  shadowJar {} // Reobfuscate the shadowed JAR
}
```

### Constructing a collar client

```
CollarConfiguration configuration = new CollarConfiguration.Builder()
                .withCollarServer("http://localhost:3000/")
                .withHomeDirectory(new File("target"))
                .withMojangAuthentication(() -> MinecraftSession.from(username, password, "smp.catgirl.team"))
                .withPlayerPosition(() -> new Position(1d, 1d, 1d, 0))
                .withListener(collarListener)
                .build();
Collar collar = Collar.create(configuration);
collar.connect();
```
