package com.polysql

import java.math.BigDecimal
import java.sql.Date
import java.sql.Timestamp

/**
 * A value supported by [poly-sql](https://github.com/poly-sql).
 *
 * @author Ian Caffey
 * @since 1.0
 */
interface SqlValue {
    /**
     * Returns whether this [SqlValue] is an instance of the [SqlType].
     */
    //Note: while type checking from SqlValue -> SqlType is more complex than the doing it from the SqlType -> SqlValue,
    //it avoids the boxing of inlined SqlValue classes that'd be required from a SqlType#isInstance(value: SqlValue) API
    fun instanceOf(type: SqlType): Boolean
}

/**
 * A value which can only take on a single [SqlType] in [poly-sql](https://github.com/poly-sql).
 *
 * @author Ian Caffey
 * @since 1.0
 */
interface SqlSingular : SqlValue {
    val type: SqlType
    override fun instanceOf(type: SqlType): Boolean = this.type.let { t ->
        t == type || (type is SqlUnionType && type.types.any { instanceOf(it) })
    }
}

/**
 * A `null` value [poly-sql](https://github.com/poly-sql).
 *
 * @author Ian Caffey
 * @since 1.0
 */
object SqlNull : SqlSingular {
    override val type: SqlType
        get() = SqlNullType

    override fun toString() = "null"
}

/**
 * A simple value in [poly-sql](https://github.com/poly-sql).
 *
 * @author Ian Caffey
 * @since 1.0
 */
interface SqlScalar : SqlValue

/**
 * A numeric value in [poly-sql](https://github.com/poly-sql).
 *
 * @author Ian Caffey
 * @since 1.0
 */
interface SqlNumber : SqlScalar {
    /**
     * Returns the value of this [SqlNumber] as a [SqlTinyInt], which may involve rounding or truncation.
     */
    fun toTinyInt(): SqlTinyInt

    /**
     * Returns the value of this [SqlNumber] as a [SqlSmallInt], which may involve rounding or truncation.
     */
    fun toSmallInt(): SqlSmallInt

    /**
     * Returns the value of this [SqlNumber] as a [SqlInt], which may involve rounding or truncation.
     */
    fun toInt(): SqlInt

    /**
     * Returns the value of this [SqlNumber] as a [SqlBigInt], which may involve rounding or truncation.
     */
    fun toBigInt(): SqlBigInt

    /**
     * Returns the value of this [SqlNumber] as a [SqlFloat], which may involve rounding.
     */
    fun toFloat(): SqlFloat

    /**
     * Returns the value of this [SqlNumber] as a [SqlDouble], which may involve rounding.
     */
    fun toDouble(): SqlDouble

    /**
     * Returns the value of this [SqlNumber] as a [SqlDecimal].
     */
    fun toDecimal(): SqlDecimal
}

/**
 * A boolean value in [poly-sql](https://github.com/poly-sql).
 *
 * @author Ian Caffey
 * @since 1.0
 */
//Note: an inline class wouldn't get us really any benefit here and would cause unnecessary boxing in certain places
enum class SqlBoolean(val value: Boolean) : SqlScalar, SqlSingular {
    TRUE(true), FALSE(false);

    override val type: SqlType
        get() = SqlBooleanType

    infix fun and(other: SqlBoolean) = Companion(value and other.value)
    infix fun or(other: SqlBoolean) = Companion(value or other.value)
    infix fun xor(other: SqlBoolean) = Companion(value xor other.value)
    operator fun not(): SqlBoolean = Companion(!value)

    companion object {
        operator fun invoke(value: Boolean) = if (value) TRUE else FALSE
    }
}

/**
 * A 8-bit signed integer with a minimum value of `-2^7` and maximum value of `2^7-1` in [poly-sql](https://github.com/poly-sql).
 *
 * @author Ian Caffey
 * @since 1.0
 */
inline class SqlTinyInt(val value: Byte) : SqlNumber, SqlSingular, Comparable<SqlTinyInt> {
    override val type: SqlType
        get() = SqlTinyIntType

    override fun toTinyInt() = this
    override fun toSmallInt() = SqlSmallInt(value.toShort())
    override fun toInt() = SqlInt(value.toInt())
    override fun toBigInt() = SqlBigInt(value.toLong())
    override fun toFloat() = SqlFloat(value.toFloat())
    override fun toDouble() = SqlDouble(value.toDouble())
    override fun toDecimal() = SqlDecimal(BigDecimal.valueOf(value.toLong()))
    override fun compareTo(other: SqlTinyInt) = value.compareTo(other.value)
}

