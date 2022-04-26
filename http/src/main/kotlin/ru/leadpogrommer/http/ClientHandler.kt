package ru.leadpogrommer.http

import org.intellij.lang.annotations.Language
import java.io.InputStream
import java.net.Socket
import java.net.URLDecoder
import java.nio.charset.Charset
import java.nio.file.Path
import java.util.*

class HTTPRequest private constructor() {
    lateinit var method: String
    lateinit var url: String
    var httpVersion: String? = null
    lateinit var headers: MutableMap<String, String>


    companion object {
        fun parse(stream: InputStream): HTTPRequest {
            val res = HTTPRequest()
            val scanner = Scanner(stream).useDelimiter("\r\n")
            val requestLine = scanner.nextLine().split(" ")
            res.method = requestLine[0]
            res.url = URLDecoder.decode(requestLine[1], Charset.forName("UTF-8"))
            if (requestLine.size == 3) {
                res.httpVersion = requestLine[2]
            }
            res.headers = mutableMapOf()
            while (true) {
                val line = scanner.nextLine()
                if (line.isEmpty()) {
                    break
                }
                val split = line.split(":")
                res.headers[split[0].trim()] = split[1].trim()
            }
            return res
        }
    }
}


class ClientHandler(private val client: Socket, private val basePath: Path) : Runnable {
    override fun run() {
        val request = HTTPRequest.parse(client.getInputStream())
        val path = basePath.resolve(request.url.trimStart('/'))
        val file = path.toFile()
        if (!file.exists()) {
            sendResponse(404, "notfound", "Not found")
        }
        if (file.isDirectory) {
            val fileEntries =
                file.list()!!.map { "<a href=\"${basePath.relativize(path.resolve(it))}\">${it}</a><br>" }
                    .reduce { a, b -> a + b }
            @Language("HTML") val response = """
                <html lang="en">
                <head>
                
                <title>Index of ${request.url}</title></head>
                <body>
                Index of ${request.url}
                <hr>
                $fileEntries
                </body>
                </html>
            """.trimIndent()
            sendResponse(200, "Ok", response)
        } else {
            val data = file.readBytes().decodeToString()
            sendResponse(200, "Ok", data)
        }
    }


    private fun sendResponse(code: Int, reason: String, body: String) {
        val encodedBody = body.toByteArray()
        val headers = mapOf("Content-Length" to encodedBody.size.toString(), "Connection" to "Close")
        val stream = client.getOutputStream()
        stream.write("HTTP/1.0 $code $reason\r\n".toByteArray())
        for (header in headers) {
            stream.write("${header.key}: ${header.value}\r\n".toByteArray())
        }
        stream.write("\r\n".toByteArray())
        stream.write(encodedBody)

    }
}