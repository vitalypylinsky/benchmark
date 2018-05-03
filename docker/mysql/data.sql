create table test_table
(
  column_1 int auto_increment
    primary key,
  column_2 int default '1'            null,
  column_3 int default '2'            null,
  column_4 varchar(100) default 'aaa' null,
  constraint test_table_column_1_uindex
  unique (column_1)
);

INSERT INTO benchmark.test_table (column_1, column_2, column_3, column_4) VALUES (1, 1, 2, 'aaa');
INSERT INTO benchmark.test_table (column_1, column_2, column_3, column_4) VALUES (2, 1, 2, 'aaa');
INSERT INTO benchmark.test_table (column_1, column_2, column_3, column_4) VALUES (3, 1, 2, 'aaa');
INSERT INTO benchmark.test_table (column_1, column_2, column_3, column_4) VALUES (4, 1, 2, 'aaa');
INSERT INTO benchmark.test_table (column_1, column_2, column_3, column_4) VALUES (5, 1, 2, 'aaa');
INSERT INTO benchmark.test_table (column_1, column_2, column_3, column_4) VALUES (6, 1, 2, 'aaa');
INSERT INTO benchmark.test_table (column_1, column_2, column_3, column_4) VALUES (7, 1, 2, 'aaa');
INSERT INTO benchmark.test_table (column_1, column_2, column_3, column_4) VALUES (8, 1, 2, 'aaa');
INSERT INTO benchmark.test_table (column_1, column_2, column_3, column_4) VALUES (9, 1, 2, 'aaa');
