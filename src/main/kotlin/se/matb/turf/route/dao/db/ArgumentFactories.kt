package se.matb.turf.route.dao.db

import org.jdbi.v3.core.argument.AbstractArgumentFactory
import org.jdbi.v3.core.argument.Argument
import org.jdbi.v3.core.config.ConfigRegistry
import org.jdbi.v3.core.statement.StatementContext
import java.sql.PreparedStatement
import java.sql.Types

class IntListArgument(private val value: MutableList<Int>?) : Argument {
    override fun apply(position: Int, statement: PreparedStatement, ctx: StatementContext) =
        statement.setString(position, value?.joinToString(","))
}

class TimesArgumentFactory : AbstractArgumentFactory<MutableList<Int>>(Types.VARCHAR) {
    override fun build(value: MutableList<Int>, config: ConfigRegistry): Argument =
        IntListArgument(value)
}
