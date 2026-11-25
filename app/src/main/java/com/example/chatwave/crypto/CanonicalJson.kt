package com.example.chatwave.crypto

object CanonicalJson {
  fun canonicalize(value: Any?): String = when (value) {
    null -> "null"
    is String -> quote(value)
    is Boolean -> if (value) "true" else "false"
    is Byte, is Short, is Int, is Long -> value.toString()
    is Float -> finite(value.toDouble(), value.toString())
    is Double -> finite(value, value.toString())
    is Map<*, *> -> {
      val keys = value.keys.filterIsInstance<String>().sorted()
      keys.joinToString(",", "{", "}") { key -> "${quote(key)}:${canonicalize(value[key])}" }
    }
    is Iterable<*> -> value.joinToString(",", "[", "]") { canonicalize(it) }
    is Array<*> -> value.joinToString(",", "[", "]") { canonicalize(it) }
    else -> throw IllegalArgumentException("Unsupported canonical JSON value: ${value.javaClass.name}")
  }

  private fun finite(value: Double, text: String): String {
    require(!value.isNaN() && !value.isInfinite()) { "Non-finite number is not canonical" }
    return text
  }

  private fun quote(value: String): String {
    val output = StringBuilder(value.length + 2)
    output.append('"')
    for (char in value) {
      when (char) {
        '"' -> output.append("\\\"")
        '\\' -> output.append("\\\\")
        '\b' -> output.append("\\b")
        '\u000C' -> output.append("\\f")
        '\n' -> output.append("\\n")
        '\r' -> output.append("\\r")
        '\t' -> output.append("\\t")
        else -> if (char.code < 0x20) {
          output.append("\\u").append(char.code.toString(16).padStart(4, '0'))
        } else {
          output.append(char)
        }
      }
    }
    output.append('"')
    return output.toString()
  }
}
