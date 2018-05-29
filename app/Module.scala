import
    play.api.{Configuration, Environment}
  , play.api.inject.Module

class ExModule extends Module {
  override def bindings(env: Environment, conf : Configuration) = {
    Seq()
  }
}
