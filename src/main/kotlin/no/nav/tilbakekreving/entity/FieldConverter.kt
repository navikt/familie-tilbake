package no.nav.tilbakekreving.entity

import java.math.BigDecimal
import java.math.BigInteger
import java.sql.Date
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.Timestamp
import java.sql.Types
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.UUID

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

    object StringConverter : FieldConverter<String?, String?> {
        override fun convert(value: String?): String? {
            return value
        }

        override fun convert(resultSet: ResultSet, column: String): String? {
            return resultSet.getString(column)
        }

        override fun setColumn(index: Int, preparedStatement: PreparedStatement, value: String?) {
            preparedStatement.setString(index, value)
        }

        fun required() = Required(this)
    }

    object LocalDateTimeConverter : FieldConverter<LocalDateTime?, Timestamp?> {
        override fun convert(value: LocalDateTime?): Timestamp? {
            return value?.toInstant(ZoneOffset.UTC)?.let(Timestamp::from)
        }

        override fun convert(resultSet: ResultSet, column: String): LocalDateTime? {
            return resultSet.getTimestamp(column).toLocalDateTime()
        }

        override fun setColumn(index: Int, preparedStatement: PreparedStatement, value: LocalDateTime?) {
            preparedStatement.setTimestamp(index, convert(value))
        }

        fun required() = Required(this)
    }

    object LocalDateConverter : FieldConverter<LocalDate?, Date?> {
        override fun convert(value: LocalDate?): Date? {
            return value?.let(Date::valueOf)
        }

        override fun convert(resultSet: ResultSet, column: String): LocalDate? {
            return resultSet.getDate(column)?.toLocalDate()
        }

        override fun setColumn(index: Int, preparedStatement: PreparedStatement, value: LocalDate?) {
            preparedStatement.setDate(index, convert(value))
        }

        fun required() = Required(this)
    }

    class EnumConverter<T>(
        private val stringValue: (String) -> T,
        private val enumValue: (T) -> String,
    ) : FieldConverter<T?, String?> {
        override fun convert(value: T?): String? {
            return value?.let(enumValue)
        }

        override fun convert(resultSet: ResultSet, column: String): T? {
            val name = resultSet.getString(column)
            return stringValue(name)
        }

        override fun setColumn(index: Int, preparedStatement: PreparedStatement, value: T?) {
            preparedStatement.setString(index, convert(value))
        }

        fun required() = Required(this)

        companion object {
            inline fun <reified T : Enum<T>> of() = EnumConverter<T>(::enumValueOf, Enum<T>::name)
        }
    }

    object UUIDConverter : FieldConverter<UUID?, UUID?> {
        override fun convert(value: UUID?): UUID? {
            return value
        }

        override fun convert(resultSet: ResultSet, column: String): UUID? {
            return resultSet.getObject(column, UUID::class.java)
        }

        override fun setColumn(index: Int, preparedStatement: PreparedStatement, value: UUID?) {
            preparedStatement.setObject(index, value)
        }

        fun required() = Required(this)
    }

    object BigIntConverter : FieldConverter<BigInteger?, BigInteger?> {
        override fun convert(value: BigInteger?): BigInteger? {
            return value
        }

        override fun convert(resultSet: ResultSet, column: String): BigInteger? {
            return resultSet.getObject(column, BigInteger::class.java)
        }

        override fun setColumn(index: Int, preparedStatement: PreparedStatement, value: BigInteger?) {
            preparedStatement.setObject(index, convert(value))
        }

        fun required() = Required(this)
    }

    object BooleanConverter : FieldConverter<Boolean?, Boolean?> {
        override fun convert(value: Boolean?): Boolean? {
            return value
        }

        override fun convert(resultSet: ResultSet, column: String): Boolean {
            return resultSet.getBoolean(column)
        }

        override fun setColumn(index: Int, preparedStatement: PreparedStatement, value: Boolean?) {
            if (value != null) {
                preparedStatement.setBoolean(index, value)
            } else {
                preparedStatement.setNull(index, Types.BOOLEAN)
            }
        }

        fun required() = Required(this)
    }

    object BigDecimalConverter : FieldConverter<BigDecimal?, String?> {
        override fun convert(value: BigDecimal?): String? {
            return value?.toString()
        }

        override fun convert(resultSet: ResultSet, column: String): BigDecimal? {
            return resultSet.getString(column)?.toBigDecimal()
        }

        override fun setColumn(index: Int, preparedStatement: PreparedStatement, value: BigDecimal?) {
            preparedStatement.setString(index, convert(value))
        }

        fun required() = Required(this)
    }

    class Required<JavaType, DbPrimitive>(
        private val original: FieldConverter<JavaType?, DbPrimitive?>,
    ) : FieldConverter<JavaType, DbPrimitive> {
        override fun convert(value: JavaType): DbPrimitive {
            return original.convert(value)!!
        }

        override fun convert(resultSet: ResultSet, column: String): JavaType {
            return requireNotNull(original.convert(resultSet, column)) {
                "Converter expects column `$column` not to be null"
            }
        }

        override fun setColumn(index: Int, preparedStatement: PreparedStatement, value: JavaType) {
            return original.setColumn(index, preparedStatement, value)
        }
    }
}
