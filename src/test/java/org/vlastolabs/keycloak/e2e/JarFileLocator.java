/*
 * Copyright 2026 VlastoLabs Software
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.vlastolabs.keycloak.e2e;

import java.io.File;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Optional;

public class JarFileLocator {

    private final String jarPrefix;
    private final String targetDirectory;

    public JarFileLocator() {
        this("keycloak-invite-registration", "target");
    }

    public JarFileLocator(String jarPrefix, String targetDirectory) {
        this.jarPrefix = jarPrefix;
        this.targetDirectory = targetDirectory;
    }

    public String locateJar() {
        File targetDir = getTargetDirectory();
        validateTargetDirectory(targetDir);

        return findLatestJarFile(targetDir)
                .orElseThrow(() -> new JarNotFoundException(
                        String.format("Could not find %s JAR file in %s. " +
                                        "Please build the project first with 'mvn clean package'",
                                jarPrefix, targetDir.getAbsolutePath())));
    }

    private File getTargetDirectory() {
        String userDir = System.getProperty("user.dir");
        if (userDir == null) {
            throw new IllegalStateException("System property 'user.dir' is not set");
        }
        return new File(userDir, targetDirectory);
    }

    private void validateTargetDirectory(File targetDir) {
        if (!targetDir.exists()) {
            throw new JarNotFoundException("Target directory does not exist: " + targetDir.getAbsolutePath());
        }
        if (!targetDir.isDirectory()) {
            throw new JarNotFoundException("Target path is not a directory: " + targetDir.getAbsolutePath());
        }
    }

    private Optional<String> findLatestJarFile(File targetDir) {
        File[] jarFiles = targetDir.listFiles((dir, name) ->
                name.startsWith(jarPrefix) &&
                        name.endsWith(".jar") &&
                        !name.endsWith("-sources.jar") &&
                        !name.endsWith("-javadoc.jar"));

        if (jarFiles == null || jarFiles.length == 0) {
            return Optional.empty();
        }

        return Arrays.stream(jarFiles)
                .max(Comparator.comparingLong(File::lastModified))
                .map(File::getAbsolutePath);
    }

    public static class JarNotFoundException extends RuntimeException {
        public JarNotFoundException(String message) {
            super(message);
        }
    }
}