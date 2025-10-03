package no.nav.tilbakekreving.entity

import java.sql.PreparedStatement
import java.sql.ResultSet

class EntityFieldMapper<E, T, IdPrimitive>(
    val column: String,
    private val getter: (E) -> T,
    private val converter: FieldConverter<T, IdPrimitive>,
) {
    fun set(preparedStatement: PreparedStatement, index: Int, value: E) {
        converter.setColumn(index, preparedStatement, getter(value))
    }

    fun get(resultSet: ResultSet): T {
        return converter.convert(resultSet, column)
    }
}
