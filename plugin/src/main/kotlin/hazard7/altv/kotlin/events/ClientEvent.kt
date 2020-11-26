package hazard7.altv.kotlin.events

import hazard7.altv.jvm.CAPI
import hazard7.altv.kotlin.*
import hazard7.altv.kotlin.StringView
import hazard7.altv.kotlin.altview
import hazard7.altv.kotlin.entities.Player
import hazard7.altv.kotlin.pointer
import jnr.ffi.Pointer
import java.lang.Exception
import java.lang.reflect.MalformedParametersException
import java.lang.reflect.Method
import java.security.InvalidParameterException
import kotlin.coroutines.suspendCoroutine
import kotlin.reflect.KFunction
import kotlin.reflect.jvm.reflect

class ClientEvent internal constructor(ceventptr: Pointer) : Event(ceventptr) {
    val pointer = CAPI.func.alt_CEvent_to_alt_CClientScriptEvent(ceventptr)
    val name = StringView { CAPI.func.alt_CClientScriptEvent_GetName(pointer, it) }
    internal val player = run {
        val ref = CAPI.alt_RefBase_RefStore_IPlayer()
        CAPI.func.alt_CClientScriptEvent_GetTarget(pointer, ref.pointer)
        ref.ptr.get()
    }
    fun getPlayer(resource: Resource) = resource.getOrCreatePlayer(player)

    val args = CAPI.alt_Array_RefBase_RefStore_constIMValue(
        CAPI.func.alt_CClientScriptEvent_GetArgs(pointer)
    )
    val argsCount = args.size.get().toInt()

    inline fun <reified T> getArg(index: Int): T {
        if(index < 0 && index > argsCount-1)
            throw IndexOutOfBoundsException("Event had $argsCount arguments, index must be between 0 and ${argsCount-1}")

        val mvalueref = CAPI.func.alt_Array_RefBase_RefStore_constIMValue_Access_unsignedlonglong_1(args.pointer, index.toLong())
        val mval = CAPI.func.alt_RefBase_RefStore_constIMValue_Get(mvalueref)
        val type = CAPI.func.alt_IMValue_GetType(mval)

        when (type) {
            CAPI.alt_IMValue_Type.ALT_IMVALUE_TYPE_BOOL -> {
                when (T::class) {
                    Boolean::class ->
                        return CAPI.func.alt_IMValueBool_Value(CAPI.func.alt_IMValue_to_alt_IMValueBool(mval)) as T
                    Int::class ->
                        return (if (CAPI.func.alt_IMValueBool_Value(CAPI.func.alt_IMValue_to_alt_IMValueBool(mval))) 1 else 0) as T
                    String::class ->
                        return CAPI.func.alt_IMValueBool_Value(CAPI.func.alt_IMValue_to_alt_IMValueBool(mval)).toString() as T
                    else ->
                        throw ParamTypeMismatch(index,"Boolean")
                }
            }
            CAPI.alt_IMValue_Type.ALT_IMVALUE_TYPE_INT -> {
                when (T::class) {
                    Boolean::class ->
                        return (CAPI.func.alt_IMValueInt_Value(CAPI.func.alt_IMValue_to_alt_IMValueInt(mval)) == 1L) as T
                    Int::class ->
                        return CAPI.func.alt_IMValueInt_Value(CAPI.func.alt_IMValue_to_alt_IMValueInt(mval)).toInt() as T
                    UInt::class ->
                        return CAPI.func.alt_IMValueInt_Value(CAPI.func.alt_IMValue_to_alt_IMValueInt(mval)).toUInt() as T
                    Long::class ->
                        return CAPI.func.alt_IMValueInt_Value(CAPI.func.alt_IMValue_to_alt_IMValueInt(mval)).toLong() as T
                    ULong::class ->
                        return CAPI.func.alt_IMValueInt_Value(CAPI.func.alt_IMValue_to_alt_IMValueInt(mval)).toULong() as T
                    Float::class ->
                        return CAPI.func.alt_IMValueInt_Value(CAPI.func.alt_IMValue_to_alt_IMValueInt(mval)).toFloat() as T
                    Double::class ->
                        return CAPI.func.alt_IMValueInt_Value(CAPI.func.alt_IMValue_to_alt_IMValueInt(mval)).toDouble() as T
                    String::class ->
                        return CAPI.func.alt_IMValueInt_Value(CAPI.func.alt_IMValue_to_alt_IMValueInt(mval)).toString() as T
                    else -> {
                        throw ParamTypeMismatch(index,"Long")
                    }
                }
            }
            CAPI.alt_IMValue_Type.ALT_IMVALUE_TYPE_STRING -> {
                if (T::class != String::class)
                    throw ParamTypeMismatch(index, "String")
                return StringView { CAPI.func.alt_IMValueString_Value(CAPI.func.alt_IMValue_to_alt_IMValueString(mval), it) } as T
            }
            else -> {
                throw NotImplementedError("Unhandled MValue type ${type.name}")
            }
        }
    }

