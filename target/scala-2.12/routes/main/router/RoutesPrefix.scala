
// @GENERATOR:play-routes-compiler
// @SOURCE:/home/felipe/workspace-scala-eclipse/crypto-miners-demo/conf/routes
// @DATE:Tue Jan 02 14:49:52 BRT 2018


package router {
  object RoutesPrefix {
    private var _prefix: String = "/"
    def setPrefix(p: String): Unit = {
      _prefix = p
    }
    def prefix: String = _prefix
    val byNamePrefix: Function0[String] = { () => prefix }
  }
}
