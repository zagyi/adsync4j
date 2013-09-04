package org.adsync4j.gradle

import com.google.common.io.Files
import org.gradle.api.Project

class UnboundIdSdkUnsealingHelper {

    static def obtainUnsealedUnboundIdSdk(Project prj, String destinationFile) {
        def ver = Libs.Versions.unboundid
        def unboundIdSdkJarURL =
            "http://search.maven.org/remotecontent?filepath=com/unboundid/unboundid-ldapsdk/${ver}/unboundid-ldapsdk-${ver}.jar"

        File tempDir = Files.createTempDir()

        prj.logger.debug("Attempting to download UnboundID LDAP SDK from $unboundIdSdkJarURL")
        def unboundIdSdkJar = download(unboundIdSdkJarURL, tempDir)
        prj.logger.debug("UnboundID LDAP SDK successfully downloaded to ${unboundIdSdkJar.absolutePath}")

        prj.ant.unzip(src: unboundIdSdkJar.absolutePath, dest: tempDir)
        unboundIdSdkJar.delete()

        prj.logger.debug('Remove sealing from the jar\'s manifest.')
        File manifestFile = new File(tempDir, 'META-INF/MANIFEST.MF')
        def manifestContent = manifestFile.text
        def unsealedManifestContent = manifestContent.replaceAll(/(?i).*sealed.*:.*true/, '')
        manifestFile.write(unsealedManifestContent)

        prj.ant.zip(destFile: destinationFile) {
            fileset(dir: tempDir)
        }

        prj.logger.debug('Delete temporary directory.')
        tempDir.deleteDir()
    }

    static File download(String address, File dir) {
        def fileName = address.tokenize('/').last()
        File downloadedFile = new File(dir, fileName)
        def out = new BufferedOutputStream(new FileOutputStream(downloadedFile))
        out << new URL(address).openStream()
        out.close()
        downloadedFile
    }
}
