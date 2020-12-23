package com.polysql

/**
 * A representation of the signature of a function which can be invoked within a SQL expression.
 *
 * @param name the name of the function
 * @param returnType the function which produces the return type as a function of the input schema to the function
 * @author Ian Caffey
 * @since 1.0
 */
class SqlFunction(val name: String, val returnType: (SqlSchema) -> SqlType) {
    constructor(name: String, returnType: SqlType) : this(name, { returnType })

    companion object {
        val AVERAGE = SqlFunction("avg") { it.column(0).type or SqlNullType }
        val COUNT = SqlFunction("count", SqlBigIntType)
        val MIN = SqlFunction("min") { it.column(0).type or SqlNullType }
        val MAX = SqlFunction("max") { it.column(0).type or SqlNullType }
        val SUM = SqlFunction("sum") { it.column(0).type or SqlNullType }
    }

    override fun equals(other: Any?) = this === other || other is SqlFunction && name == other.name
    override fun hashCode() = name.hashCode()
    override fun toString() = name
}
