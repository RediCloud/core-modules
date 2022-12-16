@file:Repository("https://repo.maven.apache.org/maven2/")
@file:DependsOn("com.jcraft:jsch:0.1.55")

import com.jcraft.jsch.ChannelExec
import com.jcraft.jsch.ChannelSftp
import com.jcraft.jsch.JSch
import java.io.File

val jsch = JSch()
jsch.addIdentity("node01-ssh-key", System.getenv("NODE01_SSH_KEY").toByteArray(), null, null)
val session = jsch.getSession("root", "node01.hosting.suqatri.net", 22).apply {
    setConfig("StrictHostKeyChecking", "no")
    connect()
}
val sftp = session.openChannel("sftp").apply {
    connect()
} as ChannelSftp

fun deploy(from: String, to: String) =
    sftp.put(from, to, ChannelSftp.OVERWRITE)

fun Deploy_main.runCommandSync(it: String) {
    (session.openChannel("exec") as ChannelExec).apply {
        setCommand(it)
        connect()
        while (!isClosed) {
            Thread.sleep(100)
        }
        if (exitStatus != 0) {
            throw RuntimeException(
                "Failed to run command \"${it}\": ${
                    inputStream.readAllBytes().decodeToString()
                }"
            )
        }
    }
}
val files = listOf(
    "discord/build/libs/discord.jar" to "/home/core/modules/discord.jar",
)
runCommandSync("service core-standalone stop")
files.forEach {
    runCommandSync("mkdir -p ${it.second.substringBeforeLast("/")}")
    deploy(File(it.first).absolutePath.also { s -> println("Deploying $s to ${it.second}")}, it.second)
}
sftp.disconnect()
runCommandSync("service core-standalone start")
session.disconnect()