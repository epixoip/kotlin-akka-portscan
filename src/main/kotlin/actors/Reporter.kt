package actors

import akka.actor.UntypedAbstractActor
import enums.State
import messages.AddReport
import messages.GetReport

class Reporter : UntypedAbstractActor() {
    private val results = HashMap<String, HashMap<Int, Pair<State, MutableList<String>>>>()

    override fun onReceive(message: Any?) = when (message) {
        is AddReport -> addReport(message)
        is GetReport -> getReport()
        else         -> {}
    }

    private fun addReport(message: AddReport) {
        if (results[message.host] == null) {
            results[message.host] = if (message.banner == null) {
                hashMapOf(message.port to Pair(message.state, mutableListOf()))
            } else {
                hashMapOf(message.port to Pair(message.state, mutableListOf(message.banner!!)))
            }
        } else {
            val state = results[message.host]!![message.port]

            if (state == null) {
                results[message.host]!![message.port] = if (message.banner == null) {
                    Pair(message.state, mutableListOf())
                } else {
                    Pair(message.state, mutableListOf(message.banner!!))
                }
            } else {
                val banners = state.second

                if (message.banner != null) {
                    banners.add(message.banner!!)
                }

                results[message.host]!![message.port] = Pair(message.state, banners)
            }
        }
    }

    private fun getReport() {
        sender.tell(results, self)
    }
}
