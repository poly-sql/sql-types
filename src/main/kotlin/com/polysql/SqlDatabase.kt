package com.polysql

/**
 * A collection of [SqlTable].
 *
 * @param name the unique name of the database
 * @param tables the tables within the database
 * @author Ian Caffey
 * @since 1.0
 */
data class SqlDatabase(val name: String, val tables: Set<SqlTable>) {
    constructor(name: String, vararg tables: SqlTable) : this(name, setOf(*tables))

    private val tablesByName = tables.associateBy { it.name }
    fun table(name: String) = tablesByName[name]
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
 * A column within a [SqlSchema].
 *
 * @param name the name of the column
 * @param type the type of data contained by the column
 * @author Ian Caffey
 * @since 1.0
 */
data class SqlColumn(val name: String?, val type: SqlType)

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

    companion object {
        val EMPTY = SqlSchema(emptyList())
    }

    init {
        val duplicateNames = columns.groupBy { it.name }.filter { it.key != null && it.value.size > 1 }.keys
        if (duplicateNames.isNotEmpty()) {
            throw IllegalArgumentException(
                "Columns cannot share the same name within the same schema. [duplicates=$duplicateNames]"
            )
        }
    }

    val size: Int = columns.size
    fun contains(name: String) = columns.any { it.name == name }
    fun indexOf(name: String) = columns.indexOfFirst { it.name == name }
    fun isEmpty() = size == 0
    fun isNotEmpty() = size != 0
    fun column(index: Int) = columns[index]
    fun column(name: String) = column(indexOf(name).also {
        if (it == -1) throw IllegalArgumentException("$name cannot be found in $this.")
    })
}