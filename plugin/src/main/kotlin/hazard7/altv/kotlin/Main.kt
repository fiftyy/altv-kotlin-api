package hazard7.altv.kotlin

import hazard7.altv.jvm.AltStringView
import hazard7.altv.jvm.CAPI
import hazard7.altv.jvm.CAPIExtra
import hazard7.altv.kotlin.events.Event

fun main()
{
    logInfo("[Kotlin-JVM] Kotlin-JVM plugin loaded")

//    Thread.UncaughtExceptionHandler { t, e ->
//        Log.exception(e, "[Kotlin-JVM] Unhandled exception thrown")
//    }

    val script_runtime = CAPIExtra.func.alt_CAPIScriptRuntime_Create(
            create_resource,
            remove_resource,
            on_tick
    )
    CAPI.func.alt_ICore_RegisterScriptRuntime(
            CAPI.core,
            AltStringView("kotlin-jvm").ptr(),
            script_runtime
    )
    CAPI.func.alt_ICore_SubscribeEvent(CAPI.core, CAPI.alt_CEvent_Type.ALT_CEVENT_TYPE_ALL, Event.handler, null)

    logInfo("[Kotlin-JVM] Registered runtime for 'kotlin-jvm' resource type")
}

var create_resource = CAPIExtra.CreateImplFn { runtime, resource ->
    val kotlinResource = Resource(resource)

    CAPIExtra.func.alt_CAPIResource_Impl_Create(
            resource,
            kotlinResource.on_make_client,
            kotlinResource.on_start,
            kotlinResource.on_stop,
            kotlinResource.on_event,
            kotlinResource.on_tick,
            kotlinResource.on_create_base_object,
            kotlinResource.on_remove_base_object
    )
}

var remove_resource = CAPIExtra.DestroyImplFn { runtime, resource ->
    logInfo("[Kotlin-JVM] KOTLIN REMOVE RESOURCE")
}

var on_tick = CAPIExtra.OnRuntimeTickFn { runtime ->

}

