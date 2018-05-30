import com.google.inject.AbstractModule
import com.google.inject.name.Names
import services.{Pnyao, PnyaoService}

class Module extends AbstractModule {
  def configure() = {
    bind(classOf[PnyaoService])
      .to(classOf[Pnyao]).asEagerSingleton()
  }
}
