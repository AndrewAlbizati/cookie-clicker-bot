# Cookie Clicker Discord Bot
This bot allows users to play Cookie Clicker online through Discord.
This bot was written in Java 17 using IntelliJ IDEA and is designed to be used on small Discord servers.

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
Type /help in any channel for instructions on how to play.
In order for a user to play the game, they must allow bots to directly message them.
Once the user receives the message, it will contain information about the shop, as well as a button with a cookie emoji on it.
Whenever the user presses that button, they will earn 1 cookie, which they can then use to buy items that will earn them more cookies.


## Commands

### /newgame
Starts a new game of Cookie Clicker. The user starts with 0 cookies, and must click on the cookie button to earn cookies in order to buy items.

### /buy \<item> [amount]
This command allows users to purchase items in their game. The items that the user can purchase are:

1. **Cursor**
   1. 15 cookies
   2. 0.2 CPS
2. **Grandma**
   1. 100 cookies
   2. 0.8 CPS
3. **Factory** 
   1. 500 cookies
   2. 4.0 CPS
4. **Mine**
   1. 2,000 cookies
   2. 10.0 CPS
5. **Shipment**
   1. 7,000 cookies
   2. 20.0 CPS
6. **Alchemy Lab**
   1. 50,000 cookies
   2. 100.0 CPS
7. **Portal**
   1. 1,000,000 cookies
   2. 1,333.2 CPS
8. **Time Machine**
   1. 123,456,789 cookies
   2. 24,691.2 CPS
   
#### Pricing Formulas
<img src="https://latex.codecogs.com/svg.image?Price\:=\:Base\:cost\:*\:1.1^M" />

Where **M** = number of that type of building currently owned.

<img src="https://latex.codecogs.com/svg.image?Cumulative\:price\:=\:\frac{Base\:cost\:*\:(1.1^N\:-\:1)}{0.1}" />

Where **N** = the number of buildings.

### /resendmessage
Deletes the original Cookie Clicker message and sends a new message to the user. All game updates will be sent to the new message.

### /quit
Stops the game that the user is currently playing, and deletes it from the saves file.

### /help
Explains how to start a new game, and shows a leaderboard of all users that are playing Cookie Clicker on that instance of the bot.

## Dependencies
- Javacord 3.4.0 (https://github.com/Javacord/Javacord)
- JSONSimple 1.1.1 (https://github.com/fangyidong/json-simple)