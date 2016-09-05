licenses += ("MIT", url("http://opensource.org/licenses/MIT"))
publishTo := Some("Bintray API Realm" at
  "https://api.bintray.com/content/kai-chen/maven/com.sorrentocorp." + name.value + "/" + version.value + ";publish=1")
credentials += Credentials(Path.userHome / ".bintray" / ".credentials")
