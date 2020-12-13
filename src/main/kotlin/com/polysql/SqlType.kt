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
    infix fun or(other: SqlType): SqlUnionType = SqlUnionType(this, other)
    override fun toString() = name
}

/**
 * A [SqlType] which represent simple units of data.
 *
 * @author Ian Caffey
 * @since 1.0
 */
sealed class SqlScalarType(override val name: String) : SqlType() {
    infix fun to(type: SqlType) = SqlMapType(this, type)
}

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
    protected fun cyclesIn(enclosingType: SqlType): Boolean = when (enclosingType) {
        this -> true
        is SqlTupleType -> enclosingType.elements.any { cyclesIn(it) }
        //Note: nested references to the enclosing types will not have their fields initialized yet, so these null
        //checks are mandatory to ignore these cases (as they will be caught when checking for cycles from the parent)
        is SqlStructType -> enclosingType.fields != null && enclosingType.fields.values.any { cyclesIn(it) }
        is SqlUnionType -> enclosingType.types != null && enclosingType.types.all { cyclesIn(it) }
        else -> false
    }

    /**
     * Determines if the [enclosingType] contains a reference to `this`.
     *
     * @return `true` if the enclosing type contains a reference to `this`, otherwise `false`
     */
    @Suppress("SENSELESS_COMPARISON")
    protected fun referencedIn(enclosingType: SqlType): Boolean = when (enclosingType) {
        this -> true
        is SqlArrayType<*> -> referencedIn(enclosingType.elementType)
        is SqlTupleType -> enclosingType.elements.any { referencedIn(it) }
        is SqlMapType -> referencedIn(enclosingType.keyType) || referencedIn(enclosingType.valueType)
        //Note: nested references to the enclosing types will not have their fields initialized yet, so these null
        //checks are mandatory to ignore these cases (as they will be caught when checking for cycles from the parent)
        is SqlStructType -> enclosingType.fields != null && enclosingType.fields.values.any { referencedIn(it) }
        is SqlUnionType -> enclosingType.types != null && enclosingType.types.any { referencedIn(it) }
        else -> false
    }
}

/**
 * A [SqlType] capable of storing a null value.
 *
 * @author Ian Caffey
 * @since 1.0
 */
object SqlNullType : SqlScalarType("Null")

/**
 * A [SqlType] capable of storing two possible values; `true` or `false`.
 *
 * @author Ian Caffey
 * @since 1.0
 */
object SqlBooleanType : SqlScalarType("Boolean")

/**
 * A [SqlType] capable of storing a 8-bit signed integer with a minimum value of `-2^7` and maximum value of `2^7-1`.
 *
 * @author Ian Caffey
 * @since 1.0
 */
object SqlTinyIntType : SqlNumberType("TinyInt")

/**
 * A [SqlType] capable of storing a 16-bit signed integer with a minimum value of `-2^15` and maximum value of `2^15-1`.
 *
 * @author Ian Caffey
 * @since 1.0
 */
object SqlSmallIntType : SqlNumberType("SmallInt")

/**
 * A [SqlType] capable of storing a 32-bit signed integer with a minimum value of `-2^31` and maximum value of `2^31-1`.
 *
 * @author Ian Caffey
 * @since 1.0
 */
object SqlIntType : SqlNumberType("Int")

/**
 * A [SqlType] capable of storing a 64-bit signed integer with a minimum value of `-2^63` and maximum value of `2^63-1`.
 *
 * @author Ian Caffey
 * @since 1.0
 */
object SqlBigIntType : SqlNumberType("BigInt")

/**
 * A [SqlType] capable of storing a 32-bit single-precision floating-point number.
 *
 * @author Ian Caffey
 * @since 1.0
 */
object SqlFloatType : SqlNumberType("Float")

/**
 * A [SqlType] capable of storing a 64-bit double-precision floating-point number.
 *
 * @author Ian Caffey
 * @since 1.0
 */
object SqlDoubleType : SqlNumberType("Double")

/**
 * A [SqlType] capable of storing a fixed precision decimal number.
 *
 * @param precision the total number of digits
 * @param scale the number of digits in the fraction
 * @author Ian Caffey
 * @since 1.0
 */
data class SqlDecimalType(val precision: Int, val scale: Int) : SqlNumberType("Decimal($precision, $scale)") {
    init {
        require(precision <= 38) {
            "'$name' only supports precision up to 38 digits. [precision=$precision]"
        }
    }

    val mathContext = MathContext(precision, RoundingMode.HALF_UP)
    override fun toString() = name
}

/**
 * A [SqlType] capable of storing a instant in time (with calendar day precision).
 *
 * @author Ian Caffey
 * @since 1.0
 */
object SqlDateType : SqlScalarType("Date")

/**
 * A [SqlType] capable of storing an instant in time (with nanosecond precision).
 *
 * @author Ian Caffey
 * @since 1.0
 */
object SqlTimestampType : SqlScalarType("Timestamp")

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

    override fun toString() = super.toString()
}

/**
 * A [SqlType] capable of storing variable-length sequences of characters.
 *
 * @author Ian Caffey
 * @since 1.0
 */
object SqlStringType : SqlScalarType("String")

/**
 * A [SqlType] capable of storing a variable-length, ordered sequence of values from another [SqlType].
 *
 * @author Ian Caffey
 * @since 1.0
 */
data class SqlArrayType<T : SqlType>(val elementType: T) : SqlType() {
    override val name = "${elementType.name}[]"
    override fun toString() = super.toString()
}

/**
 * A [SqlType] capable of storing a values from an enumeration of [SqlType].
 *
 * @author Ian Caffey
 * @since 1.0
 */
data class SqlTupleType(val elements: List<SqlType>) : SqlType() {
    constructor(vararg types: SqlType) : this(listOf(*types))

    override val name = elements.joinToString(separator = ",", prefix = "(", postfix = ")") { it.name }
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
            this.name = this.types.joinToString("|") { it.name }
        }
        this.cyclic = this.types.any { referencedIn(it) }
        if (name == null && cyclic) {
            throw IllegalStateException("Anonymous unions cannot have cyclic references.")
        }
        this.types.forEach {
            if (cyclesIn(it))
                throw IllegalStateException("Invalid cycle detected in $it for $this.")
        }
    }

    override fun hashCode() = name.hashCode()
    override fun equals(other: Any?) =
        this === other || (other is SqlUnionType && if (cyclic) name == other.name else types == other.types)
}
