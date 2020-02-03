package socket

import mqtt.Broker
import mqtt.ClientConnection
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.SelectionKey
import java.nio.channels.Selector
import java.nio.channels.ServerSocketChannel


actual open class ServerSocket actual constructor(private val broker: Broker) : ServerSocketInterface {

    private val socket = ServerSocketChannel.open()
    protected val selector: Selector = Selector.open()

    protected var sendBuffer: ByteBuffer = ByteBuffer.allocate(broker.maximumPacketSize.toInt())
    protected var receiveBuffer: ByteBuffer = ByteBuffer.allocate(broker.maximumPacketSize.toInt())

    init {
        socket.configureBlocking(false)
        socket.bind(InetSocketAddress(broker.host, broker.port), broker.backlog)
        socket.register(selector, SelectionKey.OP_ACCEPT)
    }

    override fun accept(key: SelectionKey) {
        try {
            val channel = (key.channel() as ServerSocketChannel).accept()
            channel.configureBlocking(false)

            val socket = Socket(sendBuffer, receiveBuffer)

            val clientConnection = ClientConnection(socket, broker)

            val socketKey = channel.register(selector, SelectionKey.OP_READ, clientConnection)
            socket.key = socketKey
        } catch (e: java.io.IOException) {
            e.printStackTrace()
        }
    }

    actual fun close() {
        selector.close()
        socket.close()
    }

    actual fun isRunning(): Boolean = selector.isOpen

    actual fun select(
        timeout: Long,
        block: (socket: ClientConnection, state: ServerSocketLoop.SocketState) -> Boolean
    ) {
        if (isRunning()) {
            val count = selector.select(timeout)
            if (count > 0) {
                val iterator = selector.selectedKeys().iterator()
                while (iterator.hasNext()) {
                    val key = iterator.next()
                    iterator.remove()
                    if (key.isValid) {
                        val clientConnection = (key.attachment() as ClientConnection?)
                        when {
                            key.isAcceptable -> accept(key)
                            key.isReadable -> block(clientConnection!!, ServerSocketLoop.SocketState.READ)
                            key.isWritable -> block(clientConnection!!, ServerSocketLoop.SocketState.WRITE)
                        }
                    }
                }
            }
        }
    }

}
