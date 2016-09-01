import java.io.FileInputStream

// val in = io.Source.fromInputStream(new FileInputStream("src/test/resources/tx-genes-01.json"))
val src = FileIO.fromPath(new File("src/test/resources/tx-genes-01.json").toPath)
