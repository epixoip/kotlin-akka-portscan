package actors

import akka.actor.ActorRef
import akka.actor.UntypedAbstractActor
import akka.event.Logging
import akka.io.Tcp
import akka.io.TcpMessage
import akka.util.ByteString
import enums.State
import messages.AddReport
import messages.Scan
import payloads
import scansComplete
import timeout
import java.net.InetSocketAddress
import java.time.Duration

class Scanner : UntypedAbstractActor() {
    private val log = Logging.getLogger(context.system, this)

    private lateinit var host : String
    private var port = 0

    override fun onReceive(msg: Any?) {
        log.debug("onReceive() received message of ${msg?.javaClass}")

        when(msg) {
            is Tcp.Connected -> connected()
            is Scan          -> scan(msg)
            else             -> closedOrFiltered(msg)
        }
    }

    private fun onConnected(connection: ActorRef) = receiveBuilder()
        .matchAny { msg ->
            log.debug("onConnected() received message of ${msg.javaClass}")

            when (msg) {
                is Tcp.Received -> received(msg)
                else            -> close()
            }
        }
        .build()

    private fun scan(msg: Scan) {
        host = msg.host
        port = msg.port

        Tcp.get(context.system)
            .manager()
            .tell(TcpMessage.connect(
                InetSocketAddress(host, port),
                null,
                listOf(),
                Duration.ofMillis(timeout - 500L),
                false
            ), self)
    }

    private fun connected() {
        val state = State.OPEN

        log.debug("$host:$port is $state")

        context.system.actorSelection("/user/reporter")
            .tell(AddReport(
                host  = host,
                port  = port,
                state = state
            ), self)

        sender.tell(TcpMessage.register(self), self)

        payloads.forEach { payload ->
            sender.tell(TcpMessage.write(
                ByteString.fromArray(payload
                    .replace("#host#", host)
                    .toByteArray()
                )
            ), self)
        }

        context.become(onConnected(sender))
    }

    private fun received(msg: Tcp.Received) {
        val banner = String(msg.data().toArray()).trim()

        log.debug("$host:$port replied with $banner")

        val report = AddReport(
            host  = host,
            port  = port,
            state = State.OPEN
        )

        report.banner = banner

        context.system.actorSelection("/user/reporter")
            .tell(report, self)
    }

    private fun closedOrFiltered(msg: Any?) {
        val state = if (msg is Tcp.CommandFailed) {
            val cause = msg.causedByString()

            when {
                cause.contains("Connect timeout")    -> State.FILTERED
                cause.contains("Connection refused") -> State.CLOSED
                else -> {
                    log.warning("Error: $host:$port: $cause")
                    State.ERROR
                }
            }
        } else {
            log.warning("Error: $host:$port: ${msg?.javaClass}")
            State.ERROR
        }

        log.debug("$host:$port is $state")

        context.system.actorSelection("/user/reporter")
            .tell(AddReport(
                host  = host,
                port  = port,
                state = state
            ), self)

        close()
    }

    private fun close() {
        sender.tell(TcpMessage.close(), self)
        scansComplete.countDown()
        context.stop(self)
    }
}
