package hazard7.altv.kotlin.events

import hazard7.altv.jvm.CAPI
import hazard7.altv.kotlin.StringView
import hazard7.altv.kotlin.entities.Player
import hazard7.altv.kotlin.pointer
import jnr.ffi.Pointer

class PlayerDisconnectEvent internal constructor(ceventptr: Pointer) : Event(ceventptr) {
    val pointer = CAPI.func.alt_CEvent_to_alt_CPlayerDisconnectEvent(ceventptr)
    val player = run {
        val ref = CAPI.alt_RefBase_RefStore_IPlayer()
        CAPI.func.alt_CPlayerDisconnectEvent_GetTarget(pointer, ref.pointer)
        Player(ref.ptr.get())
    }
    val reason = StringView { CAPI.func.alt_CPlayerDisconnectEvent_GetReason(pointer, it) }
}