/**
 * A 16-bit signed integer with a minimum value of `-2^15` and maximum value of `2^15-1` in [poly-sql](https://github.com/poly-sql).
 *
 * @author Ian Caffey
 * @since 1.0
 */
inline class SqlSmallInt(val value: Short) : SqlNumber, SqlSingular, Comparable<SqlSmallInt> {
    override val type: SqlType
        get() = SqlSmallIntType

    override fun toTinyInt() = SqlTinyInt(value.toByte())
    override fun toSmallInt() = this
    override fun toInt() = SqlInt(value.toInt())
    override fun toBigInt() = SqlBigInt(value.toLong())
    override fun toFloat() = SqlFloat(value.toFloat())
    override fun toDouble() = SqlDouble(value.toDouble())
    override fun toDecimal() = SqlDecimal(BigDecimal.valueOf(value.toLong()))
    override fun compareTo(other: SqlSmallInt) = value.compareTo(other.value)
}

/**
 * A 32-bit signed integer with a minimum value of `-2^31` and maximum value of `2^31-1` in [poly-sql](https://github.com/poly-sql).
 *
 * @author Ian Caffey
 * @since 1.0
 */
inline class SqlInt(val value: Int) : SqlNumber, SqlSingular, Comparable<SqlInt> {
    override val type: SqlType
        get() = SqlIntType

    override fun toTinyInt() = SqlTinyInt(value.toByte())
    override fun toSmallInt() = SqlSmallInt(value.toShort())
    override fun toInt() = this
    override fun toBigInt() = SqlBigInt(value.toLong())
    override fun toFloat() = SqlFloat(value.toFloat())
    override fun toDouble() = SqlDouble(value.toDouble())
    override fun toDecimal() = SqlDecimal(BigDecimal.valueOf(value.toLong()))
    override fun compareTo(other: SqlInt) = value.compareTo(other.value)
}

/**
 * A 64-bit signed integer with a minimum value of `-2^63` and maximum value of `2^63-1` in [poly-sql](https://github.com/poly-sql).
 *
 * @author Ian Caffey
 * @since 1.0
 */
inline class SqlBigInt(val value: Long) : SqlNumber, SqlSingular, Comparable<SqlBigInt> {
    override val type: SqlType
        get() = SqlBigIntType

    override fun toTinyInt() = SqlTinyInt(value.toByte())
    override fun toSmallInt() = SqlSmallInt(value.toShort())
    override fun toInt() = SqlInt(value.toInt())
    override fun toBigInt() = this
    override fun toFloat() = SqlFloat(value.toFloat())
    override fun toDouble() = SqlDouble(value.toDouble())
    override fun toDecimal() = SqlDecimal(BigDecimal.valueOf(value))
    override fun compareTo(other: SqlBigInt) = value.compareTo(other.value)
}

/**
 * A 32-bit single-precision floating-point number in [poly-sql](https://github.com/poly-sql).
 *
 * @author Ian Caffey
 * @since 1.0
 */
inline class SqlFloat(val value: Float) : SqlNumber, SqlSingular, Comparable<SqlFloat> {
    override val type: SqlType
        get() = SqlFloatType

    override fun toTinyInt() = SqlTinyInt(value.toInt().toByte())
    override fun toSmallInt() = SqlSmallInt(value.toInt().toShort())
    override fun toInt() = SqlInt(value.toInt())
    override fun toBigInt() = SqlBigInt(value.toLong())
    override fun toFloat() = this
    override fun toDouble() = SqlDouble(value.toDouble())
    override fun toDecimal() = SqlDecimal(BigDecimal.valueOf(value.toLong()))
    override fun compareTo(other: SqlFloat) = value.compareTo(other.value)
}

/**
 * A 64-bit double-precision floating-point number in [poly-sql](https://github.com/poly-sql).
 *
 * @author Ian Caffey
 * @since 1.0
 */
inline class SqlDouble(val value: Double) : SqlNumber, SqlSingular, Comparable<SqlDouble> {
    override val type: SqlType
        get() = SqlDoubleType

    override fun toTinyInt() = SqlTinyInt(value.toInt().toByte())
    override fun toSmallInt() = SqlSmallInt(value.toInt().toShort())
    override fun toInt() = SqlInt(value.toInt())
    override fun toBigInt() = SqlBigInt(value.toLong())
    override fun toFloat() = SqlFloat(value.toFloat())
    override fun toDouble() = this
    override fun toDecimal() = SqlDecimal(BigDecimal.valueOf(value.toLong()))
    override fun compareTo(other: SqlDouble) = value.compareTo(other.value)
}

