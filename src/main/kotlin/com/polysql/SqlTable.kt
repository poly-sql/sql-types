package com.polysql

/**
 * A column within a [SqlTable].
 *
 * @param name the unique name of the column
 * @param type the type of data contained by the column
 * @author Ian Caffey
 * @since 1.0
 */
data class SqlColumn(val name: String, val type: SqlType)

/**
 * A representation of a row of data with name-accessible columns.
 *
 * @param columns the columns of data within the schema
 * @author Ian Caffey
 * @since 1.0
 */
data class SqlSchema(val columns: List<SqlColumn>) {
    constructor(vararg columns: SqlColumn) : this(listOf(*columns))
    constructor(vararg columns: Pair<String, SqlType>) : this(columns.map { SqlColumn(it.first, it.second) })

    init {
        val duplicateNames = columns.groupBy { it.name }.filter { it.value.size > 1 }.keys
        if (duplicateNames.isNotEmpty()) {
            throw IllegalArgumentException(
                "Columns cannot share the same name within the same schema. [duplicates=$duplicateNames]"
            )
        }
    }

    val size: Int = columns.size
    fun indexOf(name: String) = columns.indexOfFirst { it.name == name }
    operator fun get(name: String) = this[indexOf(name)]
    operator fun get(column: Int) = columns[column]
}

/**
 * A logical organization of a collection of data which shares a common format.
 *
 * @param name the unique name of the table
 * @param schema the schema of data contained by the table
 * @author Ian Caffey
 * @since 1.0
 */
data class SqlTable(val name: String, val schema: SqlSchema) {
    val type = SqlStructType(name, schema.columns.map { it.name to it.type }.toMap())
}
