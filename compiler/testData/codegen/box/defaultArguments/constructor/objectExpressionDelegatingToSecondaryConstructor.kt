// IGNORE_BACKEND_FIR: JVM_IR

abstract class A(val value: String) {
    constructor(k: Char = 'K') : this("O$k")
}

object B: A() {}

fun box(): String {
    val o = object : A() {}

    return o.value
}