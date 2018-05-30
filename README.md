Pnyao
===

Pnyao is a pdf management tool

## usage[alpha versoin]
```
$ sbt
...
[sbt] console
[sbt console] import com.github.nymphium.pnyao.Files
[sbt console] val path = "/path/to/dir" // where pdf files are located
[sbt console] val contents = Files.traverseDirectory(path) // read PDFs information
[sbt console] Files.writeToDB(path, contents) // write information to DB, which is JSON file located at `~/.pnyaodb` by default
[sbt console] :quit
[sbt] run
```

now you can access http://localhost:9000 to see and modify PDFs information

# license
MIT
