package io.jenkins.plugins.casc.impl.secrets;

import hudson.Extension;
import io.jenkins.plugins.casc.SecretSource;
import java.util.Optional;
import org.apache.commons.lang3.StringUtils;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;

@Extension
@Restricted(NoExternalUse.class)
public class EnvSecretSource extends SecretSource {

    @Override
    public Optional<String> reveal(String secret) {
        if (StringUtils.isBlank(secret)) {
            return Optional.empty();
        }
        return Optional.ofNullable(System.getProperty(secret, System.getenv(secret)));
    }
}
