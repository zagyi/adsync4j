package org.adsync4j.gradle

import com.google.common.io.Files
import org.gradle.api.Project

/**
 * In order to be able to implement an in-memory LDAP server that can mock the behavior of Active Directory (at least in
 * regards that adsync4j is concerned about) with the help of the excellent UnboundId LDAP SDK we needed to make some
 * modifications to its internal classes. Since most of the classes in this SDK are declared final,
 * we couldn't simply change or add functionality in a sub-class, and the fact that the distributed jar is sealed means that
 * these classes cannot be replaced by their non-final copies either (by placing them before the original jar on the classpath).
 * <p>
 * To make it possible to apply that a dirty hack (replacing final classes with non-final copies) we had to resort to an even
 * more horrible hack: we un-seal the jar after downloading the original distribution...
 * <p>
 * Our only excuse for this horrendous deed is that it was necessary to implement integration tests for adsync4j that can be
 * run quickly and without any dependency on external infrastructure (namely an available Active Directory instance).
 * <p>
 * Check the {@code systemTesting} subproject to see how the UnboundId LDAP SDK is used.
 */
class UnboundIdSdkUnsealingHelper {

    /**
     * See exhausting explanation of the goal of this method in the {@link UnboundIdSdkUnsealingHelper class level}
     * documentation.
     */
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

    private static File download(String address, File dir) {
        def fileName = address.tokenize('/').last()
        File downloadedFile = new File(dir, fileName)
        def out = new BufferedOutputStream(new FileOutputStream(downloadedFile))
        out << new URL(address).openStream()
        out.close()
        downloadedFile
    }
}
