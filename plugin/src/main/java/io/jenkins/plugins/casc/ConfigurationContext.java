package io.jenkins.plugins.casc;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Util;
import io.jenkins.plugins.casc.model.CNode;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.math.NumberUtils;
import org.kohsuke.stapler.Stapler;

/**
 * @author <a href="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 */
public class ConfigurationContext implements ConfiguratorRegistry {

    public static final String CASC_YAML_MAX_ALIASES_ENV = "CASC_YAML_MAX_ALIASES";
    public static final String CASC_YAML_MAX_ALIASES_PROPERTY = "casc.yaml.max.aliases";
    public static final String CASC_YAML_CODE_POINT_LIMIT_ENV = "CASC_YAML_CODE_POINT_LIMIT";
    public static final String CASC_YAML_CODE_POINT_LIMIT_PROPERTY = "casc.yaml.code_point_limit";
    public static final String CASC_MERGE_STRATEGY_ENV = "CASC_MERGE_STRATEGY";
    public static final String CASC_MERGE_STRATEGY_PROPERTY = "casc.merge.strategy";
    private Deprecation deprecation = Deprecation.reject;
    private Restriction restriction = Restriction.reject;
    private Unknown unknown = Unknown.reject;
    private String mergeStrategy;
    private final transient int yamlMaxAliasesForCollections;
    private final transient int yamlCodePointLimit;

    /**
     * the model-introspection model to be applied by configuration-as-code.
     * as we move forward, we might need to introduce some breaking change in the way we discover
     * configurable data model from live jenkins instance. At this time, 'new' mechanism will
     * only be enabled if yaml source do include adequate 'version: x'.
     */
    private Version version = Version.ONE;

    private transient List<Listener> listeners = new ArrayList<>();

    private final transient ConfiguratorRegistry registry;

    private transient String mode;

    private transient SecretSourceResolver secretSourceResolver;

    public ConfigurationContext(ConfiguratorRegistry registry) {
        this(registry, null);
    }

    public ConfigurationContext(ConfiguratorRegistry registry, String mergeStrategy) {
        this.registry = registry;
        String prop = getPropertyOrEnv(CASC_YAML_MAX_ALIASES_ENV, CASC_YAML_MAX_ALIASES_PROPERTY);
        yamlMaxAliasesForCollections = NumberUtils.toInt(prop, 50);
        prop = getPropertyOrEnv(CASC_YAML_CODE_POINT_LIMIT_ENV, CASC_YAML_CODE_POINT_LIMIT_PROPERTY);
        yamlCodePointLimit = NumberUtils.toInt(prop, 3) * 1024 * 1024;
        secretSourceResolver = new SecretSourceResolver(this);
        this.mergeStrategy = mergeStrategy != null
                ? mergeStrategy
                : getPropertyOrEnv(CASC_MERGE_STRATEGY_ENV, CASC_MERGE_STRATEGY_PROPERTY);
    }

    private String getPropertyOrEnv(String envKey, String proKey) {
        return Util.fixEmptyAndTrim(System.getProperty(proKey, System.getenv(envKey)));
    }

    public SecretSourceResolver getSecretSourceResolver() {
        return secretSourceResolver;
    }

    public void addListener(Listener listener) {
        listeners.add(listener);
    }

    public void clearListeners() {
        listeners.clear();
    }

    public void warning(@NonNull CNode node, @NonNull String message) {
        for (Listener listener : listeners) {
            listener.warning(node, message);
        }
    }

    public Deprecation getDeprecated() {
        return deprecation;
    }

    public Restriction getRestricted() {
        return restriction;
    }

    public Unknown getUnknown() {
        return unknown;
    }

    public void setDeprecated(Deprecation deprecation) {
        this.deprecation = deprecation;
    }

    public void setRestricted(Restriction restriction) {
        this.restriction = restriction;
    }

    public void setUnknown(Unknown unknown) {
        this.unknown = unknown;
    }

    public String getMergeStrategy() {
        return mergeStrategy;
    }

    String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public int getYamlMaxAliasesForCollections() {
        return yamlMaxAliasesForCollections;
    }

    public int getYamlCodePointLimit() {
        return yamlCodePointLimit;
    }

    // --- delegate methods for ConfigurationContext

    @Override
    @CheckForNull
    public RootElementConfigurator lookupRootElement(String name) {
        return registry.lookupRootElement(name);
    }

    @Override
    @NonNull
    public <T> Configurator<T> lookupOrFail(Type type) throws ConfiguratorException {
        return registry.lookupOrFail(type);
    }

    @Override
    @CheckForNull
    public <T> Configurator<T> lookup(Type type) {
        return registry.lookup(type);
    }

    public void setVersion(Version version) {
        this.version = version;
    }

    public Version getVersion() {
        return version;
    }

    /**
     * Get the {@link SecretSource}s used to substitute <code>${XX}</code> test used as yaml values with sensitive data.
     */
    public List<SecretSource> getSecretSources() {
        return SecretSource.all();
    }

    // Once we introduce some breaking change on the model inference mechanism, we will introduce `TWO` and so on
    // And this new mechanism will only get enabled when configuration file uses this version or later
    enum Version {
        ONE("1");

        private final String value;

        Version(String value) {
            this.value = value;
        }

        public static Version of(String version) {
            switch (version) {
                case "1":
                    return Version.ONE;
                default:
                    throw new IllegalArgumentException("unsupported version " + version);
            }
        }

        public String value() {
            return value;
        }

        public boolean isAtLeast(Version version) {
            return this.ordinal() >= version.ordinal();
        }

        @Override
        public String toString() {
            return value;
        }
    }

    static {
        Stapler.CONVERT_UTILS.register(new VersionConverter(), Version.class);
    }

    /**
     * Policy regarding unknown attributes.
     */
    enum Unknown {
        reject,
        warn
    }

    /**
     * Policy regarding {@link org.kohsuke.accmod.Restricted} attributes.
     */
    enum Restriction {
        reject,
        beta,
        warn
    }

    /**
     * Policy regarding {@link Deprecated} attributes.
     */
    enum Deprecation {
        reject,
        warn
    }

    @FunctionalInterface
    public interface Listener {
        void warning(@NonNull CNode node, @NonNull String error);
    }
}
