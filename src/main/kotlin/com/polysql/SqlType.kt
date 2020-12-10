package com.polysql

import java.math.MathContext
import java.math.RoundingMode

/**
 * A representation of a value supported by [poly-sql](https://github.com/poly-sql).
 *
 * @param name the name unqualified name common to all instance of value which shares the same characteristics
 * @param signature the qualified type name with all parameters
 * @constructor
 * @author Ian Caffey
 * @since 1.0
 */
sealed class SqlType(val name: String, val signature: String) {
    override fun toString() = signature
}

/**
 * A [SqlType] which represent simple units of data.
 *
 * @author Ian Caffey
 * @since 1.0
 */
sealed class SqlScalar(name: String, signature: String = name) : SqlType(name, signature) {
    infix fun to(type: SqlType) = SqlMap(this, type)
}

/**
 * A [SqlType] capable of storing two possible values; `true` or `false`.
 *
 * @author Ian Caffey
 * @since 1.0
 */
object SqlBoolean : SqlScalar("boolean")

/**
 * A [SqlType] capable of storing a 8-bit signed integer with a minimum value of `-2^7` and maximum value of `2^7-1`.
 *
 * @author Ian Caffey
 * @since 1.0
 */
object SqlTinyInt : SqlScalar("tinyint")

/**
 * A [SqlType] capable of storing a 16-bit signed integer with a minimum value of `-2^15` and maximum value of `2^15-1`.
 *
 * @author Ian Caffey
 * @since 1.0
 */
object SqlSmallInt : SqlScalar("smallint")

/**
 * A [SqlType] capable of storing a 32-bit signed integer with a minimum value of `-2^31` and maximum value of `2^31-1`.
 *
 * @author Ian Caffey
 * @since 1.0
 */
object SqlInt : SqlScalar("int")

/**
 * A [SqlType] capable of storing a 64-bit signed integer with a minimum value of `-2^63` and maximum value of `2^63-1`.
 *
 * @author Ian Caffey
 * @since 1.0
 */
object SqlBigInt : SqlScalar("bigint")

/**
 * A [SqlType] capable of storing a 32-bit single-precision floating point number.
 *
 * @author Ian Caffey
 * @since 1.0
 */
object SqlFloat : SqlScalar("float")

/**
 * A [SqlType] capable of storing a 64-bit double-precision floating point number.
 *
 * @author Ian Caffey
 * @since 1.0
 */
object SqlDouble : SqlScalar("double")

/**
 * A [SqlType] capable of storing a fixed precision decimal number.
 *
 * @param precision the total number of digits
 * @param scale the number of digits in the fraction
 * @author Ian Caffey
 * @since 1.0
 */
data class SqlDecimal(val precision: Int, val scale: Int) : SqlScalar("decimal", "decimal($precision,$scale)") {
    init {
        require(precision <= 38) {
            "'$name' only supports precision up to 38 digits. [precision=$precision]"
        }
    }

    val mathContext = MathContext(precision, RoundingMode.HALF_UP)
    override fun toString() = signature
}

/**
 * A [SqlType] capable of storing a instant in time (with calendar day precision).
 *
 * @author Ian Caffey
 * @since 1.0
 */
object SqlDate : SqlScalar("date")

/**
 * A [SqlType] capable of storing an instant in time (with nanosecond precision).
 *
 * @author Ian Caffey
 * @since 1.0
 */
object SqlTimestamp : SqlScalar("timestamp")

/**
 * A [SqlType] capable of storing fixed length character data with a minimum length of `1` character and a maximum length of `255` characters.
 *
 * @author Ian Caffey
 * @since 1.0
 */
data class SqlChar(val length: Int) : SqlScalar("char", "char($length)") {
    init {
        require(length in 1..255) { "'$name' length must be between 1 and 255. [length=$length]" }
    }

    override fun toString() = super.toString()
}

/**
 * A [SqlType] capable of storing fixed length character data with a minimum length of `1` character and a maximum length of `65,535` characters.
 *
 * @author Ian Caffey
 * @since 1.0
 */
data class SqlVarchar(val length: Int) : SqlScalar("varchar", "varchar($length)") {
    init {
        require(length in 1..65_535) { "'$name' length must be between 1 and 65,535. [length=$length]" }
    }

    override fun toString() = super.toString()
}

/**
 * A [SqlType] capable of storing variable length character data.
 *
 * @author Ian Caffey
 * @since 1.0
 */
object SqlString : SqlScalar("string")

/**
 * A [SqlType] capable of storing a nullable value of any other [SqlType] (which is not also optional).
 *
 * @author Ian Caffey
 * @since 1.0
 */
data class SqlOptional(val value: SqlType) : SqlType("optional", "${value.signature}?") {
    init {
        require(value !is SqlOptional) { "$name does not support nested optional types." }
    }

    override fun toString() = super.toString()
}

/**
 * A [SqlType] capable of storing a variable-sized, ordered sequence of values from another [SqlType].
 *
 * @author Ian Caffey
 * @since 1.0
 */
data class SqlArray<T : SqlType>(val element: T) : SqlType("array", "$element[]") {
    override fun toString() = super.toString()
}

/**
 * A [SqlType] capable of storing a finite ordered sequence of values from another [SqlType].
 *
 * @author Ian Caffey
 * @since 1.0
 */
data class SqlTuple(val elements: List<SqlType>) : SqlType(
    "tuple", elements.joinToString(separator = ",", prefix = "(", postfix = ")") { it.signature }
) {
    constructor(vararg types: SqlType) : this(listOf(*types))

    override fun toString() = super.toString()
}

/**
 * A [SqlType] capable of storing a relationship between a set of [SqlScalar] to values of another [SqlType].
 *
 * @author Ian Caffey
 * @since 1.0
 */
data class SqlMap(val key: SqlScalar, val value: SqlType) : SqlType("map", "$key=>$value>") {
    override fun toString() = super.toString()
}

/**
 * A [SqlType] capable of storing a name-accessible group of [SqlType] values.
 *
 * @author Ian Caffey
 * @since 1.0
 */
data class SqlStruct(val fields: Map<String, SqlType>) : SqlType(
    "struct",
    fields.entries.joinToString(",", "{", "}") { "${it.key}:${it.value.signature}" }
) {
    constructor(vararg entries: Pair<String, SqlType>) : this(mapOf(*entries))

    override fun toString() = super.toString()
}

/**
 * A [SqlType] capable of storing different variants of [SqlType].
 *
 * @author Ian Caffey
 * @since 1.0
 */
data class SqlUnion(val types: Set<SqlType>) : SqlType(
    "union", types.joinToString("|") { it.signature }
) {
    constructor(vararg types: SqlType) : this(setOf(*types))

    override fun toString() = super.toString()
}