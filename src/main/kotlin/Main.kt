import actors.Reporter
import actors.Scanner
import akka.actor.ActorRef
import akka.actor.ActorSystem
import akka.actor.Props
import akka.event.Logging
import akka.pattern.Patterns.ask
import akka.util.Timeout
import enums.State
import messages.GetReport
import messages.Scan
import org.barfuin.texttree.api.DefaultNode
import org.barfuin.texttree.api.TextTree
import org.barfuin.texttree.api.TreeOptions
import org.barfuin.texttree.api.style.TreeStyles
import scala.concurrent.Await.result
import util.colored
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.system.exitProcess

lateinit var scansComplete : CountDownLatch
var maxScanners = 1024
var timeout = 5000L

object Main {
    @JvmStatic
    fun main(args: Array<String>) {
        val hosts = listOf(
            "scanme.nmap.org",
            "github.com"
        )

        val ports = listOf(
              21,   22,   23,   25,   53,   80,  110,  111,
             135,  139,  143,  161,  443,  445,  554,  993,
            1723, 2222, 3306, 3389, 5060, 5900, 8080, 8443
        )

        val system   = ActorSystem.create("portscan")
        val reporter = system.actorOf(Props.create(Reporter::class.java), "reporter")
        val log      = Logging.getLogger(system, this)

        println(
            colored { "\nScanning ${ports.count()} ports on $hosts\n".bright.cyan }
        )

        val totalPorts = hosts.count() * ports.count()

        if (totalPorts < maxScanners) {
            maxScanners = totalPorts
        }

        var scannerPool = CountDownLatch(maxScanners)
        scansComplete   = CountDownLatch(maxScanners)

        hosts.forEach { host ->
            ports.forEach { port ->
                log.debug("Scanning $host:$port")
                scannerPool.countDown()

                system.actorOf(Props.create(Scanner::class.java), "$host:$port")
                    .tell(Scan(host, port), ActorRef.noSender())

                if (scannerPool.count == 0L) {
                    scansComplete.await()
                    scannerPool   = CountDownLatch(maxScanners)
                    scansComplete = CountDownLatch(maxScanners)
                }
            }
        }

        printResults(reporter)

        exitProcess(0)
    }

    private fun printResults(reporter : ActorRef) {
        val duration = Timeout(timeout, TimeUnit.MILLISECONDS).duration()

        try {
            val question = ask(reporter, GetReport(), timeout)
            val response = result(question, duration) as HashMap<*, *>

            response.toSortedMap(
                compareBy { it as String }
            ).forEach { (__host, __results) ->
                val host    = __host    as String
                val results = __results as HashMap<*, *>

                val tree = DefaultNode(colored { host.bright.blue })

                results.toSortedMap(
                    compareBy { it as Int }
                ).forEach { (__port, __status) ->
                    val port   = __port   as Int
                    val status = __status as Pair<*, *>

                    val state  = status.first  as State
                    val banner = status.second as List<*>

                    val portText = when (state) {
                        State.OPEN   -> colored { "$port ($state)".bright.green }
                        State.CLOSED -> colored { "$port ($state)".bright.red   }
                        else         -> colored { "$port ($state)".bright.black }
                    }

                    val portNode = DefaultNode(portText)

                    banner.forEach { msg ->
                        portNode.addChild(
                            DefaultNode(colored { msg.yellow })
                        )
                    }

                    tree.addChild(portNode)
                }

                val options = TreeOptions()
                options.setStyle(TreeStyles.UNICODE_ROUNDED)

                println(
                    TextTree.newInstance(options).render(tree)
                )
            }
        } catch (e: Exception) {
            println("Error: $e")
        }
    }
}
