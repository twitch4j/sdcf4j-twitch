# sdcf4j-twitch
Sdcf4j is a **s**imple **D**iscord **c**ommand **f**ramework **for** **J**ava, supporting [Javacord](https://github.com/BtoBastian/Javacord), [JDA](https://github.com/DV8FromTheWorld/JDA), [Discord4J](https://github.com/austinv11/Discord4J) and now [Twitch4J](https://github.com/PhilippHeuer/twitch4j). It helps you creating commands within seconds in a clean and simple way.

A ping command is as easy as this:
```java
@TwitchCommand(aliases = "ping", description = "Pong!")
public String onPingCommand() {
    return "Pong!";
}
```

##  Maven
```xml
<repository>
  <id>jitpack.io</id>
  <url>https://jitpack.io</url>
</repository>
...
<!-- The module for Twitch4J -->
<dependency>
  <groupId>com.github.twitch4j</groupId>
  <artifactId>sdcf4j-twitch</artifactId>
  <version>%version%</version>
</dependency>
```
Make sure to replace `%version%` with the latest version number, e.g. `v1.0.0` (don't use this one!).

## Support
 
* [Twitch4J Discord Server](https://discord.gg/FQ5vgW3)

## Usage
Take a look at the [wiki](https://github.com/BtoBastian/sdcf4j/wiki) for a detailed description on how to use the library.

Current annotation is supported for Twitch4J script. Don't even try use `@Command` annotation. It would not add some methods when doesn't have `@TwitchCommand` annotation

## Creator of Core script sdcf4j

Checkout [referenced](https://github.com/BtoBastian/sdcf4j) repository.
