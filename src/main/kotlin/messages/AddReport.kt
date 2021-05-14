package messages

import enums.State

data class AddReport (
    val host  : String,
    val port  : Int,
    var state : State
) {
    var banner : String? = null
}
