package no.nav.tilbakekreving.entity

import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.Timestamp
import java.time.LocalDateTime
import java.time.ZoneOffset

interface FieldConverter<T, DbPrimitive> {
    fun convert(value: T): DbPrimitive

    fun convert(resultSet: ResultSet, column: String): T

    fun setColumn(index: Int, preparedStatement: PreparedStatement, value: T)

    object NumericId : FieldConverter<String, Int> {
        override fun convert(value: String): Int {
            return value.toInt()
        }

        override fun convert(resultSet: ResultSet, column: String): String {
            return resultSet.getInt(column)
                .toString()
                .padStart(10, '0')
        }

        override fun setColumn(index: Int, preparedStatement: PreparedStatement, value: String) {
            preparedStatement.setInt(index, value.toInt())
        }
    }

    object StringConverter : FieldConverter<String, String> {
        override fun convert(value: String): String {
            return value
        }

        override fun convert(resultSet: ResultSet, column: String): String {
            return resultSet.getString(column)
        }

        override fun setColumn(index: Int, preparedStatement: PreparedStatement, value: String) {
            preparedStatement.setString(index, value)
        }
    }

    object LocalDateTimeConverter : FieldConverter<LocalDateTime, Timestamp> {
        override fun convert(value: LocalDateTime): Timestamp {
            return Timestamp.from(value.toInstant(ZoneOffset.UTC))
        }

        override fun convert(resultSet: ResultSet, column: String): LocalDateTime {
            return resultSet.getTimestamp(column).toLocalDateTime()
        }

        override fun setColumn(index: Int, preparedStatement: PreparedStatement, value: LocalDateTime) {
            preparedStatement.setTimestamp(index, convert(value))
        }
    }

    class EnumConverter<T>(
        private val stringValue: (String) -> T,
        private val enumValue: (T) -> String,
    ) : FieldConverter<T, String> {
        override fun convert(value: T): String {
            return enumValue(value)
        }

        override fun convert(resultSet: ResultSet, column: String): T {
            val name = resultSet.getString(column)
            return stringValue(name)
        }

        override fun setColumn(index: Int, preparedStatement: PreparedStatement, value: T) {
            preparedStatement.setString(index, convert(value))
        }

        companion object {
            inline fun <reified T : Enum<T>> of() = EnumConverter<T>(::enumValueOf, Enum<T>::name)
        }
    }
}
