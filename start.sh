nohup ./gradlew build run -x test -x ':modules:migration:jar' -DlogHome='/tmp' --stacktrace --offline &
tail -f nohup.out
