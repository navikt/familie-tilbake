package no.nav.tilbakekreving.entity

import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.PreparedStatementCreator
import java.sql.ResultSet

abstract class Entity<E, IdType, IdDbPrimitive>(
    private val tableName: String,
    idGetter: (E) -> IdType,
    idConverter: FieldConverter<IdType, IdDbPrimitive>,
) {
    val id = EntityFieldMapper("id", idGetter, idConverter)
    private val fields = mutableListOf<EntityFieldMapper<E, *, *>>()
    private val allFields get() = listOf(id) + fields

    fun <T, IdPrimitive> field(column: String, getter: (E) -> T, converter: FieldConverter<T, IdPrimitive>): EntityFieldMapper<E, T, IdPrimitive> {
        val field = EntityFieldMapper(column, getter, converter)
        fields.add(field)
        return field
    }

    fun insertQuery(jdbcTemplate: JdbcTemplate, value: E) {
        jdbcTemplate.update(createInsertStatement(value))
    }

    fun updateQuery(jdbcTemplate: JdbcTemplate, value: E) {
        val setters = fields.joinToString(", ") {
            "${it.column}=?"
        }
        jdbcTemplate.update({ ps ->
            val preparedStatement = ps.prepareStatement("UPDATE $tableName SET $setters WHERE ${id.column}=?")
            fields.forEachIndexed { index, field ->
                field.set(preparedStatement, index + 1, value)
            }
            id.set(preparedStatement, fields.size + 1, value)
            preparedStatement
        })
    }

    private fun placeholders(): String = allFields.joinToString(separator = ",") { "?" }

    private fun columnNames(): String = allFields.joinToString(",") { it.column }

    fun upsertQuery(
        jdbcTemplate: JdbcTemplate,
        value: E,
    ) {
        val setters = fields.joinToString(", ") {
            "${it.column}=EXCLUDED.${it.column}"
        }
        jdbcTemplate.update { connection ->
            val preparedStatement = connection.prepareStatement("INSERT INTO $tableName(${columnNames()}) VALUES(${placeholders()}) ON CONFLICT(id) DO UPDATE SET $setters")

            allFields.forEachIndexed { index, field ->
                field.set(preparedStatement, index + 1, value)
            }
            preparedStatement
        }
    }

    private fun createInsertStatement(value: E) = PreparedStatementCreator { connection ->
        val preparedStatement = connection.prepareStatement("INSERT INTO $tableName(${columnNames()}) VALUES(${placeholders()}) ON CONFLICT DO NOTHING;")

        allFields.forEachIndexed { index, field ->
            field.set(preparedStatement, index + 1, value)
        }
        preparedStatement
    }

    companion object {
        operator fun <E, T> ResultSet.get(mapper: EntityFieldMapper<E, T, *>): T {
            return mapper.get(this)
        }
    }
}