/**
 * A fixed-precision decimal number in [poly-sql](https://github.com/poly-sql).
 *
 * @author Ian Caffey
 * @since 1.0
 */
inline class SqlDecimal(val value: BigDecimal) : SqlNumber, Comparable<SqlDecimal> {
    override fun toTinyInt() = SqlTinyInt(value.toByte())
    override fun toSmallInt() = SqlSmallInt(value.toShort())
    override fun toInt() = SqlInt(value.toInt())
    override fun toBigInt() = SqlBigInt(value.toLong())
    override fun toFloat() = SqlFloat(value.toFloat())
    override fun toDouble() = SqlDouble(value.toDouble())
    override fun toDecimal() = this
    override fun compareTo(other: SqlDecimal) = value.compareTo(other.value)
    override fun instanceOf(type: SqlType): Boolean =
        (type is SqlDecimalType && value.precision() <= type.precision && value.scale() <= type.scale)
                || (type is SqlUnionType && type.types.any { instanceOf(it) })
}

/**
 * An instant in time (with calendar day precision) in [poly-sql](https://github.com/poly-sql).
 *
 * @author Ian Caffey
 * @since 1.0
 */
inline class SqlDate(val value: Date) : SqlScalar, SqlSingular, Comparable<SqlDate> {
    override val type: SqlType
        get() = SqlDateType

    override fun compareTo(other: SqlDate) = value.compareTo(other.value)
}

/**
 * An instant in time (with nanosecond precision) in [poly-sql](https://github.com/poly-sql).
 *
 * @author Ian Caffey
 * @since 1.0
 */
inline class SqlTimestamp(val value: Timestamp) : SqlScalar, SqlSingular, Comparable<SqlTimestamp> {
    override val type: SqlType
        get() = SqlTimestampType

    override fun compareTo(other: SqlTimestamp) = value.compareTo(other.value)
}

/**
 * A fixed-length sequence of characters with a minimum length of `1` character and a maximum length of `255` characters in [poly-sql](https://github.com/poly-sql).
 *
 * @author Ian Caffey
 * @since 1.0
 */
data class SqlChar(val value: String) : SqlScalar, CharSequence, Comparable<SqlChar> {
    override val length: Int
        get() = value.length

    override fun get(index: Int) = value[index]
    override fun subSequence(startIndex: Int, endIndex: Int) = SqlChar(value.substring(startIndex, endIndex))
    override fun compareTo(other: SqlChar) = value.compareTo(other.value)
    override fun instanceOf(type: SqlType): Boolean =
        type is SqlCharType || (type is SqlUnionType && type.types.any { instanceOf(it) })

    override fun toString() = value
}

/**
 * A fixed-length sequence of characters with a minimum length of `1` character and a maximum length of `65,535` characters in [poly-sql](https://github.com/poly-sql).
 *
 * @author Ian Caffey
 * @since 1.0
 */
data class SqlVarchar(val value: String) : SqlScalar, CharSequence, Comparable<SqlVarchar> {
    override val length: Int
        get() = value.length

    override fun get(index: Int) = value[index]
    override fun subSequence(startIndex: Int, endIndex: Int) = SqlVarchar(value.substring(startIndex, endIndex))
    override fun compareTo(other: SqlVarchar) = value.compareTo(other.value)
    override fun instanceOf(type: SqlType): Boolean =
        type is SqlVarcharType || (type is SqlUnionType && type.types.any { instanceOf(it) })

    override fun toString() = value
}

/**
 * A variable-length sequence of characters in [poly-sql](https://github.com/poly-sql).
 *
 * @author Ian Caffey
 * @since 1.0
 */
data class SqlString(val value: String) : SqlScalar, SqlSingular, CharSequence, Comparable<SqlString> {
    override val length: Int
        get() = value.length
    override val type: SqlType
        get() = SqlStringType

    override fun get(index: Int) = value[index]
    override fun subSequence(startIndex: Int, endIndex: Int) = SqlString(value.substring(startIndex, endIndex))
    override fun compareTo(other: SqlString) = value.compareTo(other.value)
    override fun toString() = value
}

/**
 * A variable-length, ordered sequence of [SqlValue] in [poly-sql](https://github.com/poly-sql).
 *
 * @author Ian Caffey
 * @since 1.0
 */
