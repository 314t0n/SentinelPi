package space.sentinel.service

import com.typesafe.config.Config
import org.eclipse.jetty.client.HttpClient
import org.eclipse.jetty.client.api.ContentProvider
import org.eclipse.jetty.http.HttpMethod
import org.eclipse.jetty.reactive.client.ReactiveRequest
import org.eclipse.jetty.reactive.client.ReactiveResponse
import org.slf4j.LoggerFactory
import reactor.core.publisher.Flux

class SentinelClient(val config: Config) {

    val logger = LoggerFactory.getLogger(SentinelClient::class.java)
    val serverUrl = config.getString("sentinel.server.url")
    val httpClient = HttpClient()

    fun send(content: ContentProvider): Flux<ReactiveResponse> {

        httpClient.start()

        val request = httpClient
                .newRequest(serverUrl)
                .method(HttpMethod.POST)
                .content(content, "text/plain")

        val reactiveRequest = ReactiveRequest.newBuilder(request).build()

        val publisher = reactiveRequest
                .response(ReactiveResponse.Content.discard())

        return Flux.from(publisher).doFinally {
            httpClient.stop()
        }
    }
}