package space.sentinel

import com.pi4j.io.gpio.GpioController
import com.pi4j.io.gpio.GpioFactory
import com.pi4j.io.gpio.GpioPinDigitalInput
import com.pi4j.io.gpio.RaspiPin
import org.eclipse.jetty.client.HttpClient
import org.eclipse.jetty.client.util.BytesContentProvider
import org.eclipse.jetty.http.HttpMethod
import org.slf4j.LoggerFactory
import reactor.core.scheduler.Schedulers
import space.sentinel.sensor.PIRReader
import space.sentinel.service.CameraReader2
import space.sentinel.service.MotionDetectorService
import space.sentinel.util.ConfigLoaderFactory
import org.eclipse.jetty.reactive.client.ReactiveRequest


fun main(args: Array<String>) {
    val logger = LoggerFactory.getLogger("Sentinel")
    val config = ConfigLoaderFactory().load()
    val pinAddress = config.getInt("camera.gpio").or(0)

    logger.debug("Setup GPIO $pinAddress")

    val gpioSensor: GpioController = GpioFactory.getInstance()
    val sensor: GpioPinDigitalInput = gpioSensor.provisionDigitalInputPin(RaspiPin.getPinByAddress(pinAddress))
    val reader = PIRReader(sensor)
    val camera = CameraReader2(config)
    val httpClient = HttpClient()
    httpClient.start()

    try {
        logger.debug("Setup motion detection")

        MotionDetectorService(reader, camera)
                .detect()
                .doOnNext { notification ->
                    //send to server
                    try {
                        logger.info("${notification.message} - image size: ${notification.image.map { it.image.size }.orElse(0)}")
                    } catch (ex: Exception) {
                        logger.error(ex.message)
                    }
                }

                .doOnNext {
                    try {
                        val request = httpClient
                                .newRequest("http://localhost:8080/notification")
                                .method(HttpMethod.POST)
                                .content(BytesContentProvider(it.message.toByteArray()), "text/plain")
                                .send()
                    } catch (ex: Exception) {
                        logger.error(ex.message)
                    }
//                    val reactiveRequest = ReactiveRequest.newBuilder(request).build()
                }

                .doOnNext {
                    //https://github.com/netty/netty/issues/8279
//                    try {
//                        HttpClient.create()
//                                .baseUrl("192.168.0.10")// Prepares an HTTP client ready for configuration
//                                .port(8080)  // Obtains the server's port and provides it as a port to which this
//                                // client should connect
//                                .post()               // Specifies that POST method will be used
//                                .uri("/notification")   // Specifies the path
//                                .send(ByteBufFlux.fromString(Mono.just(it.message)))  // Sends the request body
//                                .responseContent()    // Receives the response body
//                                .aggregate()
//                                .asString()
//                                .log("http-client")
//                                .block();
//                    } catch (ex: Exception) {
//                        logger.error(ex.message)
//                    }
                }
                .doOnError {
                    logger.error(it.message, it)
                }
                .subscribeOn(Schedulers.elastic())
                .subscribe()

        Runtime.getRuntime().addShutdownHook(object : Thread() {
            override fun run() {
                reader.close()
                camera.close()
                gpioSensor.shutdown()
                logger.info("Stop")
            }
        })

        while (true)
            Thread.sleep(500)

        // service port
        // ping

    } catch (ex: Exception) {
        logger.error(ex.message)
    }
}
