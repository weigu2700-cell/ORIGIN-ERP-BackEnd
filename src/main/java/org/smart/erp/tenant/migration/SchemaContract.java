package org.smart.erp.tenant.migration;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.sql.DataSource;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/** 严格比较结构，不读取业务行；未知差异必须人工处理，不能靠 baseline 掩盖。 */
public final class SchemaContract {

    private static final Set<String> HISTORIES = Set.of(
            "flyway_schema_history",
            TenantFlyway.TENANT_HISTORY,
            TenantFlyway.PLATFORM_HISTORY
    );

    private SchemaContract() {
    }

    public static void verify(DataSource source, String contract) {
        try (var input = SchemaContract.class.getResourceAsStream(
                "/db/schema-contract/" + contract + ".json");
             var connection = source.getConnection()) {
            if (input == null) {
                throw new IllegalStateException("缺少冻结的 schema 契约");
            }

            Map<String, String> expected = new ObjectMapper()
                    .readValue(input, new TypeReference<>() {
                    });
            var actual = snapshot(connection);
            if (!actual.keySet().equals(expected.keySet())) {
                throw new IllegalStateException("schema 表集合不匹配，请先核对备份及差异");
            }
            for (var table : expected.keySet()) {
                if (!expected.get(table).equals(actual.get(table))) {
                    throw new IllegalStateException("schema 结构不匹配：" + table);
                }
            }
        } catch (SQLException | IOException error) {
            throw new IllegalStateException("schema 校验未完成", error);
        }
    }

    static Map<String, String> snapshot(Connection connection) throws SQLException {
        var result = new TreeMap<String, String>();
        try (var query = connection.createStatement();
             var tables = query.executeQuery("SHOW FULL TABLES")) {
            while (tables.next()) {
                String table = tables.getString(1);
                if (HISTORIES.contains(table)) {
                    continue;
                }
                if (!"BASE TABLE".equals(tables.getString(2))) {
                    throw new IllegalStateException("不接管含未知视图的库");
                }
                try (var ddl = connection.createStatement();
                     var rows = ddl.executeQuery(
                             "SHOW CREATE TABLE `" + table.replace("`", "``") + "`")) {
                    rows.next();
                    // 自增当前值属于数据状态，不是结构差异。
                    result.put(table, rows.getString(2).replaceAll(" AUTO_INCREMENT=\\d+", ""));
                }
            }
        }

        for (String objects : new String[]{"TRIGGERS", "ROUTINES", "EVENTS"}) {
            String schemaColumn = objects.equals("TRIGGERS")
                    ? "TRIGGER_SCHEMA"
                    : objects.equals("ROUTINES") ? "ROUTINE_SCHEMA" : "EVENT_SCHEMA";
            try (var query = connection.prepareStatement(
                    "SELECT COUNT(*) FROM information_schema." + objects
                            + " WHERE " + schemaColumn + "=?")) {
                query.setString(1, connection.getCatalog());
                try (var rows = query.executeQuery()) {
                    rows.next();
                    if (rows.getLong(1) != 0) {
                        throw new IllegalStateException("不接管含未知触发器、存储过程或事件的库");
                    }
                }
            }
        }
        return result;
    }
}
