// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_VARIABLE -UNUSED_ANONYMOUS_PARAMETER

class Sample

fun <K> id(x: K): K = x

fun test() {
//    val f0: Sample.() -> Unit = id { val a = 1 }
    val f1: Sample.() -> Unit = id { s: Sample -> }
    val f2: Sample.() -> Unit = id<Sample.() -> Unit> { <!EXPECTED_PARAMETERS_NUMBER_MISMATCH!>s: Sample<!> -> }
}
