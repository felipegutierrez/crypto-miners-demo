package di

import controllers.AssetsComponents
import play.api.ApplicationLoader.Context
import play.api.db.{DBComponents, HikariCPComponents}
import play.api.db.evolutions.EvolutionsComponents
import play.api.db.slick.{DatabaseConfigProvider, DbName, SlickApi, SlickComponents}
import play.api.{Application, BuiltInComponentsFromContext, LoggerConfigurator, ApplicationLoader => PlayApplicationLoader}
import play.filters.HttpFiltersComponents
import slick.basic.{BasicProfile, DatabaseConfig}

import scala.concurrent.ExecutionContext

class ApplicationLoader extends PlayApplicationLoader {
  def load(context: Context): Application = {
    LoggerConfigurator(context.environment.classLoader).foreach {
      _.configure(context.environment)
    }

    new ApplicationComponents(context).application
  }
}

class ApplicationComponents(context: Context)
  extends BuiltInComponentsFromContext(context)
    with AssetsComponents
    with SlickComponents
    with DBComponents
    with EvolutionsComponents
    with HikariCPComponents
    with HttpFiltersComponents
    with PersistenceComponents {

  applicationEvolutions

  lazy val repositoryController = new controllers.HomeController(controllerComponents)

  lazy val router = new _root_.router.Routes(httpErrorHandler, repositoryController, assets)

}

trait PersistenceComponents {

  implicit def executionContext: ExecutionContext

  def slickApi: SlickApi

  lazy val defaultDbProvider = new DatabaseConfigProvider {
    def get[P <: BasicProfile]: DatabaseConfig[P] = slickApi.dbConfig[P](DbName("default"))
  }
}

