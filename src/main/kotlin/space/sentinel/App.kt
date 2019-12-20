package space.sentinel

import com.pi4j.io.gpio.GpioController
import com.pi4j.io.gpio.GpioFactory
import com.pi4j.io.gpio.GpioPinDigitalInput
import com.pi4j.io.gpio.RaspiPin
import org.bytedeco.javacpp.Loader
import org.bytedeco.javacv.OpenCVFrameConverter
import org.slf4j.LoggerFactory
import reactor.core.scheduler.Schedulers
import space.sentinel.sensor.PIRReader
import space.sentinel.service.CameraReader2
import space.sentinel.service.MotionDetectorService
import space.sentinel.service.NotificationService
import space.sentinel.service.SentinelClient
import space.sentinel.util.ConfigLoaderFactory


fun main(args: Array<String>) {


    try {

        //   static { Loader.load(org.bytedeco.opencv.global.opencv_core.class); }

        val converter = OpenCVFrameConverter.ToMat()
    } catch (t: Throwable) {
        t.printStackTrace()
    }

    System.setProperty("org.bytedeco.javacpp.logger.debug", "true")

    val logger = LoggerFactory.getLogger("Sentinel")
    val config = ConfigLoaderFactory().load()
    val pinAddress = config.getInt("camera.gpio").or(0)

    logger.debug("Setup GPIO $pinAddress")

    val gpioSensor: GpioController = GpioFactory.getInstance()
    val sensor: GpioPinDigitalInput = gpioSensor.provisionDigitalInputPin(RaspiPin.getPinByAddress(pinAddress))
    val reader = PIRReader(sensor)
    val camera = CameraReader2(config)

    val sentinelClient = SentinelClient(config)
    val notificationService = NotificationService(sentinelClient)

    try {
        logger.debug("Setup motion detection")

        MotionDetectorService(reader, camera)
                .detect()
                .flatMap(notificationService::notify)
                .doOnNext {
                    logger.info(it.message)
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