data class SqlArray<V : SqlValue>(private val values: List<V>) : SqlValue, List<V> by values {
    constructor(vararg values: V) : this(values.toList())

    override fun instanceOf(type: SqlType): Boolean =
        (type is SqlArrayType<*> && values.all { it.instanceOf(type.elementType) })
                || (type is SqlUnionType && type.types.any { instanceOf(it) })
}

/**
 * A variable-length, ordered sequence of [SqlBoolean] in [poly-sql](https://github.com/poly-sql).
 *
 * @author Ian Caffey
 * @since 1.0
 */
data class SqlBooleanArray(private val values: BooleanArray) : SqlSingular, Iterable<SqlBoolean> {
    constructor(size: Int, init: (Int) -> SqlBoolean) : this(BooleanArray(size) { init(it).value })

    val size: Int
        get() = values.size
    override val type: SqlType
        get() = SqlBooleanArrayType

    operator fun get(index: Int) = SqlBoolean(values[index])
    operator fun set(index: Int, value: SqlBoolean) {
        values[index] = value.value
    }

    override fun iterator() = values.asSequence().map { SqlBoolean(it) }.iterator()
    override fun equals(other: Any?) =
        this === other || (other is SqlBooleanArray && values.contentEquals(other.values))

    override fun hashCode() = values.contentHashCode()
}

/**
 * A variable-length, ordered sequence of [SqlTinyInt] in [poly-sql](https://github.com/poly-sql).
 *
 * @author Ian Caffey
 * @since 1.0
 */
data class SqlTinyIntArray(private val values: ByteArray) : SqlSingular, Iterable<SqlTinyInt> {
    constructor(size: Int, init: (Int) -> SqlTinyInt) : this(ByteArray(size) { init(it).value })

    val size: Int
        get() = values.size
    override val type: SqlType
        get() = SqlTinyIntArrayType

    operator fun get(index: Int) = SqlTinyInt(values[index])
    operator fun set(index: Int, value: SqlTinyInt) {
        values[index] = value.value
    }

    override fun iterator() = values.asSequence().map { SqlTinyInt(it) }.iterator()
    override fun equals(other: Any?) =
        this === other || (other is SqlTinyIntArray && values.contentEquals(other.values))

    override fun hashCode() = values.contentHashCode()
}

/**
 * A variable-length, ordered sequence of [SqlSmallInt] in [poly-sql](https://github.com/poly-sql).
 *
 * @author Ian Caffey
 * @since 1.0
 */
data class SqlSmallIntArray(private val values: ShortArray) : SqlSingular, Iterable<SqlSmallInt> {
    constructor(size: Int, init: (Int) -> SqlSmallInt) : this(ShortArray(size) { init(it).value })

    val size: Int
        get() = values.size
    override val type: SqlType
        get() = SqlSmallIntArrayType

    operator fun get(index: Int) = SqlSmallInt(values[index])
    operator fun set(index: Int, value: SqlSmallInt) {
        values[index] = value.value
    }

    override fun iterator() = values.asSequence().map { SqlSmallInt(it) }.iterator()
    override fun equals(other: Any?) =
        this === other || (other is SqlSmallIntArray && values.contentEquals(other.values))

    override fun hashCode() = values.contentHashCode()
}

/**
 * A variable-length, ordered sequence of [SqlInt] in [poly-sql](https://github.com/poly-sql).
 *
 * @author Ian Caffey
 * @since 1.0
 */
data class SqlIntArray(private val values: IntArray) : SqlSingular, Iterable<SqlInt> {
    constructor(size: Int, init: (Int) -> SqlInt) : this(IntArray(size) { init(it).value })

    val size: Int
        get() = values.size
    override val type: SqlType
        get() = SqlIntArrayType

    operator fun get(index: Int) = SqlInt(values[index])
    operator fun set(index: Int, value: SqlInt) {
        values[index] = value.value
    }

    override fun iterator() = values.asSequence().map { SqlInt(it) }.iterator()
    override fun equals(other: Any?) = this === other || (other is SqlIntArray && values.contentEquals(other.values))
    override fun hashCode() = values.contentHashCode()
}

/**
 * A variable-length, ordered sequence of [SqlBigInt] in [poly-sql](https://github.com/poly-sql).
 *
 * @author Ian Caffey
 * @since 1.0
 */
data class SqlBigIntArray(private val values: LongArray) : SqlSingular, Iterable<SqlBigInt> {
    constructor(size: Int, init: (Int) -> SqlBigInt) : this(LongArray(size) { init(it).value })

