import com.twitter.finagle.{Http, Service, Filter}
import com.twitter.util.{Await, Future}
import org.jboss.netty.handler.codec.http._

object Server extends App {
  val slowCreditScorerService = new Service[HttpRequest, HttpResponse] {
    def apply(req: HttpRequest): Future[HttpResponse] = {
      println("slowCreditScorerService called....")
      Future.value(new DefaultHttpResponse(
        req.getProtocolVersion, HttpResponseStatus.OK))
    }
  }

  var authFilter = new Filter[HttpRequest, HttpResponse, HttpRequest, HttpResponse] {
    def apply(request: HttpRequest, service: Service[HttpRequest, HttpResponse]): Future[HttpResponse] = {
      println(" Authentication Filter called....")
      val queryString = new QueryStringDecoder(request.getUri)
      if(!queryString.getParameters().containsKey("username") || !queryString.getParameters().containsKey("password")) {
        Future.value(new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.FORBIDDEN))
      }else{
        val username = queryString.getParameters().get("username")
        val password = queryString.getParameters().get("password")
        if(username.get(0).equals("jdoe") && password.get(0).equals("password")) {
          service(request)
        }else{
          Future.value(new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.FORBIDDEN))
        }
      }
    }
  }

  val authenticatedService: Service[HttpRequest, HttpResponse] =
    authFilter andThen slowCreditScorerService

  val server = Http.serve(":8080", authenticatedService)
  Await.ready(server)
}
