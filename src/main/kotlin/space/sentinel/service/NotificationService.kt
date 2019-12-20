package space.sentinel.service

import com.fasterxml.jackson.databind.ObjectMapper
import org.bytedeco.javacv.OpenCVFrameConverter
import org.eclipse.jetty.client.util.BytesContentProvider
import org.slf4j.LoggerFactory
import reactor.core.publisher.Flux
import reactor.core.scheduler.Schedulers
import space.sentinel.domain.Notification
import space.sentinel.domain.ServerResponse

data class ServerMessage(val message: String,
                         val image: ByteArray = ByteArray(0))

class NotificationService(val client: SentinelClient) {

    private val logger = LoggerFactory.getLogger(NotificationService::class.java)
    private val objectMapper = ObjectMapper()
    private val converter = OpenCVFrameConverter.ToMat()

    private fun convert(notification: Notification): ByteArray {
        return converter.convert(notification.image.get()).asByteBuffer().array()
    }

    private fun createMessage(notification: Notification): ServerMessage {
        if (notification.image.isPresent) {
            return ServerMessage(notification.message, convert(notification))
        } else {
            return ServerMessage(notification.message)
        }
    }

    fun notify(notification: Notification): Flux<ServerResponse> {
        return client
                .send(bytesContentProvider(notification))
                .subscribeOn(Schedulers.parallel())
                .map {
                    ServerResponse(it.response.toString())
                }
    }

    private fun bytesContentProvider(notification: Notification): BytesContentProvider {
        try {
            val message = createMessage(notification)
            val output = objectMapper.writeValueAsString(message)
            val request = BytesContentProvider(output.toByteArray())

            return request
        } catch (e: Exception) {
            logger.error(e.message, e)
            return BytesContentProvider()
        }
    }
}