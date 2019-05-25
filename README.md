# Public-Server
This is the public version of my Server based on Java.
You can use it for example to connect to databases on mobile devices or rewrite it for different jobs.

Important is the class src/android/databasecontroller/server/ServerConnection.java as will have to modify the method process to listen to
your commands
These commands can either be hardcoded or in general execute SQL-queries though I would highly recommend to hardcode the commands due to an
additional layer of security. Attackers on Databases will not have direct access to the Database.
In addition to that you will need to test arround and find classes which do not work expected on your system as this project was created for
a project at school. As I was annoyed by the fact that Android has no good way of using DBs I created this controller and wanted to contribute
as it might come in handy for you, too.

If you have suggestions to enhance the program in general please let me know.
