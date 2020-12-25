package com.polysql

import java.math.MathContext
import java.math.RoundingMode
import java.util.*

/**
 * A representation of a value supported by [poly-sql](https://github.com/poly-sql).
 *
 * @param name the name unqualified name common to all instance of value which shares the same characteristics
 * @param signature the qualified type name with all parameters
 * @author Ian Caffey
 * @since 1.0
 */
sealed class SqlType {
    abstract val name: String
    abstract val default: SqlValue?
    abstract fun isSubtypeOf(type: SqlType): Boolean
    fun isSupertypeOf(type: SqlType) = type.isSubtypeOf(this)
    infix fun or(other: SqlType): SqlUnionType = SqlUnionType(this, other)
    override fun toString() = name
}

/**
 * A [SqlType] which represent simple units of data.
 *
 * @author Ian Caffey
 * @since 1.0
 */
sealed class SqlScalarType(override val name: String) : SqlType()

/**
 * A [SqlType] which represent numerical data.
 *
 * @author Ian Caffey
 * @since 1.0
 */
sealed class SqlNumberType(name: String) : SqlScalarType(name)

/**
 * A [SqlType] which can contain nested references to itself.
 *
 * @author Ian Caffey
 * @since 1.0
 */
sealed class SqlRecursiveType : SqlType() {
    /**
     * Determines if the [enclosingType] contains an invalid reference to `this` which will cause an infinite loop.
     *
     * @return `true` if the enclosing type contains a cyclic reference to `this`, otherwise `false`
     */
    @Suppress("SENSELESS_COMPARISON")
    protected fun cyclesIn(enclosingType: SqlType, visited: MutableSet<SqlType> = mutableSetOf()): Boolean =
        if (visited.contains(enclosingType)) false else visited.add(enclosingType).let {
            when (enclosingType) {
                this -> true
                is SqlTupleType -> enclosingType.elements.any { cyclesIn(it, visited) }
                //Note: nested references to the enclosing types will not have their fields initialized yet, so these null
                //checks are mandatory to ignore these cases (as they will be caught when checking for cycles from the parent)
                is SqlStructType -> enclosingType.fields != null && enclosingType.fields.values.any {
                    cyclesIn(it, visited)
                }
                is SqlUnionType -> enclosingType.types != null && enclosingType.types.all { cyclesIn(it, visited) }
                else -> false
            }
        }

    /**
     * Determines if the [enclosingType] contains a reference to `this`.
     *
     * @return `true` if the enclosing type contains a reference to `this`, otherwise `false`
     */
    @Suppress("SENSELESS_COMPARISON")
    protected fun referencedIn(enclosingType: SqlType, visited: MutableSet<SqlType> = mutableSetOf()): Boolean =
        if (visited.contains(enclosingType)) false else visited.add(enclosingType).let {
            when (enclosingType) {
                this -> true
                is SqlArrayType<*> -> referencedIn(enclosingType.elementType, visited)
                is SqlTupleType -> enclosingType.elements.any { referencedIn(it, visited) }
                is SqlMapType -> referencedIn(enclosingType.keyType, visited) || referencedIn(
                    enclosingType.valueType,
                    visited
                )
                //Note: nested references to the enclosing types will not have their fields initialized yet, so these null
                //checks are mandatory to ignore these cases (as they will be caught when checking for cycles from the parent)
                is SqlStructType -> enclosingType.fields != null && enclosingType.fields.values.any {
                    referencedIn(it, visited)
                }
                is SqlUnionType -> enclosingType.types != null && enclosingType.types.any { referencedIn(it, visited) }
                else -> false
            }
        }
}

object SqlAnyType : SqlType() {
    override val name = "Any"
    override val default: SqlValue? = null
    override fun isSubtypeOf(type: SqlType): Boolean =
        equals(type) || (type is SqlUnionType && type.types.any { isSubtypeOf(it) })
}

/**
 * A [SqlType] capable of storing a null value.
 *
 * @author Ian Caffey
 * @since 1.0
 */
object SqlNullType : SqlScalarType("Null") {
    override val default = SqlNull
    override fun isSubtypeOf(type: SqlType): Boolean =
        equals(type) || (type is SqlUnionType && type.types.any { isSubtypeOf(it) })
}

/**
 * A [SqlType] capable of storing two possible values; `true` or `false`.
 *
 * @author Ian Caffey
 * @since 1.0
 */
object SqlBooleanType : SqlScalarType("Boolean") {
    override val default = SqlBoolean.FALSE
    override fun isSubtypeOf(type: SqlType): Boolean =
        equals(type) || type is SqlAnyType || (type is SqlUnionType && type.types.any { isSubtypeOf(it) })
}

