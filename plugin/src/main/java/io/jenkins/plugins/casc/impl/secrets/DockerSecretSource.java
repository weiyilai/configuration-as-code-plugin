package io.jenkins.plugins.casc.impl.secrets;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import hudson.Extension;
import io.jenkins.plugins.casc.SecretSource;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

/**
 * {@link SecretSource} implementation relying on <a href="https://docs.docker.com/engine/swarm/secrets">docker secrets</a>.
 * The path to secret directory can be overridden by setting environment variable <code>SECRETS</code>.
 * @author <a href="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 */
@Extension
public class DockerSecretSource extends SecretSource {

    public static final String DOCKER_SECRETS = "/run/secrets/";
    private final File secrets;

    @SuppressFBWarnings("DMI_HARDCODED_ABSOLUTE_FILENAME")
    public DockerSecretSource() {
        String s = System.getenv("SECRETS");
        secrets = s != null ? new File(s) : new File(DOCKER_SECRETS);
    }

    @Override
    public Optional<String> reveal(String secret) throws IOException {
        if (StringUtils.isBlank(secret)) {
            return Optional.empty();
        }
        final File file = new File(secrets, secret);
        if (file.isFile()) {
            return Optional.of(
                    FileUtils.readFileToString(file, StandardCharsets.UTF_8).trim());
        } else if (file.exists()) {
            throw new IOException("Cannot load non-file " + file);
        }
        return Optional.empty();
    }
}