    val size: Int
        get() = values.size
    override val type: SqlType
        get() = SqlBigIntArrayType

    operator fun get(index: Int) = SqlBigInt(values[index])
    operator fun set(index: Int, value: SqlBigInt) {
        values[index] = value.value
    }

    override fun iterator() = values.asSequence().map { SqlBigInt(it) }.iterator()
    override fun equals(other: Any?) = this === other || (other is SqlBigIntArray && values.contentEquals(other.values))
    override fun hashCode() = values.contentHashCode()
}

/**
 * A variable-length, ordered sequence of [SqlFloat] in [poly-sql](https://github.com/poly-sql).
 *
 * @author Ian Caffey
 * @since 1.0
 */
data class SqlFloatArray(private val values: FloatArray) : SqlSingular, Iterable<SqlFloat> {
    constructor(size: Int, init: (Int) -> SqlFloat) : this(FloatArray(size) { init(it).value })

    val size: Int
        get() = values.size
    override val type: SqlType
        get() = SqlFloatArrayType

    operator fun get(index: Int) = SqlFloat(values[index])
    operator fun set(index: Int, value: SqlFloat) {
        values[index] = value.value
    }

    override fun iterator() = values.asSequence().map { SqlFloat(it) }.iterator()
    override fun equals(other: Any?) = this === other || (other is SqlFloatArray && values.contentEquals(other.values))
    override fun hashCode() = values.contentHashCode()
}

/**
 * A variable-length, ordered sequence of [SqlDouble] in [poly-sql](https://github.com/poly-sql).
 *
 * @author Ian Caffey
 * @since 1.0
 */
data class SqlDoubleArray(private val values: DoubleArray) : SqlSingular, Iterable<SqlDouble> {
    constructor(size: Int, init: (Int) -> SqlDouble) : this(DoubleArray(size) { init(it).value })

    val size: Int
        get() = values.size
    override val type: SqlType
        get() = SqlDoubleArrayType

    operator fun get(index: Int) = SqlDouble(values[index])
    operator fun set(index: Int, value: SqlDouble) {
        values[index] = value.value
    }

    override fun iterator() = values.asSequence().map { SqlDouble(it) }.iterator()
    override fun equals(other: Any?) = this === other || (other is SqlDoubleArray && values.contentEquals(other.values))
    override fun hashCode() = values.contentHashCode()
}

/**
 * A fixed-length, ordered sequence of [SqlValue] in [poly-sql](https://github.com/poly-sql).
 *
 * @author Ian Caffey
 * @since 1.0
 */
data class SqlTuple(private val values: List<SqlValue>) : SqlValue, List<SqlValue> by values {
    constructor(vararg values: SqlValue) : this(values.toList())

    override fun instanceOf(type: SqlType): Boolean =
        (type is SqlTupleType && type.elements.size == values.size
                && values.indices.all { values[it].instanceOf(type.elements[it]) })
                || (type is SqlUnionType && type.types.any { instanceOf(it) })
}

/**
 * A relationship between a set of [SqlScalar] to [SqlValue] in [poly-sql](https://github.com/poly-sql).
 *
 * @author Ian Caffey
 * @since 1.0
 */
data class SqlMap(private val map: Map<SqlScalar, SqlValue>) : SqlValue, Map<SqlScalar, SqlValue> by map {
    constructor(vararg values: Pair<SqlScalar, SqlValue>) : this(values.toMap())

    override fun get(key: SqlScalar) = map[key] ?: SqlNull
    override fun instanceOf(type: SqlType): Boolean =
        (type is SqlMapType && map.keys.all { it.instanceOf(type.keyType) }
                && map.values.all { it.instanceOf(type.valueType) })
                || (type is SqlUnionType && type.types.any { instanceOf(it) })
}

/**
 * A name-accessible structured group of [SqlValue] in [poly-sql](https://github.com/poly-sql).
 *
 * @author Ian Caffey
 * @since 1.0
 */
data class SqlStruct(private val fields: Map<String, SqlValue>) : SqlValue, Map<String, SqlValue> by fields {
    constructor(vararg values: Pair<String, SqlValue>) : this(values.toMap())

    override fun get(key: String) = fields[key] ?: SqlNull
    override fun instanceOf(type: SqlType): Boolean =
        (type is SqlStructType && type.fields.all { (name, type) -> this[name].instanceOf(type) })
                || (type is SqlUnionType && type.types.any { instanceOf(it) })
}