/**
 * A [SqlType] capable of storing a 8-bit signed integer with a minimum value of `-2^7` and maximum value of `2^7-1`.
 *
 * @author Ian Caffey
 * @since 1.0
 */
object SqlTinyIntType : SqlNumberType("TinyInt") {
    override val default = SqlTinyInt.ZERO
    override fun isSubtypeOf(type: SqlType): Boolean =
        equals(type) || type is SqlAnyType || (type is SqlUnionType && type.types.any { isSubtypeOf(it) })
}

/**
 * A [SqlType] capable of storing a 16-bit signed integer with a minimum value of `-2^15` and maximum value of `2^15-1`.
 *
 * @author Ian Caffey
 * @since 1.0
 */
object SqlSmallIntType : SqlNumberType("SmallInt") {
    override val default = SqlSmallInt.ZERO
    override fun isSubtypeOf(type: SqlType): Boolean =
        equals(type) || type is SqlAnyType || (type is SqlUnionType && type.types.any { isSubtypeOf(it) })
}

/**
 * A [SqlType] capable of storing a 32-bit signed integer with a minimum value of `-2^31` and maximum value of `2^31-1`.
 *
 * @author Ian Caffey
 * @since 1.0
 */
object SqlIntType : SqlNumberType("Int") {
    override val default = SqlInt.ZERO
    override fun isSubtypeOf(type: SqlType): Boolean =
        equals(type) || type is SqlAnyType || (type is SqlUnionType && type.types.any { isSubtypeOf(it) })
}

/**
 * A [SqlType] capable of storing a 64-bit signed integer with a minimum value of `-2^63` and maximum value of `2^63-1`.
 *
 * @author Ian Caffey
 * @since 1.0
 */
object SqlBigIntType : SqlNumberType("BigInt") {
    override val default = SqlBigInt.ZERO
    override fun isSubtypeOf(type: SqlType): Boolean =
        equals(type) || type is SqlAnyType || (type is SqlUnionType && type.types.any { isSubtypeOf(it) })
}

/**
 * A [SqlType] capable of storing a 32-bit single-precision floating-point number.
 *
 * @author Ian Caffey
 * @since 1.0
 */
object SqlFloatType : SqlNumberType("Float") {
    override val default = SqlFloat.ZERO
    override fun isSubtypeOf(type: SqlType): Boolean =
        equals(type) || type is SqlAnyType || (type is SqlUnionType && type.types.any { isSubtypeOf(it) })
}

/**
 * A [SqlType] capable of storing a 64-bit double-precision floating-point number.
 *
 * @author Ian Caffey
 * @since 1.0
 */
object SqlDoubleType : SqlNumberType("Double") {
    override val default = SqlDouble.ZERO
    override fun isSubtypeOf(type: SqlType): Boolean =
        equals(type) || type is SqlAnyType || (type is SqlUnionType && type.types.any { isSubtypeOf(it) })
}

/**
 * A [SqlType] capable of storing a fixed precision decimal number.
 *
 * @param precision the total number of digits
 * @param scale the number of digits in the fraction
 * @author Ian Caffey
 * @since 1.0
 */
data class SqlDecimalType(val precision: Int, val scale: Int) : SqlNumberType("Decimal($precision,$scale)") {
    init {
        require(precision <= 38) {
            "'$name' only supports precision up to 38 digits. [precision=$precision]"
        }
    }

    val mathContext = MathContext(precision, RoundingMode.HALF_UP)
    override val default = SqlDecimal.ZERO
    override fun isSubtypeOf(type: SqlType): Boolean =
        equals(type) || type is SqlAnyType || (type is SqlUnionType && type.types.any { isSubtypeOf(it) })

    override fun toString() = name
}

/**
 * A [SqlType] capable of storing a instant in time (with calendar day precision).
 *
 * @author Ian Caffey
 * @since 1.0
 */
object SqlDateType : SqlScalarType("Date") {
    override val default = SqlDate.EPOCH
    override fun isSubtypeOf(type: SqlType): Boolean =
        equals(type) || type is SqlAnyType || (type is SqlUnionType && type.types.any { isSubtypeOf(it) })
}

/**
 * A [SqlType] capable of storing an instant in time (with nanosecond precision).
 *
 * @author Ian Caffey
 * @since 1.0
 */
object SqlTimestampType : SqlScalarType("Timestamp") {
    override val default = SqlTimestamp.EPOCH
    override fun isSubtypeOf(type: SqlType): Boolean =
        equals(type) || type is SqlAnyType || (type is SqlUnionType && type.types.any { isSubtypeOf(it) })
}