    class ParamTypeMismatch(paramNum: Int, eventType: String) :
        Exception("Event arg $paramNum was a $eventType")

//    internal fun getArgs(handler: Handler): Array<Any?> {
//        val args = CAPI.alt_Array_RefBase_RefStore_constIMValue(
//            CAPI.func.alt_CClientScriptEvent_GetArgs(pointer)
//        )
//        val eventArgsCount = args.size.get().toInt()
//        val params = handler.invoker!!.parameterTypes.copyOfRange(1, handler.invoker!!.parameterTypes.size)
//
//        if (handler.paramCount > eventArgsCount)
//            throw MalformedParametersException("Event '$name' with $eventArgsCount args was sent to ${handler.paramCount} params handler")
//        else if (handler.paramCount < eventArgsCount)
//            logWarning("Recieved event '$name' with $eventArgsCount args did not match handlers ${handler.paramCount} parameters")
//
//        val ret = Array<Any?>(eventArgsCount) { null }
//        for (i in 0 until eventArgsCount)
//        {
//            val mvalueref = CAPI.func.alt_Array_RefBase_RefStore_constIMValue_Access_unsignedlonglong_1(args.pointer, i.toLong())
//            val mval = CAPI.func.alt_RefBase_RefStore_constIMValue_Get(mvalueref)
//            val type = CAPI.func.alt_IMValue_GetType(mval)
//
//            ret[i] = when (type) {
//                CAPI.alt_IMValue_Type.ALT_IMVALUE_TYPE_BOOL -> {
//                    when (params[i]) {
//                        Boolean::class.java ->
//                            CAPI.func.alt_IMValueBool_Value(CAPI.func.alt_IMValue_to_alt_IMValueBool(mval))
//                        Int::class.java ->
//                            if (CAPI.func.alt_IMValueBool_Value(CAPI.func.alt_IMValue_to_alt_IMValueBool(mval))) 1 else 0
//                        String::class.java ->
//                            CAPI.func.alt_IMValueBool_Value(CAPI.func.alt_IMValue_to_alt_IMValueBool(mval)).toString()
//                        else ->
//                            throw ParamTypeMismatch(i,"Boolean", params[i].name)
//                    }
//                }
//                CAPI.alt_IMValue_Type.ALT_IMVALUE_TYPE_INT -> {
//                    when (params[i]) {
//                        Boolean::class.java ->
//                            CAPI.func.alt_IMValueInt_Value(CAPI.func.alt_IMValue_to_alt_IMValueInt(mval)) == 1L
//                        Int::class.java ->
//                            CAPI.func.alt_IMValueInt_Value(CAPI.func.alt_IMValue_to_alt_IMValueInt(mval)).toInt()
//                        UInt::class.java ->
//                            CAPI.func.alt_IMValueInt_Value(CAPI.func.alt_IMValue_to_alt_IMValueInt(mval)).toUInt()
//                        Long::class.java ->
//                            CAPI.func.alt_IMValueInt_Value(CAPI.func.alt_IMValue_to_alt_IMValueInt(mval)).toLong()
//                        ULong::class.java ->
//                            CAPI.func.alt_IMValueInt_Value(CAPI.func.alt_IMValue_to_alt_IMValueInt(mval)).toULong()
//                        Float::class.java ->
//                            CAPI.func.alt_IMValueInt_Value(CAPI.func.alt_IMValue_to_alt_IMValueInt(mval)).toFloat()
//                        Double::class.java ->
//                            CAPI.func.alt_IMValueInt_Value(CAPI.func.alt_IMValue_to_alt_IMValueInt(mval)).toDouble()
//                        String::class.java ->
//                            CAPI.func.alt_IMValueInt_Value(CAPI.func.alt_IMValue_to_alt_IMValueInt(mval)).toString()
//                        else -> {
//                            throw ParamTypeMismatch(i,"Long", params[i].name)
//                        }
//                    }
//                }
//                CAPI.alt_IMValue_Type.ALT_IMVALUE_TYPE_STRING -> {
//                    if (params[i] != String::class.java)
//                        throw ParamTypeMismatch(i, "String",params[i].name)
//                    StringView { CAPI.func.alt_IMValueString_Value(CAPI.func.alt_IMValue_to_alt_IMValueString(mval), it) }
//                }
//                else -> {
//                    throw NotImplementedError("Unhandled MValue type ${type.name}")
//                }
//            }
//        }
//        return ret
//    }

//    companion object {
//        fun send(name: String, vararg args: Any)
//        {
//            val emptyMValue = CAPI.alt_RefBase_RefStore_constIMValue()
//            emptyMValue.ptr.set(0)
//            val arr = CAPI.alt_Array_RefBase_RefStore_constIMValue(
//                CAPI.func.alt_Array_RefBase_RefStore_constIMValue_Create_CAPI_Heap()
//            )
//            CAPI.func.alt_Array_RefBase_RefStore_constIMValue_Reserve(arr.pointer, args.size.toLong())
//
//            fun getBaseRefMValue(create: () -> Pointer, cast: (Pointer) -> Pointer): Pointer
//            {
//                // TODO: PROFILE HEAP VS STACK METHODS
//
//                // Ref<MValueBool>
//                val refmvaluetype = create()
//                // MValueBool
//                val mvaluetype = CAPI.func.alt_RefBase_RefStore_constIMValue_Get(refmvaluetype)
//                // MValue
//                val mvalue = cast(mvaluetype)
//                // Ref<MValue>
//                val refmvalue = CAPI.func.alt_RefBase_RefStore_constIMValue_Create_4_CAPI_Heap(mvalue)
//                CAPI.func.alt_IMValue_RemoveRef(mvalue)
//                return refmvalue
//            }
//
//            for ((i, arg) in args.withIndex()) {
//                val refmvalue = when (arg) {
//                    is Boolean -> {
//                        getBaseRefMValue(
//                            { CAPI.func.alt_ICore_CreateMValueBool_CAPI_Heap(CAPI.core, arg) },
//                            { CAPI.func.alt_IMValueBool_to_alt_IMValue(it) }
//                        )
//                    }
//                    is Int -> {
//                        getBaseRefMValue(
//                            { CAPI.func.alt_ICore_CreateMValueInt_CAPI_Heap(CAPI.core, arg.toLong()) },
//                            { CAPI.func.alt_IMValueInt_to_alt_IMValue(it) }
//                        )
//                    }
//                    is String -> {
//                        getBaseRefMValue(
//                            { CAPI.func.alt_ICore_CreateMValueString_CAPI_Heap(CAPI.core, arg.altstring.pointer) },
//                            { CAPI.func.alt_IMValueString_to_alt_IMValue(it) }
//                        )
//                    }
//                    else -> {
//                        throw TypeCastException("Unsupported event arg type '${arg::class.java}', value: '$arg'")
//                    }
//                }
//
//                // refcount=1, ++ after push
//                CAPI.func.alt_Array_RefBase_RefStore_constIMValue_Push(arr.pointer, refmvalue)
//                // refcount=2, ok to capi free, will deref
//                CAPI.func.alt_RefBase_RefStore_constIMValue_CAPI_Free(refmvalue)
//                // refcount = 1
//            }
//
//            CAPI.func.alt_ICore_TriggerLocalEvent(CAPI.core, name.altview.ptr(), arr.pointer)
//            // refcounts=2
//            CAPI.func.alt_Array_RefBase_RefStore_constIMValue_CAPI_Free(arr.pointer)
//            // refcounts=1
//        }
//    }

//    internal class Handler {
//        var func: Function<*>? = null
//        var isSuspend = false
//        var invoker: Method? = null
//        val paramCount by lazy {
//            if(isSuspend) invoker!!.parameterCount-2
//            else invoker!!.parameterCount-1
//        }
//        constructor(func: KFunction<*>)
//        {
//            this.func = func
//            isSuspend = func.reflect()?.isSuspend!!
//            val method = func.javaClass.declaredMethods[1]
//            method.isAccessible = true
//            invoker = method
//
//            val params = invoker!!.parameterTypes
//            if(params.size < 1 || params[0] != Player::class.java) {
//                throw InvalidParameterException("First parameter must be a Player")
//            }
//        }
//        constructor(func: Function<*>)
//        {
//            this.func = func
//            val method = func.javaClass.declaredMethods[1] // seems to always be the second one (with param types)
//            method.isAccessible = true
//            invoker = method
//
//            val params = invoker!!.parameterTypes
//            if(params.size < 1 || params[0] != Player::class.java) {
//                throw InvalidParameterException("First parameter must be a Player")
//            }
//        }
//
//        suspend fun invoke(vararg args: Any?) {
//            if(isSuspend)
//            {
//                suspendCoroutine<Unit> {
//                    invoker?.invoke(func, *args, it)
//                }
//            } else {
//                invoker?.invoke(func, *args)
//            }
//        }
//    }
}