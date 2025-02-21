This is our patched version of SQLancer. You can find the origial project [here](https://github.com/sqlancer/sqlancer).

Usage:
```
cd sqlancer
mvn package -DskipTests
cd target
java -jar sqlancer-*.jar --num-threads 1 mutable --binary BINARY --oracle ORACLE
```

Replace `BINARY` with the mutable binary and `ORACLE` with the test oracle you want to perform. Available oracles are `NoREC`, `NoRECPLUS`, `TLP`, `WHERE`, `AGGREGATE`, `GROUP_BY`, `HAVING`, `PQS` `ALL`.

### Mutable-specific Options

|Argument                           |Description                                        | Default Value                |
|-----------------------------------|---------------------------------------------------|------------------------------|
|--oracle \<oracle\>                |The oracle to perform                              |none|
|--binary                           |Path to the mutable binary                         |/mutable/build/debug/bin/shell|
|--debug                            |Print debug statements to console                  | false |
|--debugJDBC                        |Print debug statements for JDBC driver to console  | false |
|--onlyGenerateQueries \<oracle\>   |Only generate queries for an oracle                | false |
|--test-default-values \<value\>    |Allow generating DEFAULT values in tables          | true |
|--test-not-null \<value\>          |Allow generating NOT NULL constraints in tables    | true |
|--test-unique \<value\>            |Allow generating UNIQUE constraints in tables      | true |
|--test-primary-key \<value\>       |Allow generating PRIMARY KEY constraints in tables | true |
|--test-boolean-constants \<value\> |Allow generating BOOL constants                    | true |
|--test-integer-constants \<value\> |Allow generating INT constants                     | true |
|--test-string-constants \<value\>  |Allow generating VARCHAR and CHAR constants        | true |
|--test-float-constants \<value\>   |Allow generating FLOAT constants                   | true |
|--test-double-constants \<value\>  |Allow generating DOUBLE constants                  | true |
|--test-decimal-constants \<value\> |Allow generating DECIMAL constants                 | true |
|--test-date-constants \<value\>    |Allow generating DATE constants                    | true |
|--test-datetime-constants \<value\>|Allow generating DATETIME constants                | true |

### General Options
Use `java -jar sqlancer-*.jar -h` to get an overview of all options.