/**
 * A [SqlType] capable of storing fixed-length sequence of characters with a minimum length of `1` character and a maximum length of `255` characters.
 *
 * @author Ian Caffey
 * @since 1.0
 */
data class SqlCharType(val length: Int) : SqlScalarType("Char($length)") {
    init {
        require(length in 1..255) { "'$name' length must be between 1 and 255. [length=$length]" }
    }

    override val default = SqlChar.EMPTY
    override fun isSubtypeOf(type: SqlType): Boolean =
        equals(type) || type is SqlAnyType || (type is SqlUnionType && type.types.any { isSubtypeOf(it) })

    override fun toString() = super.toString()
}

/**
 * A [SqlType] capable of storing fixed-length sequence of characters with a minimum length of `1` character and a maximum length of `65,535` characters.
 *
 * @author Ian Caffey
 * @since 1.0
 */
data class SqlVarcharType(val length: Int) : SqlScalarType("Varchar($length)") {
    init {
        require(length in 1..65_535) { "'$name' length must be between 1 and 65,535. [length=$length]" }
    }

    override val default = SqlVarchar.EMPTY
    override fun isSubtypeOf(type: SqlType): Boolean =
        equals(type) || type is SqlAnyType || (type is SqlUnionType && type.types.any { isSubtypeOf(it) })

    override fun toString() = super.toString()
}

/**
 * A [SqlType] capable of storing variable-length sequences of characters.
 *
 * @author Ian Caffey
 * @since 1.0
 */
object SqlStringType : SqlScalarType("String") {
    override val default = SqlString.EMPTY
    override fun isSubtypeOf(type: SqlType): Boolean =
        equals(type) || type is SqlAnyType || (type is SqlUnionType && type.types.any { isSubtypeOf(it) })
}

/**
 * A [SqlType] capable of storing a variable-length, ordered sequence of values from another [SqlType].
 *
 * @author Ian Caffey
 * @since 1.0
 */
data class SqlArrayType<T : SqlType>(val elementType: T) : SqlType() {
    override val name = "${elementType.name}[]"
    override val default: SqlValue = when (elementType) {
        is SqlBooleanType -> SqlBooleanArray.EMPTY
        is SqlTinyIntType -> SqlTinyIntArray.EMPTY
        is SqlSmallIntType -> SqlSmallIntArray.EMPTY
        is SqlIntType -> SqlIntArray.EMPTY
        is SqlBigIntType -> SqlBigIntArray.EMPTY
        is SqlFloatType -> SqlFloatArray.EMPTY
        is SqlDoubleType -> SqlDoubleArray.EMPTY
        else -> SqlArray.EMPTY
    }

    override fun isSubtypeOf(type: SqlType): Boolean =
        equals(type) || type is SqlAnyType || (type is SqlUnionType && type.types.any { isSubtypeOf(it) })

    override fun toString() = super.toString()
}

val SqlBigIntArrayType = SqlArrayType(SqlBigIntType)
val SqlBooleanArrayType = SqlArrayType(SqlBooleanType)
val SqlDoubleArrayType = SqlArrayType(SqlDoubleType)
val SqlFloatArrayType = SqlArrayType(SqlFloatType)
val SqlIntArrayType = SqlArrayType(SqlIntType)
val SqlSmallIntArrayType = SqlArrayType(SqlSmallIntType)
val SqlTinyIntArrayType = SqlArrayType(SqlTinyIntType)

/**
 * A [SqlType] capable of storing a values from an enumeration of [SqlType].
 *
 * @author Ian Caffey
 * @since 1.0
 */
data class SqlTupleType(val elements: List<SqlType>) : SqlType() {
    constructor(vararg types: SqlType) : this(listOf(*types))

    override val name = elements.joinToString(separator = ",", prefix = "(", postfix = ")") { it.name }
    override val default = SqlTuple.EMPTY
    override fun isSubtypeOf(type: SqlType): Boolean =
        equals(type) || type is SqlAnyType || (type is SqlUnionType && type.types.any { isSubtypeOf(it) })

    override fun toString() = super.toString()
}

/**
 * A [SqlType] capable of storing a relationship between a set of [SqlScalarType] to values of another [SqlType].
 *
 * @author Ian Caffey
 * @since 1.0
 */
data class SqlMapType(val keyType: SqlScalarType, val valueType: SqlType) : SqlType() {
    override val name = "${keyType.name}=>${valueType.name}"
    override val default = SqlMap.EMPTY
    override fun isSubtypeOf(type: SqlType): Boolean =
        equals(type) || type is SqlAnyType || (type is SqlUnionType && type.types.any { isSubtypeOf(it) })

