package se.matb.turf.route.dao.db

import org.jdbi.v3.core.argument.AbstractArgumentFactory
import org.jdbi.v3.core.argument.Argument
import org.jdbi.v3.core.config.ConfigRegistry
import org.jdbi.v3.core.statement.StatementContext
import se.matb.turf.route.dao.model.RouteInfo
import java.sql.PreparedStatement
import java.sql.Types

class StringArgument(private val value: Any) : Argument {
    override fun apply(position: Int, statement: PreparedStatement, ctx: StatementContext) =
        statement.setString(position, value.toString())
}

class TimesArgumentFactory : AbstractArgumentFactory<RouteInfo.Times>(Types.VARCHAR) {
    override fun build(value: RouteInfo.Times, config: ConfigRegistry): Argument =
        StringArgument(value)
}
