#!/bin/bash

db="benchmark"
create_db="CREATE DATABASE $db"

docker exec -it mysql1 /usr/bin/mysql -proot -e "$create_db"
docker exec -it mysql2 /usr/bin/mysql -proot -e "$create_db"
docker exec -it mysql3 /usr/bin/mysql -proot -e "$create_db"

docker exec -it mysql1 /usr/bin/mysql -proot -e "source /import/data.sql" $db
docker exec -it mysql2 /usr/bin/mysql -proot -e "source /import/data.sql" $db
docker exec -it mysql3 /usr/bin/mysql -proot -e "source /import/data.sql" $db

# create schema for the application
docker exec -it mysql_app /usr/bin/mysql -proot -e "$create_db"