    override fun toString() = super.toString()
}

/**
 * A [SqlType] capable of storing a name-accessible structured group of [SqlType] values.
 *
 * @author Ian Caffey
 * @since 1.0
 */
@Suppress("UNUSED_PARAMETER")
class SqlStructType private constructor(
    name: String?, fields: (SqlStructType) -> Map<String, SqlType>, disambiguator: Void?
) : SqlRecursiveType() {
    //Anonymous struct
    constructor(vararg fields: Pair<String, SqlType>) : this(fields.toMap())
    constructor(fields: Map<String, SqlType>) : this(null, { fields.toMap() }, null)

    //Named struct
    constructor(name: String, vararg fields: Pair<String, SqlType>) : this(name, fields.toMap())
    constructor(name: String, fields: Map<String, SqlType>) : this(name, { fields })
    constructor(name: String, fields: (SqlStructType) -> Map<String, SqlType>) : this(name, fields, null)

    //Note: this can't be initialized directly due to how the name/fields are dependent on each other in different ways
    //depending on if the struct is anonymous or not
    override val name: String
    val fields: Map<String, SqlType>
    private val cyclic: Boolean

    init {
        if (name != null) {
            this.name = name
            this.fields = fields(this)
        } else {
            this.fields = fields(this)
            this.name = this.fields.entries.joinToString(",", "{", "}") { "${it.key}:${it.value.name}" }
        }
        this.cyclic = this.fields.values.any { referencedIn(it) }
        if (name == null && cyclic) {
            throw IllegalStateException("Anonymous structs cannot have cyclic references.")
        }
        this.fields.forEach { (field, type) ->
            if (cyclesIn(type))
                throw IllegalStateException("Invalid cycle detected in $field for $this.")
        }
    }

    override val default = SqlStruct.EMPTY
    override fun isSubtypeOf(type: SqlType): Boolean =
        equals(type)
                || type is SqlAnyType
                || (type is SqlUnionType && type.types.any { isSubtypeOf(it) })
                || (type is SqlStructType && type.fields.all { (fieldName, fieldType) ->
            this.fields[fieldName]?.isSubtypeOf(fieldType) == true
        })

    override fun hashCode() = name.hashCode()
    override fun equals(other: Any?) =
        this === other || (other is SqlStructType && if (cyclic) name == other.name else fields == other.fields)
}

/**
 * A [SqlType] capable of storing different variants of [SqlType].
 *
 * @author Ian Caffey
 * @since 1.0
 */
@Suppress("UNUSED_PARAMETER")
class SqlUnionType private constructor(
    name: String?, types: (SqlUnionType) -> Set<SqlType>, disambiguator: Void?
) : SqlRecursiveType() {
    //Anonymous union
    constructor(vararg types: SqlType) : this(types.toSet())
    constructor(types: Set<SqlType>) : this(null, { types }, null)

    //Named union
    constructor(name: String, vararg types: SqlType) : this(name, types.toSet())
    constructor(name: String, types: Set<SqlType>) : this(name, { types })
    constructor(name: String, types: (SqlUnionType) -> Set<SqlType>) : this(name, types, null)

    //Note: this can't be initialized directly due to how the name/fields are dependent on each other in different ways
    //depending on if the union is anonymous or not
    override val name: String
    val types: Set<SqlType>
    private val cyclic: Boolean

    init {
        if (name != null) {
            this.name = name
            this.types = types(this)
        } else {
            this.types = types(this)
            this.name = if (this.types.size == 2 && this.types.contains(SqlNullType))
                "${this.types.first { it !is SqlNullType }}?" //T or SqlNullType will be rendered as T?
            else this.types.joinToString("|") { it.name }
        }
        this.cyclic = this.types.any { referencedIn(it) }
        if (name == null && cyclic) {
            throw IllegalStateException("Anonymous unions cannot have cyclic references.")
        }
        this.types.forEach {
            if (cyclesIn(it))
                throw IllegalStateException("Invalid cycle detected in $it for $this.")
        }
        require(this.types.size > 1) {
            "Unions must have at least 2 variants. [types=${this.types}]"
        }
    }

    override val default = SqlNull.takeIf { SqlNull.instanceOf(this) }
    override fun isSubtypeOf(type: SqlType): Boolean = equals(type) || types.all { it.isSubtypeOf(type) }
    override fun hashCode() = name.hashCode()
    override fun equals(other: Any?) =
        this === other || (other is SqlUnionType && if (cyclic) name == other.name else types == other.types)
}
