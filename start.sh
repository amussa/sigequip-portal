nohup ./gradlew build run -x test -x ':modules:migration:jar' -DlogHome='/tmp' --stacktrace &
tail -f nohup.out
