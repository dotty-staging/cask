package cask.main

import cask.internal.Router
import cask.internal.Router.EntryPoint
import cask.model.{BaseResponse, ParamContext}

import scala.reflect.macros.blackbox.Context
import language.experimental.macros

object Routes{

  trait Endpoint[R]{
    type InputType
    val path: String
    val methods: Seq[String]
    def subpath: Boolean = false
    def wrapMethodOutput(t: R): Any
    def handle(ctx: ParamContext,
               bindings: Map[String, String],
               routes: Routes,
               entryPoint: EntryPoint[InputType, Routes, cask.model.ParamContext]): Router.Result[BaseResponse]
  }

  case class EndpointMetadata[T](endpoint: Endpoint[_],
                                 entryPoint: EntryPoint[_, T, ParamContext])
  case class RoutesEndpointsMetadata[T](value: EndpointMetadata[T]*)
  object RoutesEndpointsMetadata{
    implicit def initialize[T] = macro initializeImpl[T]
    implicit def initializeImpl[T: c.WeakTypeTag](c: Context): c.Expr[RoutesEndpointsMetadata[T]] = {
      import c.universe._
      val router = new cask.internal.Router[c.type](c)

      val routeParts = for{
        m <- c.weakTypeOf[T].members
        annot <- m.annotations.filter(_.tree.tpe <:< c.weakTypeOf[Endpoint[_]])
      } yield {
        val annotObject = q"new ${annot.tree.tpe}(..${annot.tree.children.tail})"
        val annotObjectSym = c.universe.TermName(c.freshName("annotObject"))
        val route = router.extractMethod(
          m.asInstanceOf[MethodSymbol],
          weakTypeOf[T],
          (t: router.c.universe.Tree) => q"$annotObjectSym.wrapMethodOutput($t)",
          c.weakTypeOf[ParamContext],
          q"$annotObjectSym.parseMethodInput",
          tq"$annotObjectSym.InputType"
        )


        q"""{
          val $annotObjectSym = $annotObject
          cask.main.Routes.EndpointMetadata(
            $annotObjectSym,
            $route
          )
        }"""
      }

      c.Expr[RoutesEndpointsMetadata[T]](q"""cask.main.Routes.RoutesEndpointsMetadata(..$routeParts)""")
    }
  }
}

trait Routes{
  private[this] var metadata0: Routes.RoutesEndpointsMetadata[this.type] = null
  def caskMetadata =
    if (metadata0 != null) metadata0
    else throw new Exception("Routes not yet initialize")

  protected[this] def initialize()(implicit routes: Routes.RoutesEndpointsMetadata[this.type]): Unit = {
    metadata0 = routes
  }
}

