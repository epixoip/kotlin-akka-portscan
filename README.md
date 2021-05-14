### kotlin-akka-portscan

A toy program to help me (and you!) learn Kotlin + Akka.

### butwhy.gif

When I want to learn a new language, I've found it helpful to attempt to write a silly toy program while I study the 
documentation and other people's code. More often than not, what I end up writing is a [port scanner](https://en.wikipedia.org/wiki/Port_scanner).

Why a port scanner? Well, I'm certainly not trying to compete with Nmap. It's something I started doing over 20 years ago: I wanted to learn 
Visual Basic, and I stumbled across a port scanner someone wrote in VB4, which was right up my alley. So I started hacking on it until there 
was virtually none of the original code remaining. I then ported it to VB5, VB6, Python, and Perl. After learning C, I wanted to learn socket
programming, and I could think of no better project than a port scanner that can do SYN scans with raw sockets. I then ported that program to 
C++ and C#. As a port scanner touches on a majority of the concepts one must learn to write in a language, I've found it to be kind of the 
perfect toy program and thus have kept the tradition going with virtually every language I've learned since.

So here's a toy port scanner I wrote to help me learn Kotlin + Akka, which you can also hack on to help learn these two amazing 
technologies as well!

### About the program

Kotlin is a super terse and concise language that combines object-orient and functional programming. Writing in Kotlin is extremely fun, and is
sort of like a blend of Java, Scala, and Groovy. Kotlin can be compiled to run on everything from bare metal (Kotlin/Native) to the JVM 
(Kotlin/JVM), mobile devices, the web browser as JavaScript (Kotlin/JS), and can even share code between multiple platforms (Kotlin Multiplatform).
This makes Kotlin an extremely attractive language for full stack development, as your back-end, front-end, mobile apps, and desktop apps can
all share a common code base. The majority of Kotlin adopters are currently targeting Android, but I've primarily been using Kotlin for 
server-side and desktop applications over the past year.

Akka is a Scala library that implements reactive programming via the [actor model](https://en.wikipedia.org/wiki/Actor_model), and is 
heavily inspired by Erlang. Instead of calling functions and spawning threads, we create independent actors that react to messages, much like
an actor on a stage waits for and reacts to their cues. Akka does not explicitly have support for Kotlin, but as Akka runs on the JVM, we can 
utilize Akka with Kotlin/JVM. Akka also makes it trivial to cluster nodes to build a reactive system where actors can run on any number of 
nodes, although this particular program does not implement clustering.

The `main` function in `Main.kt` creates our actor system with `ActorSystem.create`, then spawns an actor for the `Reporter` class. It then
loops through each of the hosts and ports, spawning up to 1024 simultaneous actors from the `Scanner`, class and sending each a `Scan` 
message containing the host and port that actor is to scan. These actors will execute in parallel, so the 1024 limit is used to throttle 
(via `CountDownLatch`) the number of open connections.

When each `Scanner` actor receives a `Scan` message, they send a message to Akka's TCP Manager actor to connect to their designated hosts 
and ports. If the connection is successful, the TCP Manager actor will send a `Tcp.Connected` message to the `Scanner` actor. The `Scanner` 
actor then sends a `Report` message to the `Reporter` actor, identifying the port as being `OPEN`. The `Scanner` actor will then send a 
series of payloads to the TCP Manager actor to send to the open port, and if any data is returned, the TCP Manager actor will send a 
`Tcp.Received` message to the `Scanner` actor, and the `Scanner` actor will then send a `Report` message to the `Reporter` actor, informing
it of the returned data. If the connection attempt is unsuccessful, the TCP Manager actor will send a `Tcp.CommandFailed` message to the 
`Scanner` actor, which we can then parse to determine if the port is closed (packet rejected) or filtered (packet dropped). The `Scanner`
actor will then inform the `Reporter` actor of the port's status via a `Report` message. Once a connection has been closed, the actor will 
kill itself, allowing us to spawn more actors until all ports have been scanned. In other words, the `Scanner` actors are basically Meeseeks,
dying immediately after accomplishing their one task.

Once all the ports have been scanned, we send a `GetReport` message to the `Reporter` actor to collect the status and banners from all the
ports we just scanned, and then print the results in an attractive tree format. If you're on Windows, you unfortunately will
not see the pretty colors :(

### Compiling

```mvn clean:clean kotlin:compile resources:resources jar:jar exec:java```

### Executing

```java -jar target/kotlin-akka-portscan-0.1.jar```

### Hacking

There's a lot you can do with this program! Make it accept command line arguments for hosts & ports, including CIDR notation and IP ranges, 
as well as port ranges. Implement Akka Cluster and make this a distributed scanner. Add UDP support. Rewrite everything until none of my
code is left!
