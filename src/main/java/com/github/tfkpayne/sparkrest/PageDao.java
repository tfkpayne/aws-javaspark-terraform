package com.github.tfkpayne.sparkrest;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

/**
 * Created by tom on 27/06/2017.
 */
public class PageDao {

    private final String connectionUrl;
    private final String dbname;

    public PageDao(String connectionUrl, String dbname) {
        this.connectionUrl = connectionUrl;
        this.dbname = dbname;
    }

    private <R> Optional<R> executeQuery(Function<Connection, R> sqlQuery) {
        Connection conn = null;
        R result = null;
        try {
            conn = DriverManager.getConnection("jdbc:mysql://" + connectionUrl + ":3306/" + dbname,"javaspark", "javaspark");
            result = sqlQuery.apply(conn);
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            try {
                conn.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return Optional.ofNullable(result);
    }

    public void createTables() {
        Optional<Boolean> success = executeQuery(connection -> {
            try {
                DatabaseMetaData dbm = connection.getMetaData();
                ResultSet tables = dbm.getTables(null, null, "pages", null);
                if (tables.next()) {
                    System.out.println("Table already exists. Dropping table to re-create");
                    Statement dropTableStatement = connection.createStatement();
                    dropTableStatement.execute(
                            "DROP TABLE pages CASCADE");
                    dropTableStatement.close();

                }
                Statement createTableStatement = connection.createStatement();
                createTableStatement.execute(
                        "CREATE TABLE pages" +
                                "( path VARCHAR(255), " +
                                "content VARCHAR(255))");
                createTableStatement.close();
            } catch (SQLException e) {
                e.printStackTrace();
                return false;
            }
            return true;
        });
        if (success.isPresent() && success.get().equals(true)) {
            System.out.println("Table created successfully");
        } else {
            System.out.println("Table creation failed.");
        }
    }


    public Optional<List<Page>> insertPages(List<Page> pages) {
        Optional<List<Page>> insertedPages = executeQuery(connection -> {

            pages.forEach( page -> {
                try {
                    PreparedStatement insertStatement = connection.prepareStatement("INSERT INTO pages (path, content) VALUES (?, ?)");
                    insertStatement.setString(1, page.getPath());
                    insertStatement.setString(2, page.getContent());
                    insertStatement.execute();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            });
            List<Page> dbPages = new ArrayList<>();
            try {
                Statement selectStatement = connection.createStatement();
                ResultSet results = selectStatement.executeQuery("SELECT * FROM pages");
                while (results.next()) {
                    Page resultPage = new Page();
                    resultPage.setPath(results.getString("path"));
                    resultPage.setContent(results.getString("content"));
                    dbPages.add(resultPage);
                }
                results.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }


            return dbPages;

        });
        return insertedPages;
    }
}
