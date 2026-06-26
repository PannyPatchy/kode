package com.example

fun main() {
    val greeter = Greeter()
    // A deliberately unused local to provoke a "never used" diagnostic.
    val unused = 42
    println(greeter.greet("world"))
}
