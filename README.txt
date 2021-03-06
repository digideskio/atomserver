=======================================================
Building AtomServer
=======================================================

To build against Postgres, you *must* provide the "env" property to maven (i.e "-Denv=postgres")
env  is the atomserver environment to use. It controls which environment properties file to use. (default=dev)
           These files live at src/main/resources/env. And follow the naming convention {env}.properties
           postgres.properties is setup for a local Postgres Server.

$ mvn -Denv=asdev-postgres clean install

BTW; you can also control logging somewhat this same way; 
  rootLoglevel    is the root log4j log level (default=DEBUG)
  loglevel        is the log4j log level for "org.atomserver" (default=TRACE)

$ mvn -Denv=asdev-postgres -DrootLogLevel=WARN install

NOTES
------
A)   If you kill a build mid-flight you might leave the DB in a "bad state". So you may need to "clear it".
Just use this backdoor JUnit (which clears all rows from the tables)
(of course, you can just use a SQL client with "DELETE *  FROM EntryStore" to do the same, assuming you have access)

$ mvn -Denv=asdev-postgres -DENABLE_DB_CLEAR_ALL=true -Dtest=ClearDBSTest test

=======================================================
Database Information
====================

Setting up Postgres
---------------------
A) Installing Postgres;
`````````````
On MAC:: Follow these instructions. http://developer.apple.com/internet/opensource/postgres.html
This is a fairly detailed process. You must actually compile postgres.
So you'll need XCode tools installed, and you'll have to install Fink, etc.
But don't get discouraged, it's not really hard, just a bit labourious
And ultimately, worth the trouble!!

NOTE - as of 2/18/2010 there is a download of the Postgres install for MAC.  http://www.postgresql.org/download/macosx

B) Start Postgres;
``````````````
$ su postgres;
$ pg-ctl restart;   (Or possibly pg-ctl reload;)

If you install the new Postgres, it will likely be running as your user.  You can use the ui to stop and start it
or the command line.

C) Create the DB;  (Note: postgres.properties is currently setup for to use the local "atomserver" user)
``````````````
$ su postgres;
$ createdb atomserver_dev;

Depending on how you set things up, you may need to create an "atomserver" user.  Log into psql and run:

CREATE USER atomserver WITH password 'atomserver';
GRANT ALL PRIVILEGES ON DATABASE atomserver_dev TO atomserver;

D) Install the tables 
``````````````
$ psql atomserver_dev -f ./src/main/resources/org/atomserver/sql/postgres/postgres_ddl.sql -U postgres
$ psql atomserver_dev -f ./src/main/resources/org/atomserver/sql/postgres/grant_perms.sql  -U postgres

E) Verify
$ psql atomserver_dev
# \d EntryStore;
# \q

F) Seed the DB
``````````
The JUnits seed (and tear down!!!!!) the DB, so you don't need to do this in you dev environment
But on, say, staging, you can seed the DB using the convenience script; dbseed.sh
