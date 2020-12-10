package com.polysql

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

    val size: Int = columns.size
    val type = SqlStruct(columns.associate { it.name to it.type })
    fun indexOf(name: String) = columns.indexOfFirst { it.name == name }
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
data class SqlTable(val name: String, val schema: SqlSchema)

/**
 * A column within a [SqlTable].
 *
 * @param name the unique name of the column
 * @param type the type of data contained by the column
 * @author Ian Caffey
 * @since 1.0
 */
data class SqlColumn(val name: String, val type: SqlType)
