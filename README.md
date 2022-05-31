# Cookie Clicker Discord Bot
This bot allows users to play Cookie CLicker online through Discord. This bot was written in Java 17 using IntelliJ IDEA and is designed to be used on small Discord servers.

## Setup
1. Add a file titled `config.properties` into the folder.
2. Add the following to the file (replace `{Discord bot token}` with your bot's token)
```
token={Discord bot token}
```
3. If on Windows:
    1. `gradlew build`
    2. `move build\libs\cookie-clicker-bot-1.0.0.jar .`
    3. `java -jar cookie-clicker-bot-1.0.0.jar`

4. If on macOS/Linux
    1. `chmod +x gradlew`
    2. `./gradlew build`
    3. `mv build/libs/cookie-clicker-bot-1.0.0.jar .`
    4. `java -jar cookie-clicker-bot-1.0.0.jar`

## How to Play
Type /help in any channel for instructions on how to play. In order for a user to play the game, they must allow bots to directly message them.

## Dependencies
- Javacord 3.4.0 (https://github.com/Javacord/Javacord)
- JSONSimple 1.1.1 (https://github.com/fangyidong/json-simple